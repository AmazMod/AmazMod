package com.amazmod.service.ui;

import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import android.content.Context;
import com.amazmod.service.MainService;
import com.amazmod.service.R;
import com.amazmod.service.springboard.AmazModLauncher;
import com.amazmod.service.springboard.LauncherWearGridActivity;
import com.amazmod.service.util.Overlay_Main;


public class overlay_launcher extends Service implements OnClickListener {

    private View bottomRightView;

    private Button overlayedButton;
    private WindowManager wm;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        overlayedButton = new Button(this);
        overlayedButton.setText("                                 ");
        //overlayedButton.setOnTouchListener(this);
        overlayedButton.setBackgroundColor(0x55fe4444);
        overlayedButton.setOnClickListener(this);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        params.x = 0;
        params.y = 0;
        wm.addView(overlayedButton, params);

        bottomRightView = new View(this);
        WindowManager.LayoutParams bottomRightParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        bottomRightParams.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        bottomRightParams.x = 0;
        bottomRightParams.y = 0;
        bottomRightParams.width = 0;
        bottomRightParams.height = 0;
        wm.addView(bottomRightView, bottomRightParams);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayedButton != null) {
            wm.removeView(overlayedButton);
            wm.removeView(bottomRightView);
            overlayedButton = null;
            bottomRightView = null;
        }
    }


    private Context mContext;
    private static Intent intent;

    /*    @Override
            public View getView(final Context paramContext) {
            this.mContext = paramContext;
            mContext.startService(new Intent(paramContext, MainService.class));
            this.view = LayoutInflater.from(mContext).inflate(R.layout.amazmod_launcher, null);
            init();
            return this.view;
        }
        public void init()
            messages.setImageResource(R.drawable.notify_icon_24);

            Intent = new Intent(mContext, LauncherWearGridActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            messages.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v){
                intent.putExtra(LauncherWearGridActivity.MODE, LauncherWearGridActivity.NOTIFICATIONS);
                mContext.startActivity(intent);
            }
            });

    }*/
    @Override
    public void onClick(View v) {
        Toast.makeText(this, "I DON'T KNOW HOW TO OPEN NOTIFICATION", Toast.LENGTH_SHORT).show();
    }
}


