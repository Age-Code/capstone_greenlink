package com.greenlink.greenlink.common.constants;

public final class IotThresholds {

    private IotThresholds() {
    }

    public static final double SOIL_MOISTURE_SHORTAGE_PERCENT = 30.0;
    public static final double SOIL_MOISTURE_TOO_WET_PERCENT = 80.0;

    public static final int WATER_COOLDOWN_SECONDS = 30;
    public static final int WATER_WINDOW_SECONDS = 600;
    public static final int WATER_MAX_REQUESTS_PER_WINDOW = 3;

    public static final double DEFAULT_AUTO_WATER_THRESHOLD_PERCENT = 35.0;
    public static final double DEFAULT_LIGHT_ON_THRESHOLD_LUX = 300.0;
    public static final double DEFAULT_LIGHT_OFF_THRESHOLD_LUX = 500.0;
}
