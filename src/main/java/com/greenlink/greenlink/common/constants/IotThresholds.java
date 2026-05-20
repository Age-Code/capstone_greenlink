package com.greenlink.greenlink.common.constants;

public final class IotThresholds {

    private IotThresholds() {
    }

    public static final double SOIL_MOISTURE_SHORTAGE_PERCENT = 30.0;
    public static final double SOIL_MOISTURE_TOO_WET_PERCENT = 80.0;

    public static final double DEFAULT_AUTO_WATER_THRESHOLD_PERCENT = 35.0;
    public static final double DEFAULT_LIGHT_ON_THRESHOLD_LUX = 300.0;
    public static final double DEFAULT_LIGHT_OFF_THRESHOLD_LUX = 500.0;
}
