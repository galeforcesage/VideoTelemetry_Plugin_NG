package com.sagetv.ng.telemetry;

import java.io.File;
import java.io.IOException;

public interface TelemetryAdapter {
    TelemetryTrack parse(File input) throws IOException;
}
