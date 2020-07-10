package com.amazmod.service.sleep;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amazmod.service.sleep.alarm.alarmActivity;
import com.amazmod.service.sleep.alarm.alarmReceiver;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;

import org.tinylog.Logger;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.SleepData;

import static amazmod.com.transport.data.SleepData.actions;

public class sleepService extends Service implements Transporter.DataListener {

    private static Transporter sleepTransporter;

    public static Transporter getTransporter() {
        return sleepTransporter;
    }

    private void registerListener() {
        sleepTransporter = Transporter.get(this, Transport.NAME_SLEEP);
        sleepTransporter.addDataListener(this);
        if (!sleepTransporter.isTransportServiceConnected())
            sleepTransporter.connectTransportService();
    }

    private void unregisterListener() {
        sleepTransporter.removeDataListener(this);
        if (sleepTransporter.isTransportServiceConnected())
            sleepTransporter.disconnectTransportService();
        sleepTransporter = null;
    }

    @Override
    public void onDataReceived(TransportDataItem transportDataItem) {
        if (!transportDataItem.getAction().equals(Transport.SLEEP_DATA))
            return;
        SleepData sleepData = new SleepData();
        sleepData.fromDataBundle(transportDataItem.getData());
        switch (sleepData.getAction()) {
            case actions.ACTION_START_TRACKING:
                sleepUtils.startTracking(this);
                break;
            case actions.ACTION_STOP_TRACKING:
                sleepUtils.stopTracking(this);
                break;
            case actions.ACTION_SET_SUSPENDED:
                sleepStore.getInstance().setSuspended(sleepData.isSuspended(), this);
                break;
            case actions.ACTION_SET_BATCH_SIZE:
                sleepStore.getInstance().setBatchSize(sleepData.getBatchsize());
                break;
            case actions.ACTION_START_ALARM:
                Intent alarmIntent = new Intent(this, alarmActivity.class);
                alarmIntent.putExtra("DELAY", sleepData.getDelay());
                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(alarmIntent);
                break;
            case actions.ACTION_STOP_ALARM:
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(new Intent(alarmActivity.INTENT_CLOSE));
                break;
            case actions.ACTION_UPDATE_ALARM:
                alarmReceiver.setAlarm(this, sleepData.getTimestamp());
                break;
            case actions.ACTION_SHOW_NOTIFICATION:
                sleepUtils.postNotification(sleepData.getTitle(), sleepData.getText(), this);
                break;
            case actions.ACTION_HINT:
                sleepUtils.startHint(sleepData.getRepeat(), this);
                break;
            default:
                break;
        }
        Logger.debug("sleep: Received data with action " + sleepData.getAction());
    }

    public static void send(DataBundle dataBundle) {
        if (!sleepTransporter.isTransportServiceConnected()) {
            sleepTransporter.connectTransportService();
        }
        String action = Transport.SLEEP_DATA;

        if (dataBundle != null) {
            Logger.debug("Sleep send: " + action);
            sleepTransporter.send(action, dataBundle, dataTransportResult -> Logger.debug("Send result: " + dataTransportResult.toString()));
        } else {
            Logger.error("Sleep send: can't send a sleep action without DataBundle!");
        }
    }

    public void onCreate() {
        registerListener();
    }

    public void onDestroy() {
        unregisterListener();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
