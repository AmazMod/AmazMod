package com.amazmod.service.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.support.wearable.view.WearableListView;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.adapters.AppInfoAdapter;
import com.amazmod.service.support.AppInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;

public class WearAppsFragment extends Fragment implements WearableListView.ClickListener {

    private View infoView;
    private ScrollView scrollView;

    private RelativeLayout wearAppsFrameLayout;
	private WearableListView listView;
	private Button buttonClose, buttonClear, buttonUninstall;
    private TextView mHeader, appName, appPackage, appVersion, appSize;
    private ImageView appIcon;
    private ProgressBar progressBar;

    private Context mContext;

    private List<AppInfo> appInfoList;
    private AppInfoAdapter mAdapter;

    private static int appChosen = 0;

    private final int UNINSTALL_REQUEST_CODE = 1;
    private static final String REFRESH = "Refresh";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Log.i(Constants.TAG,"WearAppsFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(Constants.TAG,"WearAppsFragment onCreate");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(Constants.TAG,"WearAppsFragment onCreateView");
        return inflater.inflate(R.layout.activity_wear_apps, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(Constants.TAG,"WearAppsFragment onViewCreated");
        updateContent();

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTopEmptyRegionClick() {
        //Prevent NullPointerException
        //Toast.makeText(this, "Top empty area tapped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {

        final int itemChosen = viewHolder.getPosition();
        Log.i(Constants.TAG,"WearAppsFragment onClick itemChosen: " + itemChosen);
        if (appInfoList.get(itemChosen).getAppName().equals(REFRESH)) {

            appInfoList.clear();
            mAdapter.clear();
            wearAppsFrameLayout.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            loadApps();

        } else
            showAppInfo(itemChosen);

        //Toast.makeText(mContext, "Selected: " + appInfoList.get(itemChosen).getAppName(), Toast.LENGTH_SHORT).show();

    }

    @SuppressLint("ClickableViewAccessibility")
    private void updateContent() {

        wearAppsFrameLayout = getActivity().findViewById(R.id.wear_apps_frame_layout);
        listView = getActivity().findViewById(R.id.wear_apps_list);
        mHeader = getActivity().findViewById(R.id.wear_apps_header);

        infoView = getActivity().findViewById(R.id.wear_apps_info_layout);
        scrollView = getActivity().findViewById(R.id.wear_apps_info_scrollview);
        progressBar = getActivity().findViewById(R.id.wear_apps_loading_spinner);
        appIcon = getActivity().findViewById(R.id.wear_apps_info_icon);
        appName = getActivity().findViewById(R.id.wear_apps_info_name);
        appVersion = getActivity().findViewById(R.id.wear_apps_info_version);
        appPackage = getActivity().findViewById(R.id.wear_apps_info_package);
        appSize = getActivity().findViewById(R.id.wear_apps_info_size);
        buttonClose = getActivity().findViewById(R.id.wear_apps_info_buttonClose);
        buttonClear = getActivity().findViewById(R.id.wear_apps_info_buttonClear);
        buttonUninstall = getActivity().findViewById(R.id.wear_apps_info_buttonUninstall);

        wearAppsFrameLayout.setVisibility(View.GONE);
        infoView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        loadApps();
    }

    @SuppressLint("CheckResult")
    private void loadApps() {
        Log.i(Constants.TAG,"WearAppsFragment loadApps");
        wearAppsFrameLayout.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        final Drawable drawable = getResources().getDrawable(R.drawable.outline_refresh_white_24);

        Flowable.fromCallable(new Callable<List<AppInfo>>() {
            @Override
            public List<AppInfo> call() throws Exception {
                Log.i(Constants.TAG,"WearAppsFragment loadApps call");
                List<PackageInfo> packageInfoList = mContext.getPackageManager().getInstalledPackages(0);

                List<AppInfo> appInfoList = new ArrayList<>();

                for (PackageInfo packageInfo : packageInfoList) {

                    boolean isSystemApp = (packageInfo.applicationInfo.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) > 0;
                    if  (!isSystemApp) {
                        AppInfo appInfo = createAppInfo(packageInfo);
                        appInfoList.add(appInfo);
                    }
                }

                sortAppInfo(appInfoList);
                AppInfo close = new AppInfo(REFRESH, "Reload apps", "", "0", drawable);
                appInfoList.add(close);
                WearAppsFragment.this.appInfoList = appInfoList;
                return appInfoList;
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<List<AppInfo>>() {
                    @Override
                    public void accept(final List<AppInfo> appInfoList) throws Exception {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(Constants.TAG,"WearAppsFragment loadApps run");
                                mAdapter = new AppInfoAdapter(mContext, appInfoList);
                                if (appInfoList.isEmpty())
                                    mHeader.setText(getResources().getString(R.string.empty));
                                else
                                    mHeader.setText(getResources().getString(R.string.user_apps));
                                listView.setAdapter(mAdapter);
                                listView.post(new Runnable() {
                                    public void run() {
                                        Log.d(Constants.TAG, "WearAppsFragment loadApps scrollToTop");
                                        listView.smoothScrollToPosition(0);
                                    }
                                });
                                progressBar.setVisibility(View.GONE);
                                listView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });

        listView.setLongClickable(true);
        listView.setGreedyTouchMode(true);
        listView.addOnScrollListener(mOnScrollListener);
        listView.setClickListener(this);
    }

    private void showAppInfo(final int itemChosen) {
        setButtonTheme(buttonClose, getResources().getString(R.string.close));
        setButtonTheme(buttonClear, getResources().getString(R.string.clear_data));
        setButtonTheme(buttonUninstall, getResources().getString(R.string.uninstall));
        listView.setGreedyTouchMode(false);

        final String pkgName = appInfoList.get(itemChosen).getPackageName();
        appChosen = itemChosen;

        appIcon.setImageDrawable(appInfoList.get(itemChosen).getIcon());
        appName.setText(appInfoList.get(itemChosen).getAppName());
        appPackage.setText(pkgName);
        appVersion.setText(appInfoList.get(itemChosen).getVersionName());
        appSize.setText(appInfoList.get(itemChosen).getSize());

        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(Constants.TAG,"WearAppsFragment showAppInfo buttonClose");
                scrollView.post(new Runnable() {
                    public void run() {
                        Log.d(Constants.TAG, "WearAppsFragment showAppInfo scrollToTop");
                        //scrollView.fullScroll(scrollView.FOCUS_UP);
                        scrollView.scrollTo(0, scrollView.getTop());
                    }
                });
                infoView.setVisibility(View.GONE);
                wearAppsFrameLayout.setVisibility(View.VISIBLE);
                listView.setVisibility(View.VISIBLE);
            }
        });

        buttonUninstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(Constants.TAG,"WearAppsFragment showAppInfo buttonUninstall");
                uninstallPackage(mContext, pkgName);
            }
        });

        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(Constants.TAG,"WearAppsFragment showAppInfo buttonClear");
                clearPackage(mContext, pkgName);
            }
        });

        wearAppsFrameLayout.setVisibility(View.GONE);
        listView.setVisibility(View.GONE);
        infoView.setVisibility(View.VISIBLE);
    }

    private void hideAppInfo(){
        infoView.setVisibility(View.GONE);
        wearAppsFrameLayout.setVisibility(View.VISIBLE);
        listView.setVisibility(View.VISIBLE);
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

    private void sortAppInfo(List<AppInfo> appInfoList) {
        Collections.sort(appInfoList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo o1, AppInfo o2) {
                return o1.getAppName().compareTo(o2.getAppName());
            }
        });
    }

    private AppInfo createAppInfo(PackageInfo packageInfo) {

        final String pkgName = packageInfo.packageName;

        final AppInfo appInfo = new AppInfo();
        appInfo.setPackageName(packageInfo.packageName);
        appInfo.setAppName(packageInfo.applicationInfo.loadLabel(mContext.getPackageManager()).toString());
        appInfo.setVersionName(packageInfo.versionName);
        appInfo.setIcon(packageInfo.applicationInfo.loadIcon(mContext.getPackageManager()));

        PackageManager pm = mContext.getPackageManager();
        try {
            Method getPackageSizeInfo = pm.getClass().getMethod(
                    "getPackageSizeInfo", String.class, IPackageStatsObserver.class);
            getPackageSizeInfo.invoke(pm, pkgName,
                    new IPackageStatsObserver.Stub() {
                        @Override
                        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
                            final String size = Formatter.formatFileSize(mContext, pStats.codeSize + pStats.cacheSize + pStats.dataSize);
                            appInfo.setSize(size);
                            Log.i(Constants.TAG, "WearAppsFragment pkgName: " + pkgName + " codeSize: "
                                    + pStats.codeSize + " cacheSize: " + pStats.cacheSize + " dataSize: " + pStats.dataSize);
                        }
                    });
        } catch (Exception ex) {
            appInfo.setSize("Unknown Size");
            Log.e(Constants.TAG, "WearAppsFragment createAppInfo NoSuchMethodException");
            ex.printStackTrace();
        }

        return appInfo;
    }

    public void uninstallPackage(Context context, String packageName) {

        Log.i(Constants.TAG,"WearAppsFragment uninstallPackage packageName: " + packageName);

        /* Silent uninstall - not working
        ComponentName name = new ComponentName(getActivity().getApplication().getPackageName(), AdminReceiver.class.getCanonicalName());
        PackageManager packageManger = context.getPackageManager();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            PackageInstaller packageInstaller = packageManger.getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(packageName);
            int sessionId = 0;
            try {
                sessionId = packageInstaller.createSession(params);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            packageInstaller.uninstall(packageName, PendingIntent.getBroadcast(context, sessionId,
                    new Intent("android.intent.action.MAIN"), 0).getIntentSender());
            return true;
        }
        System.err.println("old sdk");
        return false; */

        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + packageName));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        startActivityForResult(intent, UNINSTALL_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UNINSTALL_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.d(Constants.TAG, "WearAppsFragment onActivityResult RESULT_OK appChosen: " + appChosen);
                Toast.makeText(mContext, appInfoList.get(appChosen).getAppName() + "uninstalled successfully!", Toast.LENGTH_SHORT).show();
                scrollView.post(new Runnable() {
                    public void run() {
                        Log.d(Constants.TAG, "WearAppsFragment onActivityResult scrollToTop");
                        //scrollView.fullScroll(scrollView.FOCUS_UP);
                        scrollView.scrollTo(0, scrollView.getTop());
                    }
                });
                //appIcon.requestFocus();
                appInfoList.remove(appChosen);
                mAdapter.notifyDataSetChanged();
                hideAppInfo();
                appChosen = 0;
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(Constants.TAG, "WearAppsFragment onActivityResult RESULT_CANCELED");
            } else if (resultCode == RESULT_FIRST_USER) {
                Log.d(Constants.TAG, "WearAppsFragment onActivityResult RESULT_FIRST_USER");
            }
        }
    }

    public void clearPackage(Context context, String packageName) {
        Log.i(Constants.TAG,"WearAppsFragment clearPackage packageName: " + packageName);

        final String command = String.format("pm force-stop %s;pm clear %s;exit", packageName, packageName);

        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.clear_app_data))
                .setMessage(getResources().getString(R.string.confirmation))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        runCommand(command);
                    }})
                .setNegativeButton(android.R.string.no, null).show();

    }

    private void setButtonTheme(Button button, String string) {
        button.setIncludeFontPadding(false);
        button.setMinHeight(24);
        button.setMinWidth(120);
        button.setText(string);
        button.setAllCaps(true);
        button.setTextColor(Color.parseColor("#000000"));
        button.setBackground(mContext.getDrawable(R.drawable.reply_grey));
    }

    private void runCommand(String command) {
        Log.d(Constants.TAG, "WearAppsFragment runCommand: " + command);

        if (!command.isEmpty()) {
            try {
                Runtime.getRuntime().exec(new String[]{"adb", "shell", command},
                        null, Environment.getExternalStorageDirectory());
            } catch (Exception e) {
                Log.e(Constants.TAG, "WearAppsFragment runCommand exception: " + e.toString());
            }
        }
    }

    public static WearAppsFragment newInstance() {
        Log.i(Constants.TAG,"WearAppsFragment newInstance");
        return new WearAppsFragment();
    }
}