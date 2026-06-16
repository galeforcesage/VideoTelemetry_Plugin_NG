package com.sagetv.ng.telemetry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class TelemetrySidecarDetector {
    public Optional<File> findDjiSidecar(File mediaFile) {
        if (mediaFile == null) {
            return Optional.empty();
        }

        String name = mediaFile.getName();
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        File parent = mediaFile.getParentFile();
        if (parent == null) {
            return Optional.empty();
        }

        File candidate = new File(parent, stem + ".srt");
        if (!candidate.isFile()) {
            return Optional.empty();
        }

        // Do not treat generic subtitle files as telemetry unless they look like DJI payloads.
        if (!looksLikeDjiTelemetry(candidate)) {
            return Optional.empty();
        }

        return Optional.of(candidate);
    }

    private boolean looksLikeDjiTelemetry(File sidecar) {
        try (BufferedReader reader = new BufferedReader(new FileReader(sidecar, StandardCharsets.UTF_8))) {
            String line;
            int inspected = 0;
            while ((line = reader.readLine()) != null && inspected < 80) {
                inspected++;
                String normalized = line.toLowerCase();
                if (normalized.contains("dji") || normalized.contains("gps") || normalized.contains("gimbal") || normalized.contains("alt")) {
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }
}
