package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class NotificationReplyData extends Transportable implements Parcelable {

    public static final String EXTRA = "notificationReply";

    public static final String NOTIFICATION_ID = "notificationId";
    public static final String REPLY = "reply";

    private String notificationId;
    private String reply;

    public NotificationReplyData() {
    }

    protected NotificationReplyData(Parcel in) {
        notificationId = in.readString();
        reply = in.readString();
    }

    public static final Creator<NotificationReplyData> CREATOR = new Creator<NotificationReplyData>() {
        @Override
        public NotificationReplyData createFromParcel(Parcel in) {
            return new NotificationReplyData(in);
        }

        @Override
        public NotificationReplyData[] newArray(int size) {
            return new NotificationReplyData[size];
        }
    };

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public static NotificationReplyData fromDataBundle(DataBundle dataBundle) {
        NotificationReplyData notificationReplyData = new NotificationReplyData();

        notificationReplyData.setNotificationId(dataBundle.getString("key"));
        notificationReplyData.setReply(dataBundle.getString("message"));

        return notificationReplyData;
    }

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {

        dataBundle.putString(NOTIFICATION_ID, notificationId);
        dataBundle.putString(REPLY, reply);

        return dataBundle;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(notificationId);
        dest.writeString(reply);
    }
}
