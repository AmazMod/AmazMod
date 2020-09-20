package com.edotassi.amazmod.sleep;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;

import org.tinylog.Logger;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.SleepData;
import static amazmod.com.transport.data.SleepData.actions;

public class sleepListener implements Transporter.DataListener {

    private static Transporter sleepTransporter;
    private static sleepListener instance;

    private Context context;

    public static sleepListener getInstance(){
        if(instance == null)
            instance = new sleepListener();
        return instance;
    }

    private void setContext(Context context){
        this.context = context;
    }

    public static void register(Context context){
        Logger.debug("Registering sleepListener to sleepTransporter...");
        sleepTransporter = Transporter.get(context, Transport.NAME_SLEEP);
        sleepTransporter.addDataListener(getInstance());
        if(!sleepTransporter.isTransportServiceConnected())
            sleepTransporter.connectTransportService();
        getInstance().setContext(context);
    }

    public static void unregister(){
        sleepTransporter.removeDataListener(getInstance());
        if(sleepTransporter.isTransportServiceConnected())
            sleepTransporter.disconnectTransportService();
        sleepTransporter = null;
        instance = null;
    }

    @Override
    public void onDataReceived(TransportDataItem transportDataItem) {
        if(!transportDataItem.getAction().equals(Transport.SLEEP_DATA))
            return;
        SleepData sleepData = new SleepData();
        sleepData.fromDataBundle(transportDataItem.getData());
        Logger.debug("sleep: Received action " + sleepData.getAction() + " from watch");
        sleepUtils.broadcast(context, sleepData);
    }

    public static void send(DataBundle dataBundle) {
        if(!BluetoothAdapter.getDefaultAdapter().isEnabled())
            return;
        if (!sleepTransporter.isTransportServiceConnected())
            sleepTransporter.connectTransportService();

        String action = Transport.SLEEP_DATA;

        if (dataBundle != null) {
            Logger.debug("Sleep send: " + action);
            sleepTransporter.send(action, dataBundle, dataTransportResult -> Logger.debug("Send result: " + dataTransportResult.toString()));
        } else {
            Logger.error("Sleep send: can't send a sleep action without DataBundle!");
        }
    }
}
