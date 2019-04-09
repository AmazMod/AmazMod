package com.amazmod.service.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.adapters.NotificationListAdapter;
import com.amazmod.service.helper.RecyclerTouchListener;
import com.amazmod.service.support.NotificationInfo;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.NotificationWearActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class WearNotificationsFragment extends Fragment {

    static WearNotificationsFragment instance = null;

    private RelativeLayout wearNotificationsFrameLayout;
	private WearableListView listView;
    private TextView mHeader;
    private ProgressBar progressBar;

    private Context mContext;

    private List<NotificationInfo> notificationInfoList;
    private NotificationListAdapter mAdapter;

    private static final String REFRESH = "Refresh";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Logger.info("WearNotificationsFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.info("WearNotificationsFragment onCreate");

        instance = this;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Logger.info("WearNotificationsFragment onCreateView");
        return inflater.inflate(R.layout.activity_wear_notifications, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.info("WearNotificationsFragment onViewCreated");

        init();

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onItemClick(int position) {

        Logger.info("WearNotificationsFragment onClick position: " + position);

        if (notificationInfoList.get(position).getNotificationTitle().equals(REFRESH)) {

            notificationInfoList.clear();
            mAdapter.clear();
            loadNotifications();

        } else
            showNotification(position);

    }

    public void onItemLongClick(int position) {

        Logger.info("WearNotificationsFragment onLongClick position: " + position);

        if (!REFRESH.equals(notificationInfoList.get(position).getNotificationTitle()))
            deleteNotification(position);

    }

    private void init() {
        wearNotificationsFrameLayout = getActivity().findViewById(R.id.wear_notifications_frame_layout);
        listView = getActivity().findViewById(R.id.wear_notifications_list);
        mHeader = getActivity().findViewById(R.id.wear_notifications_header);
        progressBar = getActivity().findViewById(R.id.wear_notifications_loading_spinner);

        listView.setLongClickable(true);
        listView.setGreedyTouchMode(true);
        listView.addOnScrollListener(mOnScrollListener);

        listView.addOnItemTouchListener(new RecyclerTouchListener(mContext, listView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Logger.debug("WearNotificationsFragment addOnItemTouchListener onClick");
                onItemClick(position);
            }

            @Override
            public void onLongClick(View view, int position) {
                Logger.debug("WearNotificationsFragment addOnItemTouchListener onLongClick");
                onItemLongClick(position);
            }
        }));

        mHeader.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if (!notificationInfoList.isEmpty())
                    new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.clear_notifications))
                        .setMessage(getResources().getString(R.string.confirmation))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                NotificationStore.clear();
                                resetNotificationsCounter();
                                getActivity().finish();
                            }})
                        .setNegativeButton(android.R.string.no, null).show();
                return false;
            }
        });

        loadNotifications();
    }

    @SuppressLint("CheckResult")
    public void loadNotifications() {
        Logger.info("WearNotificationsFragment loadNotifications");

        //Return if there is no activity to avoid crashes
        if (getActivity() == null)
            return;

        wearNotificationsFrameLayout.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        final Drawable drawable = getResources().getDrawable(R.drawable.outline_refresh_white_24);

        Flowable.fromCallable(new Callable<List<NotificationInfo>>() {
            @Override
            public List<NotificationInfo> call() throws Exception {
                Logger.debug("WearAppsFragment loadNotifications call");

                List<NotificationInfo> notificationInfoList = new ArrayList<>();
                if (NotificationStore.getKeySet() != null) {
                    for (String key : NotificationStore.getKeySet()) {
                        Logger.debug("WearAppsFragment loadNotifications adding key: " + key);
                        notificationInfoList.add(new NotificationInfo(NotificationStore.getCustomNotification(key), key));
                    }
                }

                if (!notificationInfoList.isEmpty())
                    notificationInfoList.add(new NotificationInfo(REFRESH, "Reload items","", drawable, null, "", "0"));

                sortNotifications(notificationInfoList);
                WearNotificationsFragment.this.notificationInfoList = notificationInfoList;
                return notificationInfoList;
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<List<NotificationInfo>>() {
                    @Override
                    public void accept(final List<NotificationInfo> notificationInfoList) throws Exception {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Logger.debug("WearNotificationsFragment loadNotifications run");
                                mAdapter = new NotificationListAdapter(mContext, notificationInfoList);
                                if (notificationInfoList.isEmpty())
                                    mHeader.setText(getResources().getString(R.string.empty));
                                else
                                    mHeader.setText(getResources().getString(R.string.notifications));
                                listView.setAdapter(mAdapter);
                                listView.post(new Runnable() {
                                    public void run() {
                                        Logger.debug("WearNotificationsFragment loadNotifications scrollToTop");
                                        listView.smoothScrollToPosition(0);
                                    }
                                });
                                progressBar.setVisibility(View.GONE);
                                listView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });

    }

    private void showNotification(final int itemChosen) {

        final String key = notificationInfoList.get(itemChosen).getKey();

        Logger.debug("WearNotificationsFragment showNotification key: " + key);

        Intent intent = new Intent(mContext, NotificationWearActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(NotificationWearActivity.KEY, key);
        intent.putExtra(NotificationWearActivity.MODE, NotificationWearActivity.MODE_VIEW);

        mContext.startActivity(intent);

    }

    private void deleteNotification(final int itemChosen) {

        final String key = notificationInfoList.get(itemChosen).getKey();

        Logger.debug("WearNotificationsFragment deleteNotification key: " + key);

        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.delete))
                .setMessage(getResources().getString(R.string.confirmation))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        NotificationStore.removeCustomNotification(key);
                        NotificationStore.setNotificationCount(mContext);
                        loadNotifications();
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();

    }


    // The following code ensures that the title scrolls as the user scrolls up
    // or down the list
    private WearableListView.OnScrollListener mOnScrollListener =
            new WearableListView.OnScrollListener() {
                @Override
                public void onAbsoluteScrollChange(int i) {
                    // Only scroll the title up from its original base position
                    // and not down.
                    if (i > 0) {
                        mHeader.setY(-i);
                    }
                }

                @Override
                public void onScroll(int i) {
                    // Placeholder
                }

                @Override
                public void onScrollStateChanged(int i) {
                    // Placeholder
                }

                @Override
                public void onCentralPositionChanged(int i) {
                    // Placeholder
                }
            };

    private void sortNotifications(List<NotificationInfo> notificationInfoList) {
        Collections.sort(notificationInfoList, new Comparator<NotificationInfo>() {
            @Override
            public int compare(NotificationInfo o1, NotificationInfo o2) {
                return o2.getId().compareTo(o1.getId());
            }
        });
    }

    private void resetNotificationsCounter() {
        String data = Settings.System.getString(mContext.getContentResolver(), "CustomWatchfaceData");
        if (data == null || data.equals(""))
            Settings.System.putString(mContext.getContentResolver(), "CustomWatchfaceData", "{}");

        try {
            JSONObject json_data = new JSONObject(data);
            json_data.put("notifications", 0);
            Settings.System.putString(mContext.getContentResolver(), "CustomWatchfaceData", json_data.toString());
        } catch (JSONException e) {
            Logger.error("AmazModLauncher refreshMessages JSONException: " + e.toString());
        }
    }

    public static WearNotificationsFragment newInstance() {
        Logger.info("WearNotificationsFragment newInstance");
        return new WearNotificationsFragment();
    }

    public static WearNotificationsFragment getInstance() {
        return instance;
    }
}