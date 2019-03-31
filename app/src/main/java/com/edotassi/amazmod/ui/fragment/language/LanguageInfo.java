package com.edotassi.amazmod.ui.fragment.language;

import android.support.annotation.NonNull;

public class LanguageInfo {

    private String label;
    private String code;

    public LanguageInfo(@NonNull String label,
                        @NonNull String code) {

        this.label = label;
        this.code = code;
    }


    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
