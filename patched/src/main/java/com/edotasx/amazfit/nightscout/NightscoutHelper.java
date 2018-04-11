package com.edotasx.amazfit.nightscout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.util.Log;
import android.webkit.URLUtil;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.edotasx.amazfit.transport.TransportService;
import com.github.kittinunf.fuel.Fuel;
import com.github.kittinunf.fuel.core.FuelError;
import com.github.kittinunf.fuel.core.Request;
import com.github.kittinunf.fuel.core.Response;
import com.github.kittinunf.result.Result;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import kotlin.Triple;
import okhttp3.HttpUrl;

/**
 * Created by edoardotassinari on 08/04/18.
 */

public class NightscoutHelper {

    private static NightscoutHelper instance;

    private Context context;

    private NightscoutHelper(Context context) {
        this.context = context;
    }

    public static NightscoutHelper sharedInstance(Context context) {
        if (instance == null) {
            instance = new NightscoutHelper(context);
        }

        return instance;
    }

    public void sync() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = PreferenceManager.getString(context, Constants.PREFERENCE_NIGHTSCOUT_URL, null);
                    if (url == null) {
                        return;
                    }

                    if (!URLUtil.isValidUrl(url)) {
                        Log.w(Constants.TAG, "invalid nighscout url: " + url);
                        return;
                    }

                    Triple<Request, Response, Result<String, FuelError>> fuel = Fuel.get(url).responseString();
                    Request request = fuel.getFirst();
                    Response response = fuel.getSecond();
                    Result<String, FuelError> text = fuel.getThird();

                    Gson gson = new Gson();
                    Type listType = new TypeToken<ArrayList<NightscoutData>>() {
                    }.getType();
                    List<NightscoutData> data = gson.fromJson(text.get(), listType);

                    if (data == null) {
                        Log.w(Constants.TAG, "nightscout: null data received");
                        return;
                    }

                    Log.d(Constants.TAG, "nightscout: found " + data.size() + " records");

                    if (data.size() == 0) {
                        Log.w(Constants.TAG, "nightscout: empty list received");
                        return;
                    }

                    NightscoutData lastRecord = data.get(0);

                    TransportService
                            .sharedInstance(context)
                            .send(Constants.NIGHTSCOUT_SYNC_ACTION, NightscoutData.toDataBundle(lastRecord));

                } catch (Exception networkError) {
                    Log.e(Constants.TAG, "nightscout: error " + networkError.getMessage());
                }
            }
        });

        thread.start();

    }
}
