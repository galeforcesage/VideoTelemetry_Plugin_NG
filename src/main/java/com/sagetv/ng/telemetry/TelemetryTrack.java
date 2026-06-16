package com.sagetv.ng.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class TelemetryTrack {
    private final List<TimedMetadata> entries;

    public TelemetryTrack(List<TimedMetadata> entries) {
        ArrayList<TimedMetadata> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingDouble(TimedMetadata::timestamp));
        this.entries = Collections.unmodifiableList(sorted);
    }

    public List<TimedMetadata> entries() {
        return entries;
    }

    public TimedMetadata getAtTime(double time) {
        if (entries.isEmpty()) {
            return null;
        }

        TimedMetadata latest = null;
        for (TimedMetadata entry : entries) {
            if (entry.timestamp() > time) {
                break;
            }
            latest = entry;
        }
        return latest;
    }
}
