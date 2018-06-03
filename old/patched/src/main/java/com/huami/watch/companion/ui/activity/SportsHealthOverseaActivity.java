package com.huami.watch.companion.ui.activity;

/*
 * Decompiled with CFR 0_123.
 */

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;
import com.huami.watch.companion.cloud.bean.CloudStravaAuthData;
import com.huami.watch.companion.device.DeviceManager;
import com.huami.watch.companion.event.ConnectedEvent;
import com.huami.watch.companion.event.DisconnectedEvent;
import com.huami.watch.companion.settings.WebActivity;
import com.huami.watch.companion.sync.SyncCenter;
import com.huami.watch.companion.thirdparty.strava.StravaAuthActivity;
import com.huami.watch.companion.thirdparty.strava.StravaAuthHelper;
import com.huami.watch.companion.ui.view.ActionbarLayout;
import com.huami.watch.companion.ui.view.LoadingDialog;
import com.huami.watch.companion.util.AppUtil;
import com.huami.watch.companion.util.Box;
import com.huami.watch.companion.util.DeviceCompatibility;
import com.huami.watch.companion.util.HelpUtil;
import com.huami.watch.companion.util.NetworkUtil;
import com.huami.watch.companion.util.RxBus;
import com.huami.watch.companion.util.TimeUtil;


import lanchon.dexpatcher.annotation.DexAdd;

/*
@DexAdd
public class SportsHealthOverseaActivity
        extends Activity
        implements SyncCenter.SyncListener {

    public static final String PKG_OF_HEALTH = "com.huami.watch.health";
    public static final String PKG_OF_SPORT = "com.huami.watch.sport";
    private TextView statusTextView;
    private Button syncButton;
    private DialogFragment f;
    private boolean logged;
    private Disposable h;

    private void initView() {
        ActionbarLayout actionbarLayout = (ActionbarLayout) findViewById(R.id.actionbar);
        actionbarLayout.setTitleText(getString(R.string.strava_sync));
        actionbarLayout.setBackArrowClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        this.statusTextView = (TextView) findViewById(R.id.sync_data_status);
        this.syncButton = (Button) findViewById(R.id.sync_data);
        if (DeviceManager.getManager(this).isBoundDeviceConnected()) {
            return;
        }
    }

    private void a(Activity activity) {
        if (!NetworkUtil.offlineChecking(activity)) {
            return;
        }
        HelpUtil.toHelpPage(this, 4);
    }

    private void a(Context context) {
        StravaAuthHelper.getHelper(context).checkAuthStatus(new Consumer<CloudStravaAuthData>() {
            @Override
            public void accept(CloudStravaAuthData cloudStravaAuthData) {
                if (cloudStravaAuthData != null) {
                    SportsHealthOverseaActivity.this.b(cloudStravaAuthData.isAuthValid());
                }
            }
        });
    }

    private void a(Context context, Boolean bl) {
        this.f = LoadingDialog.dismiss(this.f);
        if (bl.booleanValue()) {
            Toast.makeText(context, getString(R.string.strava_authenticated), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, getString(R.string.strava_authentication_failed), Toast.LENGTH_LONG).show();
        }
        this.a(context);
    }

    private void a(boolean bl) {
        long l = Box.getLastSportDataSyncTime();
        if (!bl) {
            this.syncButton.setText(getString(R.string.strava_updated));
            return;
        }
        if (l > 0) {
            this.syncButton.setText(getString(R.string.strava_last_update, TimeUtil.formatDateTime(l, "HH:mm dd/MM/yyyy")));
            return;
        }
        this.syncButton.setText(getString(R.string.strava_not_sync));
    }

    private void authenticateStravaViaWeb(Context context) {
        Intent intent = new Intent(context, StravaAuthActivity.class);
        intent.putExtra("webtitle", "Strava");
        intent.putExtra("url", "https://www.strava.com/oauth/authorize?client_id=14851&response_type=code&redirect_uri=http://localhost/verify&approval_prompt=force&scope=view_private,write");
        this.startActivityForResult(intent, 1);
    }

    private void b(boolean bl) {
        if (bl) {
            String string = StravaAuthHelper.getStravaUserInfoNickname();
            if (!TextUtils.isEmpty(string)) {
                this.statusTextView.setText(string);
                this.logged = true;
            }
            this.a(true);
            return;
        }
        this.statusTextView.setText("");
        this.syncButton.setText("not found");
        this.logged = false;
    }

    public void onActivityResult(int n, int n2, Intent intent) {
        if (n == 1 && n2 == -1) {
            this.f = LoadingDialog.setMessage(getString(R.string.strava_authorizing)).show(this, "StravaAuth");
            StravaAuthHelper.getHelper(this).onAuthResult(intent, new Consumer<Boolean>() {
                @Override
                public void accept(Boolean bl) {
                    SportsHealthOverseaActivity.this.a(SportsHealthOverseaActivity.this, bl);
                }
            });
        }
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.setContentView(R.layout.activity_sports);
        DeviceCompatibility.MIUI.setStatusBarDarkMode(this, true);
        this.initView();
        this.h = RxBus.get().toObservable().subscribe(new Consumer<Object>() {

            public void accept(Object object) {
                if (object instanceof ConnectedEvent) {
                    return;
                } else {
                    if (!(object instanceof DisconnectedEvent)) return;
                    {
                        return;
                    }
                }
            }
        });
        this.a(true);
        this.b(true);
        this.a((Context) this);
    }

    protected void onDestroy() {
        if (this.h != null) {
            this.h.dispose();
            this.h = null;
        }
        super.onDestroy();
    }

    public void onStravaAuthStartClick(View view) {
        if (!this.logged) {
            authenticateStravaViaWeb(this);
            return;
        }

        if (AppUtil.isAppExists(this, "com.strava")) {
            AppUtil.toApp(this, "com.strava");
        } else {
            Intent intent = new Intent(this, WebActivity.class);
            intent.putExtra("webtitle", "Strava");
            intent.putExtra("url", "https://www.strava.com");
            startActivity(intent);
        }
    }

    public void onSyncDataClick(View view) {
        if (!NetworkUtil.offlineChecking((Activity) this)) {
            return;
        }
        this.syncButton.setText(getString(R.string.strava_syncing));
        SyncCenter.getCenter(this).types(1).listen(this).startAsync();
    }

    public void onSyncStateChanged(int n, int n2, String string) {
        String string2;
        if (Integer.MAX_VALUE == n) {
            if (n2 == -1 || (SyncCenter.getCenter(this).getLastSyncFailTypes() & 1) <= 0)
                return;
            {
                this.a(false);
                return;
            }
        }
        if (n2 == 0) return;
        String string3 = string2 = null;
        block0:
        switch (n2) {
            default: {
                string3 = string2;
                break;
            }
            case 1: {
                string3 = getString(R.string.strava_syncing);
                break;
            }
            case -1: {
                switch (n) {
                    default: {
                        string3 = string2;
                        break block0;
                    }
                    case 1:
                }
                this.a(true);
                string3 = string2;
                break;
            }
            case -2: {
                switch (n) {
                    default: {
                        string3 = string2;
                        break block0;
                    }
                    case 1:
                }
                this.a(false);
                string3 = string2;
            }
            case 0:
        }
        if (TextUtils.isEmpty(string)) {
            string = string3;
        }
        if (string == null) {
            return;
        }
        switch (n) {
            default: {
                return;
            }
            case 1:
        }
        this.syncButton.setText(string);
    }

}
*/