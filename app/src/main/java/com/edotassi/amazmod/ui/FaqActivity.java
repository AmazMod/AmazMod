package com.edotassi.amazmod.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.edotassi.amazmod.R;

import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;

public class FaqActivity extends BaseAppCompatActivity {

    @BindView(R.id.activity_faq_webview)
    WebView webView;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            System.out.println("AmazMod FaqActivity onCreate exception: " + exception.toString());
            //TODO log to crashlitics
        }
        getSupportActionBar().setTitle(R.string.faq);

        ButterKnife.bind(this);

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(Constants.FAQ_URL);
    }
}
