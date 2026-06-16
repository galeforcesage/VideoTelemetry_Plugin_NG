package com.sagetv.ng.telemetry;

import java.io.File;
import java.util.Collections;

public final class TelemetryOverlayPlugin {
    public static final String PLUGIN_ID = "com.sagetv.ng.telemetry";
    public static final String PLUGIN_NAME = "Telemetry Overlay Playback";
    public static final String PLUGIN_VERSION = "1.0.0";

    private final PlaybackCapabilityService capabilityService;
    private final CaptionCapabilitySource captionCapabilitySource;

    public TelemetryOverlayPlugin() {
        this(new PlaybackCapabilityService(), mediaFile -> Collections.emptySet());
    }

    public TelemetryOverlayPlugin(
        PlaybackCapabilityService capabilityService,
        CaptionCapabilitySource captionCapabilitySource
    ) {
        this.capabilityService = capabilityService;
        this.captionCapabilitySource = captionCapabilitySource;
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
}
