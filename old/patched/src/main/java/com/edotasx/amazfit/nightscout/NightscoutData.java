package com.edotasx.amazfit.nightscout;

import com.huami.watch.transport.DataBundle;

/**
 * Created by edoardotassinari on 08/04/18.
 */

public class NightscoutData {
    private String device;
    private String dateString;
    private String sysTime;
    private long date;
    private int sgv;
    private float delta;
    private String direction;
    private int noise;
    private float filtered;
    private float unfiltered;
    private int rssi;

    public static DataBundle toDataBundle(NightscoutData nightscoutData) {
        DataBundle dataBundle = new DataBundle();

        dataBundle.putLong("date", nightscoutData.getDate());
        dataBundle.putInt("sgv", nightscoutData.getSgv());
        dataBundle.putFloat("delta", nightscoutData.getDelta());
        dataBundle.putString("direction", nightscoutData.getDirection());
        dataBundle.putInt("noise", nightscoutData.getNoise());
        dataBundle.putLong("filtered", (long) nightscoutData.getFiltered());
        dataBundle.putLong("unfiltered", (long) nightscoutData.getUnfiltered());
        dataBundle.putInt("rssi", nightscoutData.getRssi());

        return dataBundle;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

    public String getSysTime() {
        return sysTime;
    }

    public void setSysTime(String sysTime) {
        this.sysTime = sysTime;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public int getSgv() {
        return sgv;
    }

    public void setSgv(int sgv) {
        this.sgv = sgv;
    }

    public float getDelta() {
        return delta;
    }

    public void setDelta(float delta) {
        this.delta = delta;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public int getNoise() {
        return noise;
    }

    public void setNoise(int noise) {
        this.noise = noise;
    }

    public float getFiltered() {
        return filtered;
    }

    public void setFiltered(float filtered) {
        this.filtered = filtered;
    }

    public float getUnfiltered() {
        return unfiltered;
    }

    public void setUnfiltered(float unfiltered) {
        this.unfiltered = unfiltered;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
