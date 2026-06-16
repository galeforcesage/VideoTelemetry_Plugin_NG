package com.sagetv.ng.telemetry;

import java.io.File;
import java.util.Set;

public interface CaptionCapabilitySource {
    Set<String> availableCaptionTracks(File mediaFile);
}
