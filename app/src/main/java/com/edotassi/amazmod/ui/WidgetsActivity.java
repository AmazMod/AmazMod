package com.edotassi.amazmod.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.ResultWidgets;
import com.edotassi.amazmod.receiver.WatchfaceReceiver;
import com.edotassi.amazmod.support.FirebaseEvents;
import com.edotassi.amazmod.util.FilesUtil;
import com.edotassi.amazmod.util.Permissions;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.pixplicity.easyprefs.library.Prefs;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.util.MapTimeZoneCache;

import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.WidgetsData;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.mateware.snacky.Snacky;

public class WidgetsActivity extends BaseAppCompatActivity {


    @BindView(R.id.textView)
    TextView textView;
    @BindView(R.id.widgets_sync_now_button)
    Button widgets_sync_now_button;

    Context mContext;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Widgets");
        }

        ButterKnife.bind(this);

        mContext = this;


        // Sync now button
        widgets_sync_now_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Put data to bundle
                WidgetsData widgetsData = new WidgetsData();
                widgetsData.setPackages("");

                Watch.get().sendWidgetsData(widgetsData).continueWith(new Continuation<ResultWidgets, Object>() {
                    @Override
                    public Object then(@NonNull Task<ResultWidgets> task) throws Exception {
                        if (task.isSuccessful()) {
                            ResultWidgets returedData = task.getResult();
                            WidgetsData widgetsData = returedData.getWidgetsData();
                            String packages = widgetsData.getPackages();
                            Logger.debug("Widgets: "+packages);
                            textView.setText(packages);
                            Logger.debug("Widgets: "+packages);
                        } else {
                            Logger.error(task.getException(), "failed reading widgets");
                        }
                        return null;
                    }
                });

                Snacky.builder()
                        .setActivity(WidgetsActivity.this)
                        .setText(R.string.activity_watchface_data_send)
                        .setDuration(Snacky.LENGTH_SHORT)
                        .build().show();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
