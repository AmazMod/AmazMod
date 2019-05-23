package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.WidgetsData;

public class ResultWidgets {

    private WidgetsData widgetsData;

    public ResultWidgets(DataBundle dataBundle) {
        widgetsData = WidgetsData.fromDataBundle(dataBundle);
    }

    public WidgetsData getWidgetsData() {
        return widgetsData;
    }
}
