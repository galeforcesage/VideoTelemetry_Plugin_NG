package com.sagetv.ng.telemetry;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PlaybackPopupStateService {
    private static final int MAX_MEDIA_ENTRIES = 500;

    private final PlaybackCapabilityService capabilityService;
    private final TelemetrySidecarDetector sidecarDetector;
    private final TelemetryAdapter telemetryAdapter;

    // Bounded LRU caches to prevent unbounded memory growth on long-running servers.
    private final Map<String, Boolean> overlayEnabledByMedia = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_MEDIA_ENTRIES;
        }
    };
    private final Map<String, Set<String>> enabledFieldsByMedia = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
            return size() > MAX_MEDIA_ENTRIES;
        }
    };

    public PlaybackPopupStateService(PlaybackCapabilityService capabilityService) {
        this(capabilityService, new TelemetrySidecarDetector(), new DjiSrtParser());
    }

    public PlaybackPopupStateService(
        PlaybackCapabilityService capabilityService,
        TelemetrySidecarDetector sidecarDetector,
        TelemetryAdapter telemetryAdapter
    ) {
        this.capabilityService = capabilityService;
        this.sidecarDetector = sidecarDetector;
        this.telemetryAdapter = telemetryAdapter;
    }

    public TelemetryPopupState popupStateForMedia(File mediaFile, Set<String> captionTracks) {
        PlaybackCapability capability = capabilityService.buildForMedia(mediaFile, captionTracks);
        String key = mediaKey(mediaFile);

        boolean telemetryEnabled = overlayEnabledByMedia.getOrDefault(key, Boolean.TRUE);
        Set<String> allTelemetryFields = new LinkedHashSet<>(capability.telemetryFields());
        Set<String> enabledFields = new LinkedHashSet<>(
            enabledFieldsByMedia.getOrDefault(key, allTelemetryFields)
        );
        enabledFields.retainAll(allTelemetryFields);

        return new TelemetryPopupState(
            resolveMode(capability),
            telemetryEnabled,
            allTelemetryFields,
            enabledFields,
            capability.captionTracks()
        );
    }

    public TelemetryPopupState setTelemetryEnabled(File mediaFile, Set<String> captionTracks, boolean enabled) {
        overlayEnabledByMedia.put(mediaKey(mediaFile), enabled);
        return popupStateForMedia(mediaFile, captionTracks);
    }

    public TelemetryPopupState setFieldEnabled(
        File mediaFile,
        Set<String> captionTracks,
        String field,
        boolean enabled
    ) {
        String key = mediaKey(mediaFile);
        TelemetryPopupState current = popupStateForMedia(mediaFile, captionTracks);
        LinkedHashSet<String> nextEnabled = new LinkedHashSet<>(current.enabledTelemetryFields());
        if (enabled) {
            nextEnabled.add(field);
        } else {
            nextEnabled.remove(field);
        }
        enabledFieldsByMedia.put(key, nextEnabled);
        return popupStateForMedia(mediaFile, captionTracks);
    }

    public Optional<TelemetryRenderFrame> renderFrameAtTime(File mediaFile, Set<String> captionTracks, double timeSeconds) {
        TelemetryPopupState state = popupStateForMedia(mediaFile, captionTracks);
        if (!state.telemetryEnabled() || state.telemetryFields().isEmpty()) {
            return Optional.empty();
        }

        Optional<File> maybeSidecar = sidecarDetector.findDjiSidecar(mediaFile);
        if (maybeSidecar.isEmpty()) {
            return Optional.empty();
        }

        try {
            TelemetryTrack track = telemetryAdapter.parse(maybeSidecar.get());
            TimedMetadata metadata = track.getAtTime(timeSeconds);
            if (metadata == null) {
                return Optional.empty();
            }

            LinkedHashMap<String, Object> filtered = new LinkedHashMap<>();
            Set<String> enabledFields = state.enabledTelemetryFields();
            for (Map.Entry<String, Object> entry : metadata.fields().entrySet()) {
                if (enabledFields.contains(entry.getKey())) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }

            if (filtered.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new TelemetryRenderFrame(metadata.timestamp(), filtered));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private TelemetryPopupState.Mode resolveMode(PlaybackCapability capability) {
        if (capability.telemetryAvailable() && capability.captionsAvailable()) {
            return TelemetryPopupState.Mode.TELEMETRY_AND_CAPTIONS;
        }
        if (capability.telemetryAvailable()) {
            return TelemetryPopupState.Mode.TELEMETRY;
        }
        if (capability.captionsAvailable()) {
            return TelemetryPopupState.Mode.CAPTIONS;
        }
        return TelemetryPopupState.Mode.NONE;
    }

    private String mediaKey(File mediaFile) {
        if (mediaFile == null) {
            return "";
        }
        return mediaFile.getAbsolutePath();
    }
}
