package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.NotificationReplyData;
import amazmod.com.transport.data.SilenceApplicationData;

public class SilenceApplication {

    private SilenceApplicationData silenceApplicationData;

    public SilenceApplication(DataBundle dataBundle) {
        silenceApplicationData = SilenceApplicationData.fromDataBundle(dataBundle);
        }

    public SilenceApplicationData getSilenceApplicationData() {
        return silenceApplicationData;
    }
}
