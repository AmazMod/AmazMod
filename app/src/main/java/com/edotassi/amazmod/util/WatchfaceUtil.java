package com.edotassi.amazmod.util;

import android.content.Context;

import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;

import org.tinylog.Logger;

import amazmod.com.transport.data.FileData;

public class WatchfaceUtil {
    private static final String SET_WATCHFACE = "com.huami.watch.companion.transport.SetWatchFace";

    public static void setWatchface(Context c, String packagename, String servicename) {
        //Connect transporter
        Transporter transporter = TransporterClassic.get(c, "com.huami.watch.companion");
        DataBundle dataBundle = new DataBundle();
        dataBundle.putString("packagename",packagename);
        dataBundle.putString("servicename", servicename);
        transporter.connectTransportService();
        transporter.send(SET_WATCHFACE, dataBundle, dataTransportResult -> {
            Logger.debug("WatchfaceUtil setWatchface dataTransportResult: " + dataTransportResult.toString());
        });
        //Disconnect to avoid leaks
        transporter.disconnectTransportService();
    }

    public static void setWfzWatchFace(Context c, String fileName){
            String packagename = "com.huami.watch.watchface.analogyellow";
            String servicename ="com.huami.watch.watchface.ExternalWatchFace:" + fileName;
            WatchfaceUtil.setWatchface(c,packagename,servicename);
    }
}