param(
    [switch]$ValidateOnly,
    [string]$StvPath = "C:\opt\sagetv\server\STVs\SageTV7\SageTV7.xml",
    [string]$StartMarker,
    [string]$EndMarker,
    [string]$FindText,
    [string]$ReplaceText,
    [switch]$UseRegex,
    [switch]$AllowMultiple,
    [switch]$Deploy,
    [string]$DeployHost = "192.168.0.75",
    [string]$DeployUser = "sagetv",
    [string]$Container = "sagetv-mine",
    [string]$RemoteStvPath = "/opt/sagetv/server/STVs/SageTV7/SageTV7.xml"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-XmlValidation {
    param([string]$Path)

    $xml = New-Object System.Xml.XmlDocument
    $xml.Load($Path)
}

function Get-BoundedBlock {
    param(
        [string]$Content,
        [string]$Start,
        [string]$End
    )

    $startCount = ([regex]::Matches($Content, [regex]::Escape($Start))).Count
    $endCount = ([regex]::Matches($Content, [regex]::Escape($End))).Count

    if ($startCount -ne 1 -or $endCount -ne 1) {
        throw "Markers must each appear exactly once. Start=$startCount End=$endCount"
    }

    $startIndex = $Content.IndexOf($Start)
    $endIndex = $Content.IndexOf($End)

    if ($startIndex -lt 0 -or $endIndex -lt 0 -or $endIndex -le $startIndex) {
        throw "Invalid marker ordering in content."
    }

    $endIndex += $End.Length
    return @{
        Prefix = $Content.Substring(0, $startIndex)
        Block = $Content.Substring($startIndex, $endIndex - $startIndex)
        Suffix = $Content.Substring($endIndex)
    }
}

if (-not (Test-Path $StvPath)) {
    throw "STV file not found: $StvPath"
}

if ($ValidateOnly) {
    Invoke-XmlValidation -Path $StvPath
    Write-Output "XML validation OK: $StvPath"
    exit 0
}

if ([string]::IsNullOrWhiteSpace($StartMarker) -or [string]::IsNullOrWhiteSpace($EndMarker)) {
    throw "StartMarker and EndMarker are required unless -ValidateOnly is used."
}

if ($null -eq $FindText -or $null -eq $ReplaceText) {
    throw "FindText and ReplaceText are required for edit mode."
}

$original = Get-Content -Path $StvPath -Raw
$backupPath = "$StvPath.bak.$([DateTime]::UtcNow.ToString('yyyyMMddHHmmss'))"
Copy-Item -Path $StvPath -Destination $backupPath -Force

$parts = Get-BoundedBlock -Content $original -Start $StartMarker -End $EndMarker
$block = $parts.Block

if ($UseRegex) {
    $matches = [regex]::Matches($block, $FindText).Count
    if (-not $AllowMultiple -and $matches -ne 1) {
        throw "Expected exactly 1 regex match in bounded block; found $matches"
    }
    if ($matches -lt 1) {
        throw "No regex matches found in bounded block."
    }
    $newBlock = [regex]::Replace($block, $FindText, $ReplaceText)
} else {
    $matches = ([regex]::Matches($block, [regex]::Escape($FindText))).Count
    if (-not $AllowMultiple -and $matches -ne 1) {
        throw "Expected exactly 1 text match in bounded block; found $matches"
    }
    if ($matches -lt 1) {
        throw "No text matches found in bounded block."
    }
    if ($AllowMultiple) {
        $newBlock = $block.Replace($FindText, $ReplaceText)
    } else {
        $idx = $block.IndexOf($FindText)
        $newBlock = $block.Substring(0, $idx) + $ReplaceText + $block.Substring($idx + $FindText.Length)
    }
}

$updated = $parts.Prefix + $newBlock + $parts.Suffix

try {
    Set-Content -Path $StvPath -Value $updated -NoNewline
    Invoke-XmlValidation -Path $StvPath
} catch {
    Copy-Item -Path $backupPath -Destination $StvPath -Force
    throw "Edit failed and backup restored. Details: $($_.Exception.Message)"
}

Write-Output "Edit succeeded and XML validated. Backup: $backupPath"

if ($Deploy) {
    # Build docker exec prefix when deploying inside a container
    $dockerPrefix = ""
    if (-not [string]::IsNullOrWhiteSpace($Container)) {
        $dockerPrefix = "docker exec $Container "
    }

    Write-Output "Deploying with stop/copy/start sequence to $DeployUser@$DeployHost (container: $( if ($dockerPrefix) { $Container } else { 'none' }))"

    Write-Output "Stopping SageTV..."
    ssh "$DeployUser@$DeployHost" "${dockerPrefix}/opt/sagetv/server/stopsage"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to stop SageTV on remote host (exit code $LASTEXITCODE). Aborting deployment."
    }

    Write-Output "Copying STV file..."
    scp "$StvPath" "${DeployUser}@${DeployHost}:$RemoteStvPath"
    if ($LASTEXITCODE -ne 0) {
        throw "SCP transfer failed (exit code $LASTEXITCODE). SageTV is stopped — manual recovery may be needed."
    }

    Write-Output "Starting SageTV..."
    ssh "$DeployUser@$DeployHost" "${dockerPrefix}/opt/sagetv/server/startsage"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to start SageTV on remote host (exit code $LASTEXITCODE)."
    }

    Write-Output "Deployment complete."
}
