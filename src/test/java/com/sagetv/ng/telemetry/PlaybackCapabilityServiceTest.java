package com.sagetv.ng.telemetry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlaybackCapabilityServiceTest {
    @Test
    void detectsTelemetryFieldsFromDjiSidecar() throws Exception {
        Path dir = Files.createTempDirectory("telemetry-cap-test");
        Path video = dir.resolve("flight.mp4");
        Path srt = dir.resolve("flight.srt");
        Files.writeString(video, "");
        Files.writeString(
            srt,
            "1\n"
                + "00:00:00,100 --> 00:00:00,200\n"
                + "DJI\n"
                + "ALT: 120 m\n"
                + "GPS: 41.87, -87.62\n\n"
        );

        PlaybackCapabilityService svc = new PlaybackCapabilityService();
        PlaybackCapability capability = svc.buildForMedia(video.toFile(), Set.of("CC1"));

        assertTrue(capability.telemetryAvailable());
        assertTrue(capability.captionsAvailable());
        assertTrue(capability.telemetryFields().contains("alt"));
        assertTrue(capability.telemetryFields().contains("gps"));
        assertTrue(capability.captionTracks().contains("CC1"));
    }

    @Test
    void doesNotTreatGenericSrtAsTelemetry() throws Exception {
        Path dir = Files.createTempDirectory("telemetry-cap-test-plain");
        Path video = dir.resolve("movie.mp4");
        Path srt = dir.resolve("movie.srt");
        Files.writeString(video, "");
        Files.writeString(
            srt,
            "1\n"
                + "00:00:00,100 --> 00:00:02,000\n"
                + "hello world\n\n"
        );

        PlaybackCapabilityService svc = new PlaybackCapabilityService();
        PlaybackCapability capability = svc.buildForMedia(video.toFile(), Set.of("CC1"));

        assertFalse(capability.telemetryAvailable());
        assertTrue(capability.captionsAvailable());
    }

    @Test
    void pluginExposesCapabilityContract() throws Exception {
        Path dir = Files.createTempDirectory("telemetry-plugin-test");
        Path video = dir.resolve("clip.mp4");
        Files.writeString(video, "");

        TelemetryOverlayPlugin plugin = new TelemetryOverlayPlugin(
            new PlaybackCapabilityService(),
            mediaFile -> Set.of("CC1", "CC2")
        );

        PlaybackCapability capability = plugin.capabilityForMedia(video.toFile());

        assertFalse(capability.telemetryAvailable());
        assertTrue(capability.captionsAvailable());
        assertTrue(capability.captionTracks().contains("CC1"));
        assertTrue(capability.captionTracks().contains("CC2"));
    }
}
