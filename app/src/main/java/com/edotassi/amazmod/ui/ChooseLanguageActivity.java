package com.edotassi.amazmod.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.ui.fragment.language.LanguageInfo;
import com.edotassi.amazmod.util.LocaleUtils;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChooseLanguageActivity extends BaseAppCompatActivity {

    @BindView(R.id.picker)
    RadioGroup radioGroup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_language);
        ButterKnife.bind(this);

        ActionBar supportActionBar = Objects.requireNonNull(getSupportActionBar());
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setTitle(R.string.language);

        String currentLanguage = LocaleUtils.getLanguage();
        String[] languageCodes = getResources().getStringArray(R.array.languages_array_codes);
        List<LanguageInfo> languageInfos = new ArrayList<>();
        for (String code : languageCodes) {
            String label = LocaleUtils.getDisplayLanguage(code);
            LanguageInfo languageInfo = new LanguageInfo(label, code);
            languageInfos.add(languageInfo);
        }
        Collections.sort(languageInfos,
                (o1, o2) -> o1.getLabel().compareToIgnoreCase(o2.getLabel()));

        int padding = getResources().getDimensionPixelSize(R.dimen.language_spacing);
        for (int i = 0; i < languageInfos.size(); i++) {
            RadioButton radioButtonView = new RadioButton(this);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            radioButtonView.setLayoutParams(params);
            LanguageInfo model = languageInfos.get(i);
            radioButtonView.setText(model.getLabel());
            radioButtonView.setId(i);
            radioButtonView.setTag(model.getCode());
            radioButtonView.setGravity(Gravity.CENTER_VERTICAL);
            radioButtonView.setPadding(padding, padding, padding, padding);
            radioGroup.addView(radioButtonView);
            if (model.getCode().equalsIgnoreCase(currentLanguage)) radioGroup.check(i);
        }
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String tag = (String) radioGroup.getChildAt(checkedId).getTag();
            Logger.e(tag);
            LocaleUtils.setLocale(this, tag);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_choose_language, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_activity_choose_language_select:
                finish();
                break;
        }
        return true;
    }
}