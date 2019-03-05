package com.edotassi.amazmod.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.ui.fragment.language.LanguageInfo;
import com.orhanobut.logger.Logger;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ChooseLanguageActivity extends AppCompatActivity {

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

        String currentLanguage = Prefs.getString(Constants.PREF_LANGUAGE,
                Locale.getDefault().toLanguageTag());
        String[] languageCodes = getResources().getStringArray(R.array.languages_array_codes);
        List<LanguageInfo> languageModels = new ArrayList<>();
        String[] languageLabels = getResources().getStringArray(R.array.languages_array);
        for (int i = 0; i < languageLabels.length; i++) {
            String code = languageCodes[i];
            String label = languageLabels[i];
            LanguageInfo languageInfo = new LanguageInfo(label, code);
            languageModels.add(languageInfo);
            // TODO: add sort by label
        }

        int padding = getResources().getDimensionPixelSize(R.dimen.language_spacing);
        for (int i = 0; i < languageModels.size(); i++) {
            RadioButton radioButtonView = new RadioButton(this);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            radioButtonView.setLayoutParams(params);
            LanguageInfo model = languageModels.get(i);
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
            if (!tag.equalsIgnoreCase(currentLanguage)) {
                Prefs.putString(Constants.PREF_LANGUAGE, tag);
            }
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