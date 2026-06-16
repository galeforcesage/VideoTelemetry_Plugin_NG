package com.sagetv.ng.telemetry;

public final class UnitsProvider {
    public enum UnitSystem {
        METRIC,
        IMPERIAL,
        AUTO
    }

    private UnitsProvider() {
    }

    public static double metersToFeet(double meters) {
        return meters * 3.28084;
    }

    public static double metersPerSecondToMilesPerHour(double metersPerSecond) {
        return metersPerSecond * 2.23694;
    }

    public static double metersPerSecondToKilometersPerHour(double metersPerSecond) {
        return metersPerSecond * 3.6;
    }
}
