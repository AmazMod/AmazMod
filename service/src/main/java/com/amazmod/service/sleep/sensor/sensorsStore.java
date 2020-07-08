package com.amazmod.service.sleep.sensor;

public class sensorsStore {
    private static accelerometer accSensor = new accelerometer();
    private static heartrate hrSensor = new heartrate();

    public static accelerometer getAccelerometer(){
        return accSensor;
    }

    public static heartrate getHrSensor(){
        return hrSensor;
    }
}
