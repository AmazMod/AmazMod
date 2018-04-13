package com.huami.watch.companion.facestore;

import com.edotasx.amazfit.Constants;
import com.huami.midong.webview.jsbridge.JsBridgeWebView;
import com.huami.midong.webview.jsbridge.JsCallBackFunction;
import com.huami.watch.util.Log;

/**
 * Created by edoardotassinari on 12/04/18.
 */

public class CustomJsCallback implements JsCallBackFunction {

    private JsBridgeWebView jsBridgeWebView;

    public CustomJsCallback(JsBridgeWebView jsBridgeWebView) {
        this.jsBridgeWebView = jsBridgeWebView;
    }

    @Override
    public void onCallBack(String string2) {
        Log.d(Constants.TAG, "CustomJsCallback");
    }
}
