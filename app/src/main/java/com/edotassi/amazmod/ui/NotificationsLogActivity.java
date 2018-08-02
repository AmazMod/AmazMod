package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.NotificationLogAdapter;
import com.edotassi.amazmod.db.model.NotificationEntity;
import com.edotassi.amazmod.db.model.NotificationEntity_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class NotificationsLogActivity extends AppCompatActivity {

    @BindView(R.id.activity_notifications_log_progress)
    MaterialProgressBar progressBar;

    @BindView(R.id.activity_notifications_log_list)
    ListView listView;

    private NotificationLogAdapter notificationLogAdapter;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications_log);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            //TODO log to crashlitics
        }

        getSupportActionBar().setTitle(R.string.notifications_log);
        ButterKnife.bind(this);

        notificationLogAdapter = new NotificationLogAdapter(this, R.layout.row_notification_log, new ArrayList<NotificationEntity>());
        listView.setAdapter(notificationLogAdapter);

        loadLog();
    }

    @SuppressLint("CheckResult")
    private void loadLog() {
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);

        Flowable.fromCallable(new Callable<List<NotificationEntity>>() {
            @Override
            public List<NotificationEntity> call() {
                return SQLite
                        .select()
                        .from(NotificationEntity.class)
                        .orderBy(NotificationEntity_Table.date.desc())
                        .queryList();
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<List<NotificationEntity>>() {
                    @Override
                    public void accept(final List<NotificationEntity> notificationEntities) throws Exception {
                        NotificationsLogActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notificationLogAdapter.clear();
                                notificationLogAdapter.addAll(notificationEntities);
                                notificationLogAdapter.notifyDataSetChanged();

                                progressBar.setVisibility(View.GONE);
                                listView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
    }
}
