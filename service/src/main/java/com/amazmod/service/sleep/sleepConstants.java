package com.amazmod.service.sleep;

public class sleepConstants {

    //HR Sensor
    public static final int HR_VALUES = 20;
    public static final long HR_INTERVAL = 5 * 60 * 1000; //5m

    //Accelerometer
    public static final int SECS_PER_MAX_VALUE = 10;
    public static final int SAMPLING_PERIOD_US = 200_000_000; //200ms
    public static final int MAX_BATCH_SIZE = 4; //Don't increase this because we have 10k events and more can fill it

    //Other
    public static final int NOTIFICATION_ID = 1834;
    public static final String NOTIFICATION_KEY = "amazmod|SAA";


}
