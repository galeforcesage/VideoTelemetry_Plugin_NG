package com.sagetv.ng.telemetry;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PlaybackCapabilityService {
    private final TelemetrySidecarDetector sidecarDetector;
    private final TelemetryAdapter telemetryAdapter;

    public PlaybackCapabilityService() {
        this(new TelemetrySidecarDetector(), new DjiSrtParser());
    }

    public PlaybackCapabilityService(TelemetrySidecarDetector sidecarDetector, TelemetryAdapter telemetryAdapter) {
        this.sidecarDetector = sidecarDetector;
        this.telemetryAdapter = telemetryAdapter;
    }

    public PlaybackCapability buildForMedia(File mediaFile, Set<String> captionTracks) {
        Optional<File> maybeSidecar = sidecarDetector.findDjiSidecar(mediaFile);
        if (maybeSidecar.isEmpty()) {
            return new PlaybackCapability(false, !captionTracks.isEmpty(), Collections.emptySet(), captionTracks);
        }

        try {
            TelemetryTrack track = telemetryAdapter.parse(maybeSidecar.get());
            Set<String> fields = collectFieldNames(track);
            return new PlaybackCapability(!fields.isEmpty(), !captionTracks.isEmpty(), fields, captionTracks);
        } catch (IOException e) {
            return new PlaybackCapability(false, !captionTracks.isEmpty(), Collections.emptySet(), captionTracks);
        }
    }

    private Set<String> collectFieldNames(TelemetryTrack track) {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        for (TimedMetadata entry : track.entries()) {
            for (Map.Entry<String, Object> field : entry.fields().entrySet()) {
                fields.add(field.getKey());
            }
        }
        return fields;
    }
}
