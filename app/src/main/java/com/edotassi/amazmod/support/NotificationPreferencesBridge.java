package com.edotassi.amazmod.support;

import android.content.Context;
import android.content.Intent;

import com.edotassi.amazmod.db.model.NotificationPreferencesEntity;

public interface NotificationPreferencesBridge {
    Context getBridgeContext();
    void deleteCommand(NotificationPreferencesEntity notificationPreferencesEntity);
    void setResult(int resultCode, Intent resultIntent);
    void finish();
}
