package com.sagetv.ng.telemetry;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public final class NoOpCaptionCapabilitySource implements CaptionCapabilitySource {
    @Override
    public Set<String> availableCaptionTracks(File mediaFile) {
        return Collections.emptySet();
    }
}
