package com.edotassi.amazmod.update;

public interface Updater {
    void updateCheckFailed();
    void updateAvailable(int version);
}
