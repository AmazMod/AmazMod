package com.amazmod.service.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WearableFrameLayout;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.amazmod.service.R;


public class FileViewerWebViewActivity extends Activity {

    BoxInsetLayout boxInsetLayout;
    WearableFrameLayout frameLayoutImage, frameLayoutText;
    WebView webviewImage, webviewText;

    public static final String FILE_URI = "fileUri";
    public static final String MIME_TYPE = "mimeType";
    public static final String IMAGE = "image";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_viewer_webview);

        String fileUri = getIntent().getStringExtra(FILE_URI);
        String mimeType = getIntent().getStringExtra(MIME_TYPE);

        boxInsetLayout = findViewById(R.id.activity_file_viewer_main_layout);
        frameLayoutImage = findViewById(R.id.activity_file_viewer_frame_layout_image);
        webviewImage = findViewById(R.id.activity_file_viewer_webview_image);
        frameLayoutText = findViewById(R.id.activity_file_viewer_frame_layout_text);
        webviewText = findViewById(R.id.activity_file_viewer_webview_text);

        if (mimeType.contains(IMAGE)) {
            frameLayoutText.setVisibility(View.GONE);
            loadWebView(fileUri, webviewImage, true);

        } else {
            frameLayoutImage.setVisibility(View.GONE);
            loadWebView(fileUri, webviewText, false);
        }

    }

    private void loadWebView(String fileUri, WebView webView, boolean isImage) {

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(false);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);

        if (isImage) {
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
        }

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(fileUri);

    }

    private class WebViewClient extends android.webkit.WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

}
