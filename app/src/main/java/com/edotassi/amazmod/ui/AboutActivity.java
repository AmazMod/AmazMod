package com.edotassi.amazmod.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.edotassi.amazmod.BuildConfig;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.OutcomingNotification;
import com.mikepenz.iconics.context.IconicsLayoutInflater;
import com.mikepenz.iconics.context.IconicsLayoutInflater2;

import amazmod.com.transport.data.NotificationData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class AboutActivity extends AppCompatActivity {

    @BindView(R.id.activity_about_version)
    TextView version;
    @BindView(R.id.button1)
    Button button1;
    @BindView(R.id.button2)
    Button button2;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new IconicsLayoutInflater2(getDelegate()));

        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.about);

        setContentView(R.layout.activity_about);

        ButterKnife.bind(this);

        version.setText(BuildConfig.VERSION_NAME);
        button1.setText("Cst Test");
        button2.setText("Std Test");
    }

    @OnClick(R.id.button1)
    public void click1() {

        sendTestMessage(true);

    }

    @OnClick(R.id.button2)
    public void click2() {

        sendTestMessage(false);

    }

    private void sendTestMessage(boolean customUI) {

        NotificationData notificationData = new NotificationData();

        if (customUI) {
            notificationData.setForceCustom(true);
        } else {
            notificationData.setForceCustom(false);
        }

        notificationData.setId(999);
        notificationData.setKey("amazmod|test|999");
        notificationData.setTitle("AmazMod");
        notificationData.setText("Test Notification");
        notificationData.setTime("00:00");
        notificationData.setHideReplies(true);
        notificationData.setHideButtons(false);

        try {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_launcher_round);
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] intArray = new int[width * height];
            bitmap.getPixels(intArray, 0, width, 0, 0, width, height);

            notificationData.setIcon(intArray);
            notificationData.setIconWidth(width);
            notificationData.setIconHeight(height);
        } catch (Exception e) {
            notificationData.setIcon(new int[]{});
            System.out.println("AmazMod AboutActivity notificationData Failed to get bitmap " + e.toString());
        }

        HermesEventBus.getDefault().post(new OutcomingNotification(notificationData));

        Toast.makeText(this, "Test Notification Sent", Toast.LENGTH_SHORT).show();
        System.out.println("AmazMod AboutActivity notificationData: " + notificationData.toString());

    }
}
