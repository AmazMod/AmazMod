package com.amazmod.service;

import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class PackageReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context,final Intent intent) {
        final String action = intent.getAction();
        Log.d(Constants.TAG,"PackageReceiver onReceive action: " + action + " // " + intent.getDataString());

        if (action != null) {
            try {
                //This action can be used from adb for testing silent update
                if (action.contains("MY_PACKAGE_REPLACED")) {
                    Log.d(Constants.TAG,"PackageReceiver onReceive " + action);
                    if (intent.getExtras() != null) {
                        if (intent.getExtras().getString("TAG") != null)
                            if (!intent.getExtras().getString("TAG").equals("")) {
                                try {
                                    installPackage(context, context.getPackageName(), context.getPackageName(),
                                            new FileInputStream(intent.getExtras().getString("TAG")));
                                } catch (IOException | NullPointerException e) {
                                    e.printStackTrace();
                                }
                            }
                    }
                    return;
                }
                if (intent.getDataString().contains(context.getPackageName())) {
                    //Finish update by running install script or running necessary commands
                    if (action.contains("PACKAGE_REPLACED") || action.contains("PACKAGE_ADDED")) {
                        final File script = new File("/sdcard/install_apk.sh");
                        if (script.exists()) {
                            String command = String.format("busybox sh %s", script.getAbsolutePath());
                            Log.d(Constants.TAG, "PackageReceiver onReceive command: " + command);
                            try {
                                Runtime.getRuntime().exec(command, null, Environment.getExternalStorageDirectory());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        } else {
                            DevicePolicyManager mDPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                            try {
                                Log.d(Constants.TAG, "PackageReceiver onReceive restartLauncher");
                                Runtime.getRuntime().exec("adb shell am force-stop com.huami.watch.launcher");
                                Log.i(Constants.TAG, "PackageReceiver onReceive DPM: " + mDPM.getActiveAdmins());
                                if (!mDPM.isDeviceOwnerApp(context.getPackageName())) {
                                    Runtime.getRuntime().exec("adb shell dpm set-device-owner com.amazmod.service/.AdminReceiver");
                                }
                            } catch (IOException e) {
                                Log.e(Constants.TAG, "PackageReceiver onReceive IOxception: " + e.toString());
                            } catch (NullPointerException ex){
                                Log.e(Constants.TAG, "PackageReceiver onReceive NullPointException: " + ex.toString());
                            }
                        }
                    //Test action, does not seem to work
                    } else if (action.contains("MAIN")) {
                        try {
                            installPackage(context, context.getPackageName(), context.getPackageName(), new FileInputStream("/sdcard/service.apk"));
                        } catch (IOException e) {
                            Log.e(Constants.TAG, "PackageReceiver onReceive exception: " + e.toString());
                            e.printStackTrace();
                        }
                    }
                }
            } catch (NullPointerException ex) {
                Log.e(Constants.TAG, "PackageReceiver onReceive exception: " + ex.toString());
            }
        }
    }

    //Silent update
    public static void installPackage(Context context, String installSessionId, String packageName, InputStream apkStream) throws IOException {

        Log.d(Constants.TAG, "PackageReceiver onReceive packageName: " + packageName);
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();

        //Clean old sessions, if any
        List<PackageInstaller.SessionInfo> allSessions = packageInstaller.getAllSessions();
        for (PackageInstaller.SessionInfo se: allSessions) {
            packageInstaller.abandonSession(se.getSessionId());
        }

        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(packageName);
        PackageInstaller.Session session = null;

        try {
            int sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);

            OutputStream out = session.openWrite(installSessionId, 0, -1);

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

            Intent intent = new Intent(Intent.ACTION_MAIN);

            session.commit(PendingIntent.getBroadcast(context, sessionId,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT).getIntentSender());

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
