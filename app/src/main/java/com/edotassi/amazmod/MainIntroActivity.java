package com.edotassi.amazmod;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.app.NavigationPolicy;
import com.heinrichreimersoftware.materialintro.app.OnNavigationBlockedListener;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;
import com.heinrichreimersoftware.materialintro.slide.Slide;
import com.pixplicity.easyprefs.library.Prefs;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import amazmod.com.models.Reply;

public class MainIntroActivity extends IntroActivity {

    public static final String EXTRA_FULLSCREEN = "com.edotassi.amazmod.EXTRA_FULLSCREEN";
    public static final String EXTRA_SCROLLABLE = "com.edotassi.amazmod.EXTRA_SCROLLABLE";
    public static final String EXTRA_CUSTOM_FRAGMENTS = "com.edotassi.amazmod.EXTRA_CUSTOM_FRAGMENTS";
    public static final String EXTRA_PERMISSIONS = "com.edotassi.amazmod.EXTRA_PERMISSIONS";
    public static final String EXTRA_SHOW_BACK = "com.edotassi.amazmod.EXTRA_SHOW_BACK";
    public static final String EXTRA_SHOW_NEXT = "com.edotassi.amazmod.EXTRA_SHOW_NEXT";
    public static final String EXTRA_SKIP_ENABLED = "com.edotassi.amazmod.EXTRA_SKIP_ENABLED";
    public static final String EXTRA_FINISH_ENABLED = "com.edotassi.amazmod.EXTRA_FINISH_ENABLED";
    public static final String EXTRA_GET_STARTED_ENABLED = "com.edotassi.amazmod.EXTRA_GET_STARTED_ENABLED";

    @Override protected void onCreate(Bundle savedInstanceState){

        Intent intent = getIntent();

        boolean fullscreen = intent.getBooleanExtra(EXTRA_FULLSCREEN, false);
        boolean scrollable = intent.getBooleanExtra(EXTRA_SCROLLABLE, false);
        boolean customFragments = intent.getBooleanExtra(EXTRA_CUSTOM_FRAGMENTS, true);
        boolean permissions = intent.getBooleanExtra(EXTRA_PERMISSIONS, true);
        boolean showBack = intent.getBooleanExtra(EXTRA_SHOW_BACK, true);
        boolean showNext = intent.getBooleanExtra(EXTRA_SHOW_NEXT, true);
        boolean skipEnabled = intent.getBooleanExtra(EXTRA_SKIP_ENABLED, true);
        boolean finishEnabled = intent.getBooleanExtra(EXTRA_FINISH_ENABLED, true);
        boolean getStartedEnabled = intent.getBooleanExtra(EXTRA_GET_STARTED_ENABLED, true);
        setFullscreen(true);

        super.onCreate(savedInstanceState);

        // Add slides, edit configuration...
        skipEnabled=false;
        setButtonBackFunction(skipEnabled ? BUTTON_BACK_FUNCTION_SKIP : BUTTON_BACK_FUNCTION_BACK);
        setButtonNextFunction(
                finishEnabled ? BUTTON_NEXT_FUNCTION_NEXT_FINISH : BUTTON_NEXT_FUNCTION_NEXT);
        setButtonBackVisible(showBack);
        setButtonNextVisible(showNext);
        setButtonCtaVisible(getStartedEnabled);
        setButtonCtaTintMode(BUTTON_CTA_TINT_MODE_TEXT);

        // Slides
        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_title_1)
                .description(R.string.intro_description_1)
                .image(R.drawable.ic_launcher_web)
                .background(R.color.background_1)
                .backgroundDark(R.color.background_dark_1)
                .scrollable(scrollable)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_title_2)
                .description(R.string.intro_description_2)
                .image(R.drawable.art_material_motion)
                .background(R.color.background_1)
                .backgroundDark(R.color.background_dark_1)
                .scrollable(scrollable)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_title_3)
                .description(R.string.intro_description_3)
                .image(R.drawable.art_canteen_intro1)
                .background(R.color.background_1)
                .backgroundDark(R.color.background_dark_1)
                .scrollable(scrollable)
                .build());

        final Slide accessNotificationsSlide;
        accessNotificationsSlide = new SimpleSlide.Builder()
                    .title(R.string.intro_title_permissions)
                    .description(R.string.intro_description_permissions)
                    .image(R.drawable.notfication_access)
                    .background(R.color.background_1)
                    .backgroundDark(R.color.background_dark_1)
                    .scrollable(scrollable)
                    .buttonCtaLabel("Grant Access")
                    .buttonCtaClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                            } else {
                                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                            }
                        }
                    })
                    .build();
        addSlide(accessNotificationsSlide);

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_title_4)
                .description(R.string.intro_description_4)
                .image(R.drawable.ic_launcher_web)
                .background(R.color.background_1)
                .backgroundDark(R.color.background_dark_1)
                .scrollable(scrollable)
                .build());

        // Block navigation and show message if Permission is not granted
        setNavigationPolicy(new NavigationPolicy() {
            @Override
            public boolean canGoForward(int position) {
                Slide slide = getSlide(position);
                if (slide == accessNotificationsSlide) {
                    return hasNotificationAccess();
                } else {
                    return true;
                }
            }
            @Override
            public boolean canGoBackward(int position) {
                return true;
            }
        });

        // Show message if Permission is not granted
        addOnNavigationBlockedListener(new OnNavigationBlockedListener() {
            @Override
            public void onNavigationBlocked(int position, int direction) {
                View contentView = findViewById(android.R.id.content);
                if (contentView != null) {
                    Slide slide = getSlide(position);
                    if (slide == accessNotificationsSlide) {
                        Snackbar.make(contentView, R.string.intro_label_grant_permissions, Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
            }
        });

        //Set default replies if preference setting is empty
        List<Reply> repliesValues = new ArrayList<>();
        repliesValues = loadRepliesFromPrefs();

        if (repliesValues.isEmpty()) {
            String [] arrayReplies = getResources().getStringArray(R.array.default_notification_replies);
            for (String a : arrayReplies ) {
                Reply reply = new Reply();
                reply.setValue(a);
                repliesValues.add(reply);
            }
        }
        Gson gsonReplies = new Gson();
        String repliesJson = gsonReplies.toJson(repliesValues);
        Prefs.putString(Constants.PREF_NOTIFICATIONS_REPLIES, repliesJson);

        // Set default enabled packages if preference setting is empty
        String packagesJson = Prefs.getString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, "[]");
        Gson gsonPackages = new Gson();

        if (packagesJson.equals("[]")) {

            String [] arrayPackages = getResources().getStringArray(R.array.default_notification_packages);
            List<PackageInfo> packageInfoList = getPackageManager().getInstalledPackages(0);
            List<String> appInfoList = new ArrayList<>();

            for (PackageInfo packageInfo : packageInfoList) {
                for (String appName : arrayPackages) {
                    if (packageInfo.packageName.equals(appName)) {
                        appInfoList.add(appName);
                    }
                }
            }

            Collections.sort(appInfoList, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            });

            String pref = gsonPackages.toJson(appInfoList);

            Prefs.putString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, pref);

        }

    }

    private List<Reply> loadRepliesFromPrefs() {
        try {
            String repliesJson = Prefs.getString(Constants.PREF_NOTIFICATIONS_REPLIES, Constants.PREF_DEFAULT_NOTIFICATIONS_REPLIES);
            Type listType = new TypeToken<List<Reply>>() {
            }.getType();
            return new Gson().fromJson(repliesJson, listType);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private boolean hasNotificationAccess() {
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getApplicationContext().getPackageName();

        return !(enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName));
    }

}
