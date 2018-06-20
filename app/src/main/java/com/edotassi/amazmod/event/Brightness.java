package com.edotassi.amazmod.event;

import amazmod.com.transport.data.BrightnessData;

public class Brightness {

    private BrightnessData brightnessData;

    public Brightness(BrightnessData brightnessData) {
        this.brightnessData = brightnessData;
    }

    public BrightnessData getBrightnessData() {
        return brightnessData;
    }
}
