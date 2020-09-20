package com.amazmod.service.sleep.sensor;

public class sensorsStore {
    private static final accelerometer accSensor = new accelerometer();
    private static final heartrate hrSensor = new heartrate();

    public static accelerometer getAccelerometer(){
        return accSensor;
    }

    public static heartrate getHrSensor(){
        return hrSensor;
    }
}
