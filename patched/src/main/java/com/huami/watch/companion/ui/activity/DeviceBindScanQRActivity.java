package com.huami.watch.companion.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.view.SurfaceHolder;
import android.view.View;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.huami.watch.companion.bind.BindHelper;
import com.huami.watch.companion.initial.InitialState;
import com.huami.watch.companion.qrcode.CaptureActivityHandler;
import com.huami.watch.companion.qrcode.InactivityTimer;
import com.huami.watch.companion.util.Analytics;
import com.huami.watch.hmwatchmanager.view.AlertDialogFragment;

import java.util.Vector;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexAdd;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexReplace;

/**
 * Created by edoardotassinari on 30/01/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class DeviceBindScanQRActivity extends Activity
        implements SurfaceHolder.Callback{

    @DexIgnore
    private InactivityTimer j;

    @DexIgnore
    private CaptureActivityHandler a;

    @DexIgnore
    private Vector<BarcodeFormat> h;

    @DexIgnore
    private String i;

    @SuppressLint("ResourceType")
    @DexReplace
    public void handleDecode(Result object) {
        this.j.onActivity();
        this.b();
        String string2 = this.b(object);
        if (BluetoothAdapter.checkBluetoothAddress(string2)) {
            InitialState.BindingState.saveBindingDevice(string2);
            BindHelper.getHelper().notifyStateChanged(3);
            finish();
        }
        else if ("openbluetooth".equals(string2)) {
            final AlertDialogFragment dialog = AlertDialogFragment.newInstance(3);
            dialog.setTitle(this.getString(2131231202));
            dialog.setMessage(this.getString(2131231597));
            dialog.setConfirm(this.getString(2131230827), new ClickListener1(dialog));
            dialog.setCancel(this.getString(2131231205), new ClickListener2(dialog));

            if (!this.isFinishing()) {
                dialog.show(this.getFragmentManager(), "CaptureBTOffDialog");
            }
        } else {
            Analytics.event((Context) this, "scan_qr_failed", 1);
            final AlertDialogFragment dialog = AlertDialogFragment.newInstance(4);
            dialog.setTitle(this.getString(2131231202));
            dialog.setMessage(this.getString(2131231203));
            dialog.setNeutral(this.getString(2131230827), new ClickListener3(dialog));
            dialog.show(this.getFragmentManager(), "captureDialog");
        }
        Analytics.event(this, "ScanCode", 1);
    }

    @DexIgnore
    private void b() {
    }

    @DexIgnore
    private String b(Result object) {
        return "";
    }

    @DexIgnore
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @DexIgnore
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @DexIgnore
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @DexAdd
    private class ClickListener1 implements View.OnClickListener {

        private AlertDialogFragment dialog;

        public ClickListener1(AlertDialogFragment pDialog) {
            dialog = pDialog;
        }

        @Override
        public void onClick(View v) {
            dialog.dismiss();
            DeviceBindScanQRActivity.this.a = new CaptureActivityHandler(DeviceBindScanQRActivity.this, DeviceBindScanQRActivity.this.h, DeviceBindScanQRActivity.this.i);
        }
    }

    @DexAdd
    private class ClickListener2 implements View.OnClickListener {

        private AlertDialogFragment dialog;

        public ClickListener2(AlertDialogFragment pDialog) {
            dialog = pDialog;
        }

        @Override
        public void onClick(View v) {
            dialog.dismiss();
            BindHelper.getHelper().notifyStateChanged(-2);
            DeviceBindScanQRActivity.this.finish();
        }
    }

    @DexAdd
    private class ClickListener3 implements View.OnClickListener {

        private AlertDialogFragment dialog;

        public ClickListener3(AlertDialogFragment pDialog) {
            dialog = pDialog;
        }

        @Override
        public void onClick(View v) {
            dialog.dismiss();
            DeviceBindScanQRActivity.this.a = new CaptureActivityHandler(DeviceBindScanQRActivity.this, DeviceBindScanQRActivity.this.h, DeviceBindScanQRActivity.this.i);
            Analytics.event((Context) DeviceBindScanQRActivity.this, "ScanFailKnow", 1);
        }
    }
}
