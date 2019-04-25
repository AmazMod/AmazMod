package com.edotassi.amazmod.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.os.ConfigurationCompat;
import android.util.Log;

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

    public static Locale getLocale() {
        String currentLanguage = getPersistedData(Locale.getDefault().getLanguage());
        //TODO: commented line below because it was making TinyLog config not to work (is any log is done before configuration, nothing works)
        //Logger.debug("LocaleUtils getLocale currentLanguage: " + currentLanguage);
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

    // Save new language
    public static void persist(String language) {
        Prefs.putString(Constants.PREF_LANGUAGE, language);
    }

    // Get saved language
    private static String getPersistedData(String defaultLanguage) {
        return Prefs.getString(Constants.PREF_LANGUAGE, defaultLanguage);
    }
    public static String getLanguage() {
        return getPersistedData(Locale.getDefault().getLanguage());
    }

    // Change language
    private static Context setLocale(Context context, String language) {
        Log.d("Amazmod","Change language - System: "+Locale.getDefault().getLanguage()+", To: "+language+", Device: "+ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).toLanguageTags());

        Locale locale;
        // If AUTO get the system Locale
        if (language.equals(Constants.PREF_LANGUAGE_AUTO)) {
            //language = Locale.getDefault().getLanguage();
            locale = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0);
        }else{
            locale = getLocaleByLanguageCode(language);
        }

        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            configuration.setLayoutDirection(locale);
            return context.createConfigurationContext(configuration);
        }else{
            configuration.locale = locale;
            // Min APP SDK is above
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                configuration.setLayoutDirection(locale);
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
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
