package com.amazmod.service.sleep;

import android.hardware.SensorManager;

public class sleepConstants {

    //HR Sensor
    public static final int HR_VALUES = 15;
    public static final long HR_INTERVAL = 5 * 60 * 1000; //5m

    //Accelerometer
    public static final int SECS_PER_MAX_VALUE = 10;
    public static final int SAMPLING_PERIOD_US = SensorManager.SENSOR_DELAY_NORMAL;
    public static final int MAX_BATCH_SIZE = 2; //Can be increased to 4 or 5 with proper testing

    //Other
    public static final int NOTIFICATION_ID = 1834;
    public static final String NOTIFICATION_KEY = "amazmod|SAA";


}
