package com.sagetv.ng.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlaybackPopupStateServiceTest {
    @Test
    void popupModeIncludesTelemetryAndCaptionsWhenBothExist() throws Exception {
        Path dir = Files.createTempDirectory("popup-state-test");
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

        PlaybackPopupStateService service = new PlaybackPopupStateService(new PlaybackCapabilityService());
        TelemetryPopupState state = service.popupStateForMedia(video.toFile(), Set.of("CC1"));

        assertEquals(TelemetryPopupState.Mode.TELEMETRY_AND_CAPTIONS, state.mode());
        assertTrue(state.telemetryEnabled());
        assertTrue(state.telemetryFields().contains("alt"));
        assertTrue(state.captionTracks().contains("CC1"));
    }

    @Test
    void renderHookRespectsFieldToggles() throws Exception {
        Path dir = Files.createTempDirectory("popup-render-test");
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

        PlaybackPopupStateService service = new PlaybackPopupStateService(new PlaybackCapabilityService());
        service.setFieldEnabled(video.toFile(), Set.of(), "gps", false);

        Optional<TelemetryRenderFrame> frame = service.renderFrameAtTime(video.toFile(), Set.of(), 0.15);

        assertTrue(frame.isPresent());
        assertTrue(frame.get().fields().containsKey("alt"));
        assertFalse(frame.get().fields().containsKey("gps"));
    }
}
