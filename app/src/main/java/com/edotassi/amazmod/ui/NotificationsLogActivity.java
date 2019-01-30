package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.NotificationLogAdapter;
import com.edotassi.amazmod.db.model.NotificationEntity;
import com.edotassi.amazmod.db.model.NotificationEntity_Table;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import amazmod.com.transport.Constants;
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
    private static boolean showOnlySelected;

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

        showOnlySelected = Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_LOG_SHOW_ONLY_SELECTED, false);
        loadLog(showOnlySelected);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_notifications_log, menu);
        menu.findItem(R.id.activity_notifications_log_show_only_selected).setChecked(showOnlySelected);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.activity_notifications_log_delete_all) {

            new MaterialDialog.Builder(this)
                    .title(R.string.are_you_sure)
                    .content(R.string.cannot_be_undone)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Delete.table(NotificationEntity.class);
                            notificationLogAdapter.clear();
                            notificationLogAdapter.notifyDataSetChanged();
                        }
                    }).show();
            return true;

        }

        if (id == R.id.activity_notifications_log_show_only_selected) {
            item.setChecked(!item.isChecked());
            showOnlySelected = item.isChecked();
            loadLog(showOnlySelected);
            Prefs.putBoolean(Constants.PREF_NOTIFICATIONS_LOG_SHOW_ONLY_SELECTED, showOnlySelected);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("CheckResult")
    private void loadLog(final boolean loadSelectedOnly) {
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);

        Flowable.fromCallable(new Callable<List<NotificationEntity>>() {
            @Override
            public List<NotificationEntity> call() {
                if (loadSelectedOnly)
                    return SQLite
                            .select()
                            .from(NotificationEntity.class)
                            .where(NotificationEntity_Table.filterResult.notEq(Constants.FILTER_PACKAGE))
                            .orderBy(NotificationEntity_Table.date.desc())
                            .queryList();
                else
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
