package com.sagetv.ng.telemetry;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TelemetryOverlayPlugin implements sage.SageTVPlugin {
    public static final String PLUGIN_ID = "com.sagetv.ng.telemetry";
    public static final String PLUGIN_NAME = "Telemetry Overlay Playback";
    public static final String PLUGIN_VERSION = "1.0.0";

    public static final String SETTING_OVERLAY_ENABLED = "telemetry_overlay_enabled";
    public static final String SETTING_UNITS_OVERRIDE = "telemetry_units_override";

    private final PlaybackCapabilityService capabilityService;
    private final PlaybackPopupStateService popupStateService;
    private final CaptionCapabilitySource captionCapabilitySource;
    private final sage.SageTVPluginRegistry pluginRegistry;
    private final Map<String, String> configValues = new LinkedHashMap<>();

    private volatile boolean started;

    public TelemetryOverlayPlugin(sage.SageTVPluginRegistry pluginRegistry) {
        this(pluginRegistry, false);
    }

    public TelemetryOverlayPlugin(sage.SageTVPluginRegistry pluginRegistry, boolean resetConfig) {
        this(new PlaybackCapabilityService(), new NoOpCaptionCapabilitySource(), pluginRegistry, resetConfig);
    }

    public TelemetryOverlayPlugin() {
        this(new PlaybackCapabilityService(), new NoOpCaptionCapabilitySource(), null, false);
    }

    public TelemetryOverlayPlugin(
        PlaybackCapabilityService capabilityService,
        CaptionCapabilitySource captionCapabilitySource
    ) {
        this(capabilityService, captionCapabilitySource, null, false);
    }

    private TelemetryOverlayPlugin(
        PlaybackCapabilityService capabilityService,
        CaptionCapabilitySource captionCapabilitySource,
        sage.SageTVPluginRegistry pluginRegistry,
        boolean resetConfig
    ) {
        this.capabilityService = capabilityService;
        this.popupStateService = new PlaybackPopupStateService(capabilityService);
        this.captionCapabilitySource = captionCapabilitySource;
        this.pluginRegistry = pluginRegistry;
        initializeDefaultConfig();
        if (resetConfig) {
            resetConfig();
        }
    }

    public String pluginId() {
        return PLUGIN_ID;
    }

    public String pluginName() {
        return PLUGIN_NAME;
    }

    public String pluginVersion() {
        return PLUGIN_VERSION;
    }

    public PlaybackCapability capabilityForMedia(File mediaFile) {
        return capabilityService.buildForMedia(mediaFile, captionCapabilitySource.availableCaptionTracks(mediaFile));
    }

    public TelemetryPopupState popupStateForMedia(File mediaFile) {
        return popupStateService.popupStateForMedia(mediaFile, captionCapabilitySource.availableCaptionTracks(mediaFile));
    }

    public TelemetryPopupState setTelemetryEnabled(File mediaFile, boolean enabled) {
        return popupStateService.setTelemetryEnabled(
            mediaFile,
            captionCapabilitySource.availableCaptionTracks(mediaFile),
            enabled
        );
    }

    public TelemetryPopupState setTelemetryFieldEnabled(File mediaFile, String field, boolean enabled) {
        return popupStateService.setFieldEnabled(
            mediaFile,
            captionCapabilitySource.availableCaptionTracks(mediaFile),
            field,
            enabled
        );
    }

    public Optional<TelemetryRenderFrame> renderFrameAtTime(File mediaFile, double timeSeconds) {
        return popupStateService.renderFrameAtTime(
            mediaFile,
            captionCapabilitySource.availableCaptionTracks(mediaFile),
            timeSeconds
        );
    }

    @Override
    public void start() {
        if (started) {
            return;
        }
        started = true;
        if (pluginRegistry != null) {
            pluginRegistry.eventSubscribe(this, ServerIntegrationNotes.EVENT_PLAYBACK_STARTED);
            pluginRegistry.eventSubscribe(this, ServerIntegrationNotes.EVENT_PLAYBACK_STOPPED);
            pluginRegistry.eventSubscribe(this, ServerIntegrationNotes.EVENT_PLAYBACK_FINISHED);
            pluginRegistry.eventSubscribe(this, ServerIntegrationNotes.EVENT_MEDIA_FILE_REMOVED);
        }
    }

    @Override
    public void stop() {
        if (!started) {
            return;
        }
        started = false;
        if (pluginRegistry != null) {
            pluginRegistry.eventUnsubscribe(this, ServerIntegrationNotes.EVENT_PLAYBACK_STARTED);
            pluginRegistry.eventUnsubscribe(this, ServerIntegrationNotes.EVENT_PLAYBACK_STOPPED);
            pluginRegistry.eventUnsubscribe(this, ServerIntegrationNotes.EVENT_PLAYBACK_FINISHED);
            pluginRegistry.eventUnsubscribe(this, ServerIntegrationNotes.EVENT_MEDIA_FILE_REMOVED);
        }
    }

    @Override
    public void destroy() {
        stop();
    }

    @Override
    public String[] getConfigSettings() {
        return new String[] { SETTING_OVERLAY_ENABLED, SETTING_UNITS_OVERRIDE };
    }

    @Override
    public String getConfigValue(String setting) {
        return configValues.get(setting);
    }

    @Override
    public String[] getConfigValues(String setting) {
        String value = getConfigValue(setting);
        if (value == null) {
            return new String[0];
        }
        return new String[] { value };
    }

    @Override
    public int getConfigType(String setting) {
        if (SETTING_OVERLAY_ENABLED.equals(setting)) {
            return CONFIG_BOOL;
        }
        if (SETTING_UNITS_OVERRIDE.equals(setting)) {
            return CONFIG_CHOICE;
        }
        return CONFIG_TEXT;
    }

    @Override
    public void setConfigValue(String setting, String value) {
        if (setting == null || value == null) {
            return;
        }
        configValues.put(setting, value);
    }

    @Override
    public void setConfigValues(String setting, String[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        setConfigValue(setting, values[0]);
    }

    @Override
    public String[] getConfigOptions(String setting) {
        if (SETTING_UNITS_OVERRIDE.equals(setting)) {
            return new String[] {
                UnitsProvider.UnitSystem.AUTO.name().toLowerCase(),
                UnitsProvider.UnitSystem.METRIC.name().toLowerCase(),
                UnitsProvider.UnitSystem.IMPERIAL.name().toLowerCase()
            };
        }
        return new String[0];
    }

    @Override
    public String getConfigHelpText(String setting) {
        if (SETTING_OVERLAY_ENABLED.equals(setting)) {
            return "Enable telemetry overlay playback integration.";
        }
        if (SETTING_UNITS_OVERRIDE.equals(setting)) {
            return "Override telemetry units. Use auto to follow global preference.";
        }
        return "";
    }

    @Override
    public String getConfigLabel(String setting) {
        if (SETTING_OVERLAY_ENABLED.equals(setting)) {
            return "Telemetry overlay enabled";
        }
        if (SETTING_UNITS_OVERRIDE.equals(setting)) {
            return "Telemetry units override";
        }
        return setting;
    }

    @Override
    public void resetConfig() {
        configValues.clear();
        initializeDefaultConfig();
    }

    @Override
    public void sageEvent(String eventName, Map eventVars) {
        // V1 intentionally keeps event handling lightweight; capability generation is on-demand.
        if (ServerIntegrationNotes.EVENT_MEDIA_FILE_REMOVED.equals(eventName)) {
            // Reserved for future cache eviction once persisted caches are introduced.
        }
    }

    private void initializeDefaultConfig() {
        configValues.put(SETTING_OVERLAY_ENABLED, Boolean.TRUE.toString());
        configValues.put(SETTING_UNITS_OVERRIDE, UnitsProvider.UnitSystem.AUTO.name().toLowerCase());
    }
}
