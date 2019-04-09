package com.edotassi.amazmod.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;

import com.pixplicity.easyprefs.library.Prefs;

import org.tinylog.Logger;

import java.util.Locale;

import amazmod.com.transport.Constants;

public class LocaleUtils {

    public static Context onAttach(Context context) {
        String language = getPersistedData(Locale.getDefault().getLanguage());
        return setLocale(context, language);
    }

    public static Context onAttach(Context context, String defaultLanguage) {
        String lang = getPersistedData(defaultLanguage);
        return setLocale(context, lang);
    }

    public static String getLanguage() {
        return getPersistedData(Locale.getDefault().getLanguage());
    }

    public static Locale getLocale() {
        String currentLanguage = getPersistedData(Locale.getDefault().getLanguage());
        Logger.debug("LocaleUtils getLocale currentLanguage: " + currentLanguage);
        if (currentLanguage.equals(Constants.PREF_LANGUAGE_AUTO)){
            currentLanguage = Locale.getDefault().getLanguage();
        }
        return getLocaleByLanguageCode(currentLanguage);
    }

    private static Locale getLocaleByLanguageCode(@NonNull String languageCode) {
        String[] languageCodes = languageCode.split("-r");
        if (languageCodes.length > 1) {
            return new Locale(languageCodes[0], languageCodes[1]);
        } else {
            return new Locale(languageCode);
        }

    }

    public static Context setLocale(Context context, String language) {
        persist(language);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, getLocale());
        }

        return updateResourcesLegacy(context, getLocale());
    }

    private static String getPersistedData(String defaultLanguage) {
        return Prefs.getString(Constants.PREF_LANGUAGE, defaultLanguage);
    }

    private static void persist(String language) {
        Prefs.putString(Constants.PREF_LANGUAGE, language);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context updateResources(Context context, Locale locale) {
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, Locale locale) {
        Locale.setDefault(locale);

        Resources resources = context.getResources();

        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale);
        }

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        return context;
    }

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
