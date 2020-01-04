package com.amazmod.service.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.core.os.ConfigurationCompat;

import com.pixplicity.easyprefs.library.Prefs;

import java.util.Locale;

import com.amazmod.service.Constants;

public class LocaleUtils {


    public static Context onAttach(Context context) {
        String language = getLanguage();
        return setLocale(context, language);
    }

    public static Locale getLocale() {
        String currentLanguage = getPersistedData(Locale.getDefault().getLanguage());
        System.out.println("D/AmazMod LocaleUtils getLocale currentLanguage: " + currentLanguage);
        return getLocaleByLanguageCode(currentLanguage);
    }

    public static Context onAttach(Context context, String defaultLanguage) {
        String lang = getPersistedData(defaultLanguage);
        return setLocale(context, lang);
    }

    private static Locale getLocaleByLanguageCode(@NonNull String languageCode) {
        String[] languageCodes = languageCode.split("-r");
        if (languageCodes.length > 1) {
            return new Locale(languageCodes[0], languageCodes[1]);
        } else {
            return new Locale(languageCode);
        }

    }

    // Save new language
    public static void persist(String language) {
        Prefs.putString(Constants.PREF_DEFAULT_LOCALE, language);
    }

    // Get saved language
    private static String getPersistedData(String defaultLanguage) {
        return Prefs.getString(Constants.PREF_DEFAULT_LOCALE, defaultLanguage);
    }
    public static String getLanguage() {
        return getPersistedData(ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0).getLanguage());
    }

    // Change language
    private static Context setLocale(Context context, String language) {
        System.out.println("D/AmazMod LocaleUtils Change language - System: " + Locale.getDefault().getLanguage() + ", " +
                "To: " + language + ", Device: " + ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0).getLanguage());

        Locale locale = getLocaleByLanguageCode(language);
        Locale.setDefault(locale);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = locale;
        conf.setLayoutDirection(locale);
        res.updateConfiguration(conf, dm);
        return context;
    }

    // THIS ONE PROBABLY IS NOT NEED
    public static String getDisplayLanguage(@NonNull String languageCode) {
        Locale locale = getLocaleByLanguageCode(languageCode);
        String displayCountry = locale.getDisplayCountry(locale);
        if (displayCountry != null && !displayCountry.isEmpty()) {
            return String.format("%s (%s)", locale.getDisplayLanguage(Locale.UK), displayCountry);
        } else {
            return locale.getDisplayLanguage(Locale.UK);
        }
    }
}
