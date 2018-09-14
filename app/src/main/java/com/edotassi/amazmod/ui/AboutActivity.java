package com.edotassi.amazmod.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.edotassi.amazmod.BuildConfig;
import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.mikepenz.iconics.context.IconicsLayoutInflater2;
import com.pixplicity.easyprefs.library.Prefs;

import amazmod.com.transport.data.NotificationData;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.mateware.snacky.Snacky;

public class AboutActivity extends AppCompatActivity {

    @BindView(R.id.activity_about_version)
    TextView version;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new IconicsLayoutInflater2(getDelegate()));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            System.out.println("AmazMod AboutActivity onCreate exception: " + exception.toString());
            //TODO log to crashlitics
        }
        getSupportActionBar().setTitle(R.string.about);

        ButterKnife.bind(this);

        version.setText(BuildConfig.VERSION_NAME);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case (R.id.action_activity_about_custom_ui_test): {
                sendTestMessage('C');
                break;
            }
            case (R.id.action_activity_about_standard_test): {
                sendTestMessage('S');
                break;
            }
            case (R.id.action_activity_about_revoke_device_owner): {
                sendTestMessage('R');
                break;
            }
            case (R.id.action_activity_about_low_power_mode): {
                sendTestMessage('L');
                break;
            }
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void sendTestMessage(char type) {
        NotificationData notificationData = new NotificationData();

        switch (type) {
            case ('C'): {
                notificationData.setForceCustom(true);
                notificationData.setText("Test Notification");
                break;
            }
            case ('S'): {
                notificationData.setForceCustom(false);
                notificationData.setText("Test Notification");
                break;
            }
            case ('R'): {
                notificationData.setForceCustom(false);
                notificationData.setText("Revoke Admin Owner");
                break;
            }
            case ('L'): {
                notificationData.setForceCustom(false);
                notificationData.setText("Enable Low Power Mode");
                break;
            }
            default:
                System.out.println("AmazMod AboutActivity sendTestMessage: something went wrong...");
        }

        notificationData.setId(999);
        notificationData.setKey("amazmod|test|999");
        notificationData.setTitle("AmazMod");
        notificationData.setTime("00:00");
        notificationData.setVibration(Integer.valueOf(Prefs.getString(Constants.PREF_NOTIFICATIONS_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATIONS_VIBRATION)));
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

        Watch.get().postNotification(notificationData).continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(@NonNull Task<Void> task) throws Exception {
                if (task.isSuccessful()) {
                    Snacky.builder()
                            .setActivity(AboutActivity.this)
                            .setText(R.string.test_notification_sent)
                            .setDuration(Snacky.LENGTH_SHORT)
                            .build().show();
                } else {
                    Snacky.builder()
                            .setActivity(AboutActivity.this)
                            .setText(R.string.failed_to_send_test_notification)
                            .setDuration(Snacky.LENGTH_SHORT)
                            .build().show();
                }
                return null;
            }
        });
    }
/*
    private void sendNotificationWithStandardUI(byte filterResult, StatusBarNotification statusBarNotification) {
        DataBundle dataBundle = new DataBundle();

        if (filterResult == Constants.FILTER_UNGROUP && Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_UNGROUP, false)) {
            int nextId = (int) (long) (System.currentTimeMillis() % 10000L);
            StatusBarNotification sbn = new StatusBarNotification(statusBarNotification.getPackageName(), "",
                    statusBarNotification.getId() + nextId,
                    statusBarNotification.getTag(), 0, 0, 0,
                    statusBarNotification.getNotification(), statusBarNotification.getUser(),
                    statusBarNotification.getPostTime());
            dataBundle.putParcelable("data", StatusBarNotificationData.from(this, sbn, false));
        } else {
            dataBundle.putParcelable("data", StatusBarNotificationData.from(this, statusBarNotification, false));
        }

        //Connect transporter
        Transporter notificationTransporter = TransporterClassic.get(this, "com.huami.action.notification");
        notificationTransporter.connectTransportService();

        notificationTransporter.send("add", dataBundle, new Transporter.DataSendResultCallback() {
            @Override
            public void onResultBack(DataTransportResult dataTransportResult) {
                log.d(dataTransportResult.toString());
            }
        });

        //Disconnect transporter to avoid leaking
        notificationTransporter.disconnectTransportService();

        log.i("NotificationService StandardUI: " + dataBundle.toString());
    } */
}
