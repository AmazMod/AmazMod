package com.amazmod.service.util;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.amazmod.service.Constants;
import com.amazmod.service.ui.ConfirmationWearActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class DeviceUtil {

    public static boolean isDeviceLocked(Context context) {
        boolean isLocked = false;

        // First we check the locked state
        try {
            KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            boolean inKeyguardRestrictedInputMode = keyguardManager.inKeyguardRestrictedInputMode();

            if (inKeyguardRestrictedInputMode) {
                isLocked = true;

            } else {
                // If password is not set in the settings, the inKeyguardRestrictedInputMode() returns false,
                // so we need to check if screen on for this case
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                    isLocked = !powerManager.isInteractive();
                } else {
                    //noinspection deprecation
                    isLocked = !powerManager.isScreenOn();
                }
            }
        } catch (NullPointerException e) {
            Log.e(Constants.TAG, "iDeviceLocked exception: " + e.toString());
        }


        return isLocked;
    }

    public static boolean isDNDActive(Context context, ContentResolver cr) {

        boolean dndEnabled = false;
        try {

            int zenModeValue = Settings.Global.getInt(cr, "zen_mode");

            switch (zenModeValue) {
                case 0:
                    Log.d(Constants.TAG, "DnD lowSDK: OFF");
                    dndEnabled = false;
                    break;
                case 1:
                    Log.d(Constants.TAG, "DnD lowSDK: ON - Priority Only");
                    dndEnabled = true;
                    break;
                case 2:
                    Log.d(Constants.TAG, "DnD lowSDK: ON - Total Silence");
                    dndEnabled = true;
                    break;
                case 3:
                    Log.d(Constants.TAG, "DnD lowSDK: ON - Alarms Only");
                    dndEnabled = true;
                    break;
                default:
                    Log.d(Constants.TAG, "DnD lowSDK Unexpected Value: " + zenModeValue);
                    dndEnabled = false;
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.e(Constants.TAG, "DnD lowSDK exception: " + e.toString());
            dndEnabled = false;
        }

        return dndEnabled;
    }

    public static void installPackage(Context context, String packageName, String filePath) {

        Log.d(Constants.TAG, "PackageReceiver installPackage packageName: " + packageName);
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();

        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(packageName);
        PackageInstaller.Session session = null;

        int sessionId = 0;
        try {
            sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);

            /*
            FileInputStream apkStream = new FileInputStream(filePath);
            OutputStream out = session.openWrite("amazmod_install", 0, -1);

            //Duplicate file to sdcard to test if copy is working fine
            FileOutputStream fos = new FileOutputStream("/sdcard/test.apk");
            long length = 0;
            try {
                byte[] buffer = new byte[1024];
                int c;
                while ((c = apkStream.read(buffer)) != -1) {
                    out.write(buffer, 0, c);
                    fos.write(buffer, 0, c);
                    length++;
                }

            }catch (Exception e) {
                Log.e(Constants.TAG, "PackageReceiver installPackage exception: " + e.toString());
            } finally {
                Log.d(Constants.TAG, "PackageReceiver installPackage length: " + length);
                session.fsync(out);
                apkStream.close();
                out.close();
                fos.close();
            }
            */

            long sizeBytes = 0;
            final File file = new File(filePath);
            if (file.isFile())
                sizeBytes = file.length();

            InputStream in = null;
            OutputStream out = null;
            in = new FileInputStream(filePath);
            out = session.openWrite("amazmod_install", 0, sizeBytes);

            int total = 0;
            byte[] buffer = new byte[65536];
            int c;
            while ((c = in.read(buffer)) != -1)
            {
                total += c;
                out.write(buffer, 0, c);
            }
            session.fsync(out);
            in.close();
            out.close();
            Log.d(Constants.TAG, "DeviceUtil installPackage sizeBytes: " + sizeBytes + " // total: " + total);

            Intent intent = new Intent(context, ConfirmationWearActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.putExtra(Constants.TEXT, "Auto Install");
            intent.putExtra(Constants.TIME, "5");

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,0);
            IntentSender mIntentSender = pendingIntent.getIntentSender();

            for (String s : session.getNames()) {
                Log.i(Constants.TAG, "DeviceUtil installPackage session.getNames: " + s);
            }
            session.commit(mIntentSender);

            //Intent intent = new Intent(Intent.ACTION_MAIN);
            //session.commit(PendingIntent.getBroadcast(context, sessionId,
            //        intent, PendingIntent.FLAG_UPDATE_CURRENT).getIntentSender());

        } catch (Exception e) {
            Log.e(Constants.TAG, "DeviceUtil installPackage exception: " + e.toString());
        } finally {
            if (session != null) {
                Log.i(Constants.TAG, "DeviceUtil installPackage session.close session: " + sessionId);
                session.close();
                Log.i(Constants.TAG, "DeviceUtil installPackage finished");
            }
        }
    }

}
