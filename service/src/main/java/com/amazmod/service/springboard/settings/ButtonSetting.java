package com.amazmod.service.springboard.settings;

import android.view.View;

public class ButtonSetting extends BaseSetting {

    View.OnClickListener onClickListener;
    String text;

    public ButtonSetting(String text, View.OnClickListener onClickListener) {
        this.text = text;
        this.onClickListener = onClickListener;
    }
}
