package com.sagetv.ng.telemetry;

public final class TelemetryOverlayPlugin {
    public static final String PLUGIN_ID = "com.sagetv.ng.telemetry";
    public static final String PLUGIN_NAME = "Telemetry Overlay Playback";
    public static final String PLUGIN_VERSION = "1.0.0";

    public TelemetryOverlayPlugin() {
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
}
