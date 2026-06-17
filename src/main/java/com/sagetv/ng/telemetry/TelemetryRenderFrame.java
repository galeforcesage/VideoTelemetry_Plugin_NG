package com.sagetv.ng.telemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TelemetryRenderFrame {
    private final double timestamp;
    private final Map<String, Object> fields;

    public TelemetryRenderFrame(double timestamp, Map<String, Object> fields) {
        this.timestamp = timestamp;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public double timestamp() {
        return timestamp;
    }

    public Map<String, Object> fields() {
        return fields;
    }
}
