package com.edotassi.amazmodcompanionservice.events;

import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;

/**
 * Created by edoardotassinari on 10/04/18.
 */

public class NightscoutDataEvent {

    private long date;
    private int sgv;
    private float delta;
    private String direction;
    private int noise;
    private long filtered;
    private long unfiltered;
    private int rssi;

    public NightscoutDataEvent(DataBundle dataBundle) {
        date = (Long) dataBundle.get("date");
        sgv = dataBundle.getInt("sgv");
        delta = (Float) dataBundle.get("delta");
        direction = dataBundle.getString("direction");
        noise = dataBundle.getInt("noise");
        filtered = (Long) dataBundle.get("filtered");
        unfiltered = (Long) dataBundle.get("unfiltered");
        rssi = dataBundle.getInt("rssi");
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

    public long getFiltered() {
        return filtered;
    }

    public void setFiltered(long filtered) {
        this.filtered = filtered;
    }

    public long getUnfiltered() {
        return unfiltered;
    }

    public void setUnfiltered(long unfiltered) {
        this.unfiltered = unfiltered;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
