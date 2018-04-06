package com.huami.watch.companion.ui.card;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.edotasx.amazfit.R;

import lanchon.dexpatcher.annotation.DexAdd;

/**
 * Created by edoardotassinari on 04/02/18.
 */

@DexAdd
public class ChangelogCard extends BaseCard {

    public static BaseCard create(Activity activity) {
        return new ChangelogCard(activity);
    }

    private ChangelogCard(Activity activity) {
        super(activity);
    }

    @Override
    protected void clickView() {
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.card_changelog;
    }

    @Override
    protected void initView() {
        final View oldChangelog = getView().findViewById(R.id.changelog_layout_read_all);

        TextView textViewReadAll = getView().findViewById(R.id.changelog_button_read_all);

        textViewReadAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int visibility = oldChangelog.getVisibility();
                if (visibility == View.GONE) {
                    oldChangelog.setVisibility(View.VISIBLE);
                } else {
                    oldChangelog.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public String tag() {
        return "changelog";
    }
}
