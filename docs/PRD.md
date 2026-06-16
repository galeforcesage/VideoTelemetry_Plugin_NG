# Video Telemetry Overlay Playback Plugin — PRD (V1)

## 1. Overview

Implement the first **SageTV-NG** native plugin: **Video Telemetry Overlay Playback Plugin**.

V1 adds:
- Telemetry-aware playback for video content
- Real-time telemetry overlay support (DJI SRT initial target)
- Extensible telemetry ingestion framework
- Plugin/server playback capability contract for telemetry + caption awareness

Integrated into: **Video -> Playback UI** (NG clients, with legacy compatibility where possible).

---

## 2. Platform Scope

### Required

| Component | Requirement |
|-----------|-------------|
| Server | SageTV-NG only |
| Clients | NG clients and legacy clients (best effort) |
| Runtime | Java 21 LTS |

### Not required
- Legacy SageTV server support

Constraint: no architectural compromises for NG due to legacy compatibility.

---

## 3. V1 Architecture

### 3.1 Plugin packaging

```
/JARs/
  telemetry-overlay.jar

/plugin/
  telemetry-overlay/plugin.xml
```

### 3.2 Plugin metadata (`plugin.xml`)

```xml
<plugin>
  <name>Telemetry Overlay Playback</name>
  <identifier>com.sagetv.ng.telemetry</identifier>
  <version>1.0.0</version>
  <type>Standard</type>
  <author>YourName</author>
  <description>Telemetry-aware playback for drone/action video</description>
</plugin>
```

### 3.3 Responsibility split

Server + plugin responsibilities in V1:
- File/sidecar detection for telemetry candidates
- DJI SRT parsing into normalized telemetry track entries
- Unit preference resolution and conversion
- Telemetry cache lifecycle
- Capability contract generation for playback popup/menu decisions

Client responsibilities:
- Render overlay and handle playback-time synchronization based on existing integration hooks

Note: do not assume client code changes unless explicitly approved.

### 3.4 Plugin/server capability contract (required)

Define a plugin/server contract that represents playback options for the current media item.

Required fields:
- `telemetryAvailable: boolean`
- `captionsAvailable: boolean`
- `telemetryFields: string[]`
- `captionTracks: string[]`

Behavior intent:
- If telemetry is available, telemetry controls can be presented.
- If telemetry is unavailable and captions exist, caption controls are presented.
- CC handling remains server-backed and already implemented; plugin consumes capability state.

---

## 4. Functional requirements (V1)

### 4.1 Playback compatibility

Must support:
- H.264
- H.265/HEVC (hardware dependent)

Constraint: use existing NG playback pipeline. Do not fork or replace the player.

### 4.2 Telemetry detection

Auto-detect telemetry sidecars for a video, starting with DJI SRT in V1.

Initial examples:
- `video.mp4`
- `video.srt` (when interpreted as telemetry sidecar for this plugin)

Caption/subtitle handling is pre-existing server functionality and must remain intact.

### 4.3 Telemetry parsing (Phase 1)

Source:
- DJI SRT

Output model:

```java
class TimedMetadata {
    double timestamp;
    Map<String, Object> fields;
}
```

### 4.4 Sync semantics

Telemetry must track playback clock and remain correct for:
- Pause
- Seek
- FF/RW

### 4.5 Overlay + popup behavior

Rendering mode:
- Overlay on video

Visual defaults:
- Semi-transparent black background (20-40%)
- Default placement top-left
- Optional placement bottom-left

Playback popup/menu requirement:
- Plugin drives availability and field-level toggles through capability contract.
- Telemetry controls and caption controls must not conflict.

### 4.6 Units

Telemetry respects global user unit preference where available.

Fallback plugin setting if global setting is unavailable:

```
telemetry_units = metric | imperial | auto
```

Conversion rules:

```
alt_ft = alt_m * 3.28084
mph    = mps   * 2.23694
kmh    = mps   * 3.6
```

---

## 5. Data model (V1)

```java
class TelemetryTrack {
    List<TimedMetadata> entries;
    TimedMetadata getAtTime(double time);
}

interface TelemetryAdapter {
    TelemetryTrack parse(File input);
}
```

Phase-2 cache target:

```
video.mp4.telemetry.json
```

---

## 6. Implementation phases

### Phase 1 (V1)
- NG plugin skeleton end-to-end
- DJI SRT parser
- Telemetry track model + lookup
- Unit conversion support
- Plugin/server capability contract for popup/menu
- Integration hooks for telemetry availability decisions

### Phase 2+ (not V1)
- Map overlay
- Widget system
- Additional adapters (GoPro, Garmin, OBD/CAN, GPX, MAVLink)
- Extended cache optimization and UI polish

Design rule: core remains telemetry-agnostic; adapters are plug-in modules.

---

## 7. Required but pending hook confirmation

These are required outcomes, but exact NG integration points/hook names are not assumed yet:
- 10-bit HEVC capability discovery path (server and client capability signals)
- Exact playback popup/menu integration hook used by plugin contract
- Exact caption-track capability interface shape consumed by plugin

---

## 8. Implementation checklist (V1)

Plugin setup:
- `plugin.xml`
- SageTV plugin entry and packaging

Playback hook discovery:
- `VideoPlayback`
- `MediaPlayer`
- `MiniClient`

Telemetry module:
- `DjiSrtParser.java`
- `TelemetryAdapter.java`
- `TelemetryTrack.java`
- `TimedMetadata.java`

Contract module:
- Playback capability model (telemetry + captions)

Units:
- Unit provider + conversion helpers
- Preference resolution fallback

Settings:
- `telemetry_overlay_enabled`
- `telemetry_units_override`

---

## 9. Risks and constraints

| Risk | Mitigation |
|------|------------|
| HEVC unsupported | Fallback/transcode decisions in existing pipeline |
| 10-bit overload | Capability discovery before telemetry-heavy overlay paths |
| SRT format drift | Tolerant parser and defensive field extraction |
| UI performance | Lightweight overlay payloads and bounded update frequency |

---

## 10. Key architecture rule

Optimize for SageTV-NG.

---

## 11. Role Clarification (Plugin vs Shared Server Core)

### 11.1 Plugin-owned server logic (V1)

The following server-side behavior is implemented by this plugin and should remain in plugin code:
- Telemetry sidecar detection and DJI SRT parsing
- Telemetry capability generation for playback popup/menu decisions
- Telemetry field inventory and per-media capability payload creation
- Unit preference resolution and telemetry unit conversions

Rationale: these are feature-specific behaviors for telemetry overlay playback and are not required by unrelated server features.

### 11.2 Shared server-core candidate

The following capability should be implemented in shared server core (or moved there) because it is reusable across many features and plugins:
- 10-bit HEVC capability discovery path and normalized capability signal

Rationale: codec capability discovery impacts direct play/transcode decisions and can be reused by playback, telemetry overlays, and other media features.

### 11.3 Integration rule

If a function is telemetry-specific, keep it in plugin code. If a function is reusable across unrelated features, propose it as shared server-core functionality first.
