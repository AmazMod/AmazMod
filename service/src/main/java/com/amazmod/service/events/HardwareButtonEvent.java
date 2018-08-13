package com.amazmod.service.events;

public class HardwareButtonEvent {

    private int code;
    private boolean longPress;

    public HardwareButtonEvent(int code, boolean longPress) {
        this.code = code;
        this.longPress = longPress;
    }

    public int getCode() {
        return code;
    }

    public boolean isLongPress() {
        return longPress;
    }
}
