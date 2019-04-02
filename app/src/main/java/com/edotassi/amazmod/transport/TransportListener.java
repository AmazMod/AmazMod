package com.edotassi.amazmod.transport;

import android.content.Context;
import android.media.AudioManager;
import android.os.SystemClock;
import android.view.KeyEvent;

import com.edotassi.amazmod.event.NextMusic;
import com.edotassi.amazmod.event.NotificationReply;
import com.edotassi.amazmod.event.SilenceApplication;
import com.edotassi.amazmod.event.ToggleMusic;
import com.edotassi.amazmod.event.local.ReplyToNotificationLocal;
import com.edotassi.amazmod.support.SilenceApplicationHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.tinylog.Logger;

import amazmod.com.transport.data.SilenceApplicationData;

public class TransportListener {

    private Context context;

    TransportListener(Context context) {
        this.context = context;
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void replyToNotification(NotificationReply notificationReply) {
        ReplyToNotificationLocal replyToNotificationLocal = new ReplyToNotificationLocal(notificationReply.getNotificationReplyData());
        Logger.debug("TransportService replyToNotification: " + notificationReply.toString());
        EventBus.getDefault().post(replyToNotificationLocal);
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void silenceApplication(SilenceApplication silenceApplication) {
        SilenceApplicationData data = silenceApplication.getSilenceApplicationData();
        Logger.debug("TransportService silenceApplication: " + data.getPackageName() + " / Minutes: " + data.getMinutes());
        SilenceApplicationHelper.silenceAppFromNotification(data.getPackageName(),Integer.valueOf(data.getMinutes()));
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void nextMusic(NextMusic nextMusic) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (mAudioManager.isMusicActive()) {
            long eventtime = SystemClock.uptimeMillis();

            KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
            mAudioManager.dispatchMediaKeyEvent(downEvent);

            KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
            mAudioManager.dispatchMediaKeyEvent(upEvent);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void toggleMusic(ToggleMusic toggleMusic) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        long eventtime = SystemClock.uptimeMillis();

        if (mAudioManager.isMusicActive()) {
            KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0);
            mAudioManager.dispatchMediaKeyEvent(downEvent);

            KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 0);
            mAudioManager.dispatchMediaKeyEvent(upEvent);
        } else {
            KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
            mAudioManager.dispatchMediaKeyEvent(downEvent);

            KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
            mAudioManager.dispatchMediaKeyEvent(upEvent);
        }
    }
}
