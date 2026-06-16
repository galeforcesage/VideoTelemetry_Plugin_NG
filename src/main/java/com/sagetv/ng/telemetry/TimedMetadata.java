package com.sagetv.ng.telemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TimedMetadata {
    private final double timestamp;
    private final Map<String, Object> fields;

    public TimedMetadata(double timestamp, Map<String, Object> fields) {
        this.timestamp = timestamp;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public double timestamp() {
        return timestamp;
    }

    public Map<String, Object> fields() {
        return fields;
    }

    public Object get(String key) {
        return fields.get(key);
    }
}
