package com.sagetv.ng.telemetry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DjiSrtParser implements TelemetryAdapter {
    private static final Pattern TIME_RANGE = Pattern.compile(
        "(?<start>\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+-->\\s+(?<end>\\d{2}:\\d{2}:\\d{2},\\d{3})"
    );
    private static final Pattern VALUE_LINE = Pattern.compile("(?<label>[A-Za-z /_-]+)[:=]\\s*(?<value>.+)");

    @Override
    public TelemetryTrack parse(File input) throws IOException {
        List<TimedMetadata> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(input, StandardCharsets.UTF_8))) {
            String line;
            Double currentTimestamp = null;
            Map<String, Object> currentFields = null;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                Matcher timeMatcher = TIME_RANGE.matcher(trimmed);
                if (timeMatcher.matches()) {
                    if (currentTimestamp != null && currentFields != null && !currentFields.isEmpty()) {
                        entries.add(new TimedMetadata(currentTimestamp, currentFields));
                    }
                    currentTimestamp = parseTimestamp(timeMatcher.group("start"));
                    currentFields = new HashMap<>();
                    continue;
                }

                if (currentTimestamp == null || currentFields == null || trimmed.isEmpty()) {
                    continue;
                }

                Matcher valueMatcher = VALUE_LINE.matcher(trimmed);
                if (valueMatcher.matches()) {
                    String key = normalizeKey(valueMatcher.group("label"));
                    String value = valueMatcher.group("value").trim();
                    currentFields.put(key, value);
                }
            }

            if (currentTimestamp != null && currentFields != null && !currentFields.isEmpty()) {
                entries.add(new TimedMetadata(currentTimestamp, currentFields));
            }
        }

        return new TelemetryTrack(entries);
    }

    private static double parseTimestamp(String timestamp) {
        String[] hmsAndMillis = timestamp.split(",");
        String[] hms = hmsAndMillis[0].split(":");
        double hours = Double.parseDouble(hms[0]);
        double minutes = Double.parseDouble(hms[1]);
        double seconds = Double.parseDouble(hms[2]);
        double millis = Double.parseDouble(hmsAndMillis[1]);
        return (hours * 3600.0) + (minutes * 60.0) + seconds + (millis / 1000.0);
    }

    private static String normalizeKey(String label) {
        return label.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
