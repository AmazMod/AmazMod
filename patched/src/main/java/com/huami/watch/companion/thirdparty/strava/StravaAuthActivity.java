package com.huami.watch.companion.thirdparty.strava;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.huami.watch.companion.settings.WebActivity;
import com.huami.watch.util.Log;

/**
 * Created by edoardotassinari on 30/01/18.
 */

public class StravaAuthActivity extends WebActivity {

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        WebSettings webSettings = this.mWebView.getSettings();
        String string = webSettings.getUserAgentString();
        Log.d((String) "Strava-AuthActivity", (String) ("UserAgent before : " + string), (Object[]) new Object[0]);
        if (string.contains(" wv")) {
            webSettings.setUserAgentString(string.replace(" wv", ""));
        } else {
            webSettings.setUserAgentString(string.replace("Version/", "xxx/"));
        }
        Log.d((String) "Strava-AuthActivity", (String) ("UserAgent after : " + string), (Object[]) new Object[0]);
        this.mWebView.setWebViewClient((WebViewClient) new a(this));
    }

    static class a extends WebActivity.AppWebClient {
        public a(WebActivity webActivity) {
            super(webActivity);
        }

        public boolean shouldOverrideUrlLoading(WebView webView, String string) {
            Log.d((String) "Strava-AuthActivity", (String) ("Load Url : " + string), (Object[]) new Object[0]);
            if (string.startsWith("http://localhost/verify")) {
                String string2 = Uri.parse((String) string).getQueryParameter("code");
                Log.d((String) "Strava-AuthActivity", (String) ("Code : " + string2), (Object[]) new Object[0]);
                WebActivity webActivity = (WebActivity) this.mWebActivity.get();
                if (webActivity != null) {
                    Intent intent = new Intent();
                    intent.putExtra("code", string2);
                    webActivity.setResult(-1, intent);
                    webActivity.finish();
                    return true;
                }
            }
            return super.shouldOverrideUrlLoading(webView, string);
        }
    }

}
