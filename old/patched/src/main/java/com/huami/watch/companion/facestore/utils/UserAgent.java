package com.huami.watch.companion.facestore.utils;

import android.content.Context;
import com.huami.watch.companion.util.AppUtil;
import com.huami.watch.util.Log;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexReplace;

/**
 * Created by edoardotassinari on 16/03/18.
 */

/*
@DexEdit(defaultAction = DexAction.IGNORE)
public class UserAgent {

    @DexReplace
    private static String a(Context object) {
        String net = NetUtil.getNetType((Context) object);
        if (net.equals("wifi")) {
            return "WIFI";
        }
        if (net.equals("2g")) {
            return "2G";
        }
        if (net.equals("3g")) return "3G+";
        if (!net.equals("4g")) return "";
        return "3G+";
    }

    @DexReplace
    public static String getUserAgent(Context object, String previous) {
        StringBuilder charSequence = new StringBuilder((String) previous);
        String netType = UserAgent.a((Context) object);
        charSequence.append(" NetType/").append(netType);
        charSequence.append(" Language/").append("zh_CN");
        charSequence.append(" Country/").append("CN");
        charSequence.append(" UserRegion/").append("1");
        Log.d("userAgent", " final userAgentString : " + charSequence.toString(), new Object[0]);
        return charSequence.toString();
    }
}
*/