package com.edotassi.amazmod.notification;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.app.RemoteInput;

/**
 * Created by JJ on 05/08/15.
 */
public class RemoteInputParcel implements Parcelable {

    private String label;
    private String resultKey;
    private String[] choices = new String[0];
    private boolean allowFreeFormInput;
    private Bundle extras;


    public RemoteInputParcel(RemoteInput input) {
        label = input.getLabel().toString();
        resultKey = input.getResultKey();
        charSequenceToStringArray(input.getChoices());
        allowFreeFormInput = input.getAllowFreeFormInput();
        extras = input.getExtras();
    }

    public RemoteInputParcel(Parcel in) {
        label = in.readString();
        resultKey = in.readString();
        choices = in.createStringArray();
        allowFreeFormInput = in.readByte() != 0;
        extras = in.readParcelable(Bundle.class.getClassLoader());
    }

    public void charSequenceToStringArray(CharSequence[] charSequence) {
        if(charSequence != null) {
            int size = charSequence.length;
            choices = new String[charSequence.length];
            for (int i = 0; i < size; i++)
                choices[i] = charSequence[i].toString();
        }
    }

    public String getResultKey() {
        return resultKey;
    }

    public String getLabel() {
        return label;
    }

    public CharSequence[] getChoices() {
        return choices;
    }

    public boolean isAllowFreeFormInput() {
        return allowFreeFormInput;
    }

    public Bundle getExtras() {
        return extras;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label);
        dest.writeString(resultKey);
        dest.writeStringArray(choices);
        dest.writeByte((byte) (allowFreeFormInput ? 1 : 0));
        dest.writeParcelable(extras, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public RemoteInputParcel createFromParcel(Parcel in) {
            return new RemoteInputParcel(in);
        }
        public RemoteInputParcel[] newArray(int size) {
            return new RemoteInputParcel[size];
        }
    };

}