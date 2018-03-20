package com.huami.watch.notification.data;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;

/**
 * Created by edoardotassinari on 19/03/18.
 */

@SuppressLint("ParcelCreator")
@DexEdit(defaultAction = DexAction.IGNORE)
public class RemoteInputData implements Parcelable {

    @DexIgnore
    public String[] choices;
    @DexIgnore
    public String label;
    @DexIgnore
    public String resultKey;

    @DexEdit
    public RemoteInputData() {}

    @DexIgnore
    @Override
    public int describeContents() {
        return 0;
    }

    @DexIgnore
    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }
}
