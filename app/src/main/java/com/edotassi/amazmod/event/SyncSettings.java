package com.edotassi.amazmod.event;

import amazmod.com.transport.data.SettingsData;

public class SyncSettings {

    private SettingsData settingsData;

    public SyncSettings(SettingsData settingsData) {
        this.settingsData = settingsData;
    }

    public SettingsData getSettingsData() {
        return settingsData;
    }
}
