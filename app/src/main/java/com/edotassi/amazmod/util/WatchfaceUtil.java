package com.edotassi.amazmod.util;

import com.edotassi.amazmod.transport.TransportService;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

public class WatchfaceUtil {

    private static final String SET_WATCHFACE = "com.huami.watch.companion.transport.SetWatchFace";

    public static void setWatchface(String packageName, String serviceName) {

        DataBundle dataBundle = new DataBundle();
        dataBundle.putString("packagename", packageName);
        dataBundle.putString("servicename", serviceName);

        Logger.debug("packageName: {} serviceName{}", packageName, serviceName);

        TransportService.sendWithTransporterCompanion(SET_WATCHFACE, dataBundle);
    }

    public static void setWfzWatchFace(String fileName) {
        Logger.debug("fileName: {}", fileName);

        String packageName = "com.huami.watch.watchface.analogyellow";
        String serviceName = "com.huami.watch.watchface.ExternalWatchFace:" + fileName;

        WatchfaceUtil.setWatchface(packageName, serviceName);
    }
}