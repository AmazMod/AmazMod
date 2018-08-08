package com.edotassi.amazmod.event.local;

public class IsWatchConnectedLocal {

    private boolean isWatchConnected;

    public IsWatchConnectedLocal(boolean isWatchConnected) {
        this.isWatchConnected = isWatchConnected;
    }

    public boolean getWatchStatus() {
        return this.isWatchConnected;
    }
}
