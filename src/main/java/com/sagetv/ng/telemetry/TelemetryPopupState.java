package com.sagetv.ng.telemetry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class TelemetryPopupState {
    public enum Mode {
        NONE,
        CAPTIONS,
        TELEMETRY,
        TELEMETRY_AND_CAPTIONS
    }

    private final Mode mode;
    private final boolean telemetryEnabled;
    private final Set<String> telemetryFields;
    private final Set<String> enabledTelemetryFields;
    private final Set<String> captionTracks;

    public TelemetryPopupState(
        Mode mode,
        boolean telemetryEnabled,
        Set<String> telemetryFields,
        Set<String> enabledTelemetryFields,
        Set<String> captionTracks
    ) {
        this.mode = mode;
        this.telemetryEnabled = telemetryEnabled;
        this.telemetryFields = Collections.unmodifiableSet(new LinkedHashSet<>(telemetryFields));
        this.enabledTelemetryFields = Collections.unmodifiableSet(new LinkedHashSet<>(enabledTelemetryFields));
        this.captionTracks = Collections.unmodifiableSet(new LinkedHashSet<>(captionTracks));
    }

    public Mode mode() {
        return mode;
    }

    public boolean telemetryEnabled() {
        return telemetryEnabled;
    }

    public Set<String> telemetryFields() {
        return telemetryFields;
    }

    public Set<String> enabledTelemetryFields() {
        return enabledTelemetryFields;
    }

    public Set<String> captionTracks() {
        return captionTracks;
    }
}
