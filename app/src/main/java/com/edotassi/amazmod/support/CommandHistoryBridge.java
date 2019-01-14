package com.edotassi.amazmod.support;

import android.content.Context;
import android.content.Intent;

import com.edotassi.amazmod.db.model.CommandHistoryEntity;

public interface CommandHistoryBridge {
    Context getBridgeContext();
    void deleteCommand(CommandHistoryEntity commandHistoryEntity);
    void setResult(int resultCode, Intent resultIntent);
    void finish();
}
