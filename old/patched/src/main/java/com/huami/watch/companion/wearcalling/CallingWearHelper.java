package com.huami.watch.companion.wearcalling;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.huami.watch.companion.util.AppUtil;
import com.huami.watch.transport.DataBundle;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexReplace;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 10/03/18.
 */

/*
@DexEdit(defaultAction = DexAction.IGNORE)
public class CallingWearHelper {

    @DexIgnore
    private Context b;
    @DexIgnore
    private String c;
    @DexIgnore
    private String d;

    @DexIgnore
    public CallingWearHelper(Context context) {
    }

    @DexWrap
    static String a(CallingWearHelper callingWearHelper, String string2) {
        Log.d("CallingMod", "called static a, string2: " + string2);

        return a(callingWearHelper, string2);
    }

    @DexWrap
    private String a(Context var1_1, String var2_5) {
        Log.d("CallingMod", "called var1_1: " + var1_1 + " - var2_5: " + var2_5);
        return a(var1_1, var2_5);
    }

    @DexReplace
    public void handleCallingToWear(String object, boolean bl2) {
        this.c = object;
        if (TextUtils.isEmpty((CharSequence) object)) {
            android.util.Log.i((String) "CallingWearHelper", (String) "null contact name . ");
            return;
        }
        DataBundle dataBundle = new DataBundle();
        dataBundle.putString("KEY_CALL_ELEMENT", this.c);

        if (bl2) {
            dataBundle.putString("KEY_CALL_TELNUMBER", "");
        } else {
            dataBundle.putString("KEY_CALL_TELNUMBER", this.d);
        }

        dataBundle.putInt("KEY_APP_VERSION_CODE", AppUtil.getVersionCode(this.b));
        dataBundle.putBoolean("KEY_NF_TEST", bl2);
        int n2 = bl2 ? 10000 : 5000;
        dataBundle.putInt("KEY_HB_INTERNAL", n2);

        Log.d("CallingMod", "KEY_CALL_ELEMENT: " + dataBundle.getString("KEY_CALL_ELEMENT"));
        Log.d("CallingMod", "KEY_CALL_TELNUMBER: " + dataBundle.getString("KEY_CALL_TELNUMBER"));
        Log.d("CallingMod", "KEY_APP_VERSION_CODE: " + dataBundle.getInt("KEY_APP_VERSION_CODE"));
        Log.d("CallingMod", "KEY_NF_TEST: " + dataBundle.getBoolean("KEY_NF_TEST"));
        Log.d("CallingMod", "KEY_HB_INTERNAL: " + dataBundle.getInt("KEY_HB_INTERNAL"));

        this.a("transport_module_calling_to_wear.ACTION_INCOMMING_CALL", dataBundle);
    }

    @DexIgnore
    private void a(String object, DataBundle dataBundle) {
    }
}
*/
