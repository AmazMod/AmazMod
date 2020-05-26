package com.edotassi.amazmod.ui;

import android.os.Bundle;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.databinding.ActivityFaqBinding;

import org.tinylog.Logger;

import amazmod.com.transport.Constants;

public class FaqActivity extends BaseAppCompatActivity {

    private ActivityFaqBinding binding;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaqBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            Logger.error("FaqActivity onCreate exception: " + exception.toString());
            //TODO log to crashlitics
        }
        getSupportActionBar().setTitle(R.string.faq);

        binding.activityFaqWebview.setWebViewClient(new WebViewClient());
        binding.activityFaqWebview.loadUrl(Constants.FAQ_URL);
    }
}
