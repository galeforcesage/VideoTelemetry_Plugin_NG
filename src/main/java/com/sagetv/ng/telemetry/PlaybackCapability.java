package com.sagetv.ng.telemetry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PlaybackCapability {
    private final boolean telemetryAvailable;
    private final boolean captionsAvailable;
    private final Set<String> telemetryFields;
    private final Set<String> captionTracks;

    public PlaybackCapability(
        boolean telemetryAvailable,
        boolean captionsAvailable,
        Set<String> telemetryFields,
        Set<String> captionTracks
    ) {
        this.telemetryAvailable = telemetryAvailable;
        this.captionsAvailable = captionsAvailable;
        this.telemetryFields = Collections.unmodifiableSet(new LinkedHashSet<>(telemetryFields));
        this.captionTracks = Collections.unmodifiableSet(new LinkedHashSet<>(captionTracks));
    }

    public boolean telemetryAvailable() {
        return telemetryAvailable;
    }

    public boolean captionsAvailable() {
        return captionsAvailable;
    }

    public Set<String> telemetryFields() {
        return telemetryFields;
    }

    public Set<String> captionTracks() {
        return captionTracks;
    }
}
