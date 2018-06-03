package com.edotassi.amazmod.transport.payload;

import android.graphics.Bitmap;
import android.os.Parcel;

import com.edotassi.amazmod.transport.Transportable;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.SafeParcelable;

public class NotificationData implements Transportable {

    private final String DATA_KEY = "key";
    private final String DATA_ID = "id";
    private final String DATA_TITLE = "title";
    private final String DATA_TEXT = "text";
    private final String DATA_ICON = "icon";
    private final String DATA_ICON_WIDTH = "iconWidth";
    private final String DATA_ICON_HEIGHT = "iconHeight";

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

    public NotificationData() {
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

    @Override
    public void toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(DATA_KEY, key);
        dataBundle.putInt(DATA_ID, id);
        dataBundle.putString(DATA_TITLE, title);
        dataBundle.putString(DATA_TEXT, text);
        dataBundle.putIntArray(DATA_ICON, icon);
        dataBundle.putInt(DATA_ICON_WIDTH, iconWidth);
        dataBundle.putInt(DATA_ICON_HEIGHT, iconHeight);
    }
}
