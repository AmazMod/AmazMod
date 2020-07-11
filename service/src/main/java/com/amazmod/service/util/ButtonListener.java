package com.amazmod.service.util;

import android.content.Context;
import android.os.PowerManager;

import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static android.content.Context.POWER_SERVICE;

public class ButtonListener {

    private final int TYPE_KEYBOARD = 1;

    public static final int KEY_DOWN = 64; //S3 key middle up
    public static final int KEY_CENTER = 116; //S3 key up
    public static final int KEY_UP = 63; //S3 key middle down
    public static final int S3_KEY_UP = 116;
    public static final int S3_KEY_MIDDLE_UP = 64;
    public static final int S3_KEY_MIDDLE_DOWN = 63;
    public static final int S3_KEY_DOWN = 158;

    private final int KEY_EVENT_UP = 0;
    private final int KEY_EVENT_PRESS = 1;

    private final int TRIGGER = 600;
    private final int LONG_TRIGGER = TRIGGER * 6;
    private final int LONG_TRIGGER_MAX = TRIGGER * 10;

    private PowerManager powerManager;
    PowerManager.WakeLock wakeLock;



    private boolean listening;

    Thread thread;

    public void start(Context context, final KeyEventListener keyEventListener) {
        if(listening)
            return;

        powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);

        listening = true;
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "AmazTimer:buttonListener");
        wakeLock.acquire();
        thread = new listenerThread(keyEventListener);
        thread.start();
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
            listening = false;
        }
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }

    private class listenerThread extends Thread{
        private String FILE_PATH;

        private LinkedHashMap<Integer, Long> lastEvents = new LinkedHashMap<Integer, Long>() {{
            put(KEY_UP, (long) 0);
            put(KEY_DOWN, (long) 0);
            put(KEY_CENTER, (long) 0);
            put(S3_KEY_DOWN, (long) 0);
        }};

        private KeyEventListener KeyEventListener;

        private listenerThread(KeyEventListener keyEventListener){
            if(SystemProperties.isStratos3())
                FILE_PATH = "/dev/input/event1";
            else
                FILE_PATH = "/dev/input/event2";

            KeyEventListener = keyEventListener;
        }

        public void run(){
            File file = new File(FILE_PATH);

            try {
                FileInputStream fileInputStream = new FileInputStream(file);

                while (true) {
                    if(Thread.currentThread().isInterrupted()){
                        fileInputStream.close();
                        break;
                    }
                    byte[] buffer = new byte[24];
                    fileInputStream.read(buffer, 0, 24);

                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(24);
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    byteBuffer.put(buffer);

                    int timeS = byteBuffer.getInt(0);
                    int timeMS = byteBuffer.getInt(4);
                    short type = byteBuffer.getShort(8);
                    short code = byteBuffer.getShort(10);
                    short value = byteBuffer.getShort(12);

                    if (type != TYPE_KEYBOARD)
                        continue;

                    long now = System.currentTimeMillis();

                    long lastKeyDown = lastEvents.get((int) code);

                    if (value == KEY_EVENT_UP) {
                        long delta = now - lastKeyDown;
                        if ((delta > TRIGGER) && (delta < LONG_TRIGGER))
                            KeyEventListener.onKeyPress(new KeyEvent(code, true));
                        else if (delta < TRIGGER)
                            KeyEventListener.onKeyPress(new KeyEvent(code, false));
                    } else if (value == KEY_EVENT_PRESS)
                        lastKeyDown = now;

                    lastEvents.put((int) code, lastKeyDown);
                }
            } catch (IOException e) {
                Logger.error(e);
            }
        }
    }

    public interface KeyEventListener {
        void onKeyPress(KeyEvent keyEvent);
    }

    public static class KeyEvent{
        private int code;
        private boolean longPress;
        private KeyEvent(int code, boolean isLongPress){
            this.code = code;
            this.longPress = isLongPress;
        }

        public int getCode(){
            return code;
        }

        public boolean isLongPress(){
            return longPress;
        }
    }
}
