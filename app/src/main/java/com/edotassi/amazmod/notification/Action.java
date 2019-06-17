package com.edotassi.amazmod.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import java.util.ArrayList;

import amazmod.com.transport.Constants;

public class Action implements Parcelable {

    private final String text;
    private final String packageName;
    private final PendingIntent p;
    private final boolean isQuickReply;
    private final ArrayList<RemoteInputParcel> remoteInputs = new ArrayList<>();

    public Action(Parcel in) {
        text = in.readString();
        packageName = in.readString();
        p = in.readParcelable(PendingIntent.class.getClassLoader());
        isQuickReply = in.readByte() != 0;
        in.readTypedList(remoteInputs, RemoteInputParcel.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeString(packageName);
        dest.writeParcelable(p, flags);
        dest.writeByte((byte) (isQuickReply ? 1 : 0));
        dest.writeTypedList(remoteInputs);
    }

    public Action(String text, String packageName, PendingIntent p, RemoteInput remoteInput, boolean isQuickReply) {
        this.text = text;
        this.packageName = packageName;
        this.p = p;
        this.isQuickReply = isQuickReply;
        remoteInputs.add(new RemoteInputParcel(remoteInput));
    }

    public Action(NotificationCompat.Action action, String packageName, boolean isQuickReply) {
        this.text = action.title.toString();
        this.packageName = packageName;
        this.p = action.actionIntent;
        if(action.getRemoteInputs() != null) {
            int size = action.getRemoteInputs().length;
            for(int i = 0; i < size; i++)
                remoteInputs.add(new RemoteInputParcel(action.getRemoteInputs()[i]));
        }
        this.isQuickReply = isQuickReply;
    }

    public void sendReply(Context context, String msg) throws PendingIntent.CanceledException {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        ArrayList<RemoteInput> actualInputs = new ArrayList<>();

        for (RemoteInputParcel input : remoteInputs) {
            Log.i(Constants.TAG, "Action RemoteInput: " + input.getLabel());
            bundle.putCharSequence(input.getResultKey(), msg);
            RemoteInput.Builder builder = new RemoteInput.Builder(input.getResultKey());
            builder.setLabel(input.getLabel());
            builder.setChoices(input.getChoices());
            builder.setAllowFreeFormInput(input.isAllowFreeFormInput());
            builder.addExtras(input.getExtras());
            actualInputs.add(builder.build());
        }

        RemoteInput[] inputs = actualInputs.toArray(new RemoteInput[actualInputs.size()]);
        RemoteInput.addResultsToIntent(inputs, intent, bundle);
        p.send(context, 0, intent);
    }

    public ArrayList<RemoteInputParcel> getRemoteInputs() {
        return remoteInputs;
    }

    public boolean isQuickReply() {
        return isQuickReply;
    }

    public String getText() {
        return text;
    }

    public PendingIntent getQuickReplyIntent() {
        return isQuickReply ? p : null;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Action createFromParcel(Parcel in) {
            return new Action(in);
        }
        public Action[] newArray(int size) {
            return new Action[size];
        }
    };

}
