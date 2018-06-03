package com.edotassi.amazmodcompanionservice.notifications;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.edotassi.amazmodcompanionservice.transport.Transportable;
import com.huami.watch.transport.DataBundle;

public class NotificationSpec implements Transportable, Parcelable {

    public static final String EXTRA = "notificationSpec";

    private String key;
    private int id;
    private String title;
    private String text;
    private int[] icon;
    private int iconWidth;
    private int iconHeight;
    private int vibration;
    private boolean isDeviceLocked;
    private int timeoutRelock;

    public NotificationSpec() {
    }

    protected NotificationSpec(Parcel in) {
        key = in.readString();
        id = in.readInt();
        title = in.readString();
        text = in.readString();
        icon = in.createIntArray();
        iconWidth = in.readInt();
        iconHeight = in.readInt();
        vibration = in.readInt();
        isDeviceLocked = in.readByte() != 0;
        timeoutRelock = in.readInt();
    }

    public static final Creator<NotificationSpec> CREATOR = new Creator<NotificationSpec>() {
        @Override
        public NotificationSpec createFromParcel(Parcel in) {
            return new NotificationSpec(in);
        }

        @Override
        public NotificationSpec[] newArray(int size) {
            return new NotificationSpec[size];
        }
    };

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int[] getIcon() {
        return icon;
    }

    public void setIcon(int[] icon) {
        this.icon = icon;
    }

    public boolean isDeviceLocked() {
        return isDeviceLocked;
    }

    public void setDeviceLocked(boolean deviceLocked) {
        isDeviceLocked = deviceLocked;
    }

    public int getVibration() {
        return vibration;
    }

    public void setVibration(int vibration) {
        this.vibration = vibration;
    }

    public int getTimeoutRelock() {
        return timeoutRelock;
    }

    public void setTimeoutRelock(int timeoutRelock) {
        this.timeoutRelock = timeoutRelock;
    }

    public int getIconWidth() {
        return iconWidth;
    }

    public void setIconWidth(int iconWidth) {
        this.iconWidth = iconWidth;
    }

    public int getIconHeight() {
        return iconHeight;
    }

    public void setIconHeight(int iconHeight) {
        this.iconHeight = iconHeight;
    }

    public void toDataBundle(DataBundle dataBundle) {

    }

    public static NotificationSpec fromDataBundle(DataBundle dataBundle) {
        NotificationSpec notificationSpec = new NotificationSpec();

        String title = dataBundle.getString("title");
        String text = dataBundle.getString("text");
        int id = dataBundle.getInt("id");
        String key = dataBundle.getString("key");
        int[] icon = dataBundle.getIntArray("icon");
        int iconWidth = dataBundle.getInt("iconWidth");
        int iconHeight = dataBundle.getInt("iconHeight");

        notificationSpec.setTitle(title);
        notificationSpec.setText(text);
        notificationSpec.setIcon(icon);
        notificationSpec.setIconWidth(iconWidth);
        notificationSpec.setIconHeight(iconHeight);
        notificationSpec.setId(id);
        notificationSpec.setKey(key);

        return notificationSpec;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(text);
        dest.writeIntArray(icon);
        dest.writeInt(iconWidth);
        dest.writeInt(iconHeight);
        dest.writeInt(vibration);
        dest.writeByte((byte) (isDeviceLocked ? 1 : 0));
        dest.writeInt(timeoutRelock);
    }
}
