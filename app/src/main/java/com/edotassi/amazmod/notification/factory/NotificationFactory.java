package com.edotassi.amazmod.notification.factory;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.pixplicity.easyprefs.library.Prefs;

import org.tinylog.Logger;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.NotificationData;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class NotificationFactory {

    private static boolean isNormalNotification = true;

    public static NotificationData fromStatusBarNotification(Context context, StatusBarNotification statusBarNotification) {

        NotificationData notificationData = new NotificationData();
        Notification notification = statusBarNotification.getNotification();
        Bundle bundle = notification.extras;
        String text = "", title = "";

        Logger.trace("notification key: {}", statusBarNotification.getKey());

        //Notification time
        //Calendar c = Calendar.getInstance();
        //c.setTimeInMillis(notification.when);
        //String notificationTime = c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE);
        String notificationTime = DateFormat.getTimeInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(notification.when);

        //EXTRA_TITLE and EXTRA_TEXT are usually CharSequence and not regular Strings...
        CharSequence bigTitle = bundle.getCharSequence(Notification.EXTRA_TITLE);
        if (bigTitle != null) {
            title = bigTitle.toString();
        } else try {
            title = bundle.getString(Notification.EXTRA_TITLE);
        } catch (ClassCastException e) {
            Logger.debug(e,"NotificationFactory exception: " + e.toString() + " title: " + title);
        }

        CharSequence bigText = bundle.getCharSequence(Notification.EXTRA_TEXT);
        if (bigText != null) {
            text = bigText.toString();
        }

        //Use EXTRA_TEXT_LINES instead, if it exists
        CharSequence[] lines = bundle.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if ((lines != null) && (lines.length > 0)) {
            text += "\n*Extra lines:\n" + lines[Math.min(lines.length - 1, 0)].toString();
            Logger.debug("NotificationFactory EXTRA_TEXT_LINES exists");
        }

        //Maybe use android.bigText instead?
        if (bundle.getCharSequence(Notification.EXTRA_BIG_TEXT) != null) {
            try {
                text = bundle.getCharSequence(Notification.EXTRA_BIG_TEXT).toString();
                Logger.debug("NotificationFactory EXTRA_BIG_TEXT exists");
            } catch (NullPointerException e) {
                Logger.debug(e,"NotificationFactory exception: " + e.toString() + " text: " + text);
            }
        }

        if (isNormalNotification) {
            String notificationPackage = statusBarNotification.getPackageName();
            try {
                int iconId = bundle.getInt(Notification.EXTRA_SMALL_ICON);
                PackageManager manager = context.getPackageManager();
                Resources resources = manager.getResourcesForApplication(notificationPackage);

                Drawable drawable = resources.getDrawable(notification.icon);
                Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);

                if (bitmap.getWidth() > 48) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true);
                }
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] intArray = new int[width * height];
                bitmap.getPixels(intArray, 0, width, 0, 0, width, height);

                notificationData.setIcon(intArray);
                notificationData.setIconWidth(width);
                notificationData.setIconHeight(height);
            } catch (Exception e) {
                notificationData.setIcon(new int[]{});
                Logger.error("Failed to get bitmap from {} notification", notificationPackage);
            }
            extractImagesFromNotification(statusBarNotification, notificationData);

        } else {
            addMapBitmap(statusBarNotification, notificationData);
        }

        notificationData.setId(statusBarNotification.getId());
        notificationData.setKey(statusBarNotification.getKey());
        notificationData.setTitle(title);
        notificationData.setText(text);
        notificationData.setTime(notificationTime);
        notificationData.setForceCustom(false);
        notificationData.setHideReplies(false);
        notificationData.setHideButtons(false);

        return notificationData;
    }

    private static void extractImagesFromNotification(StatusBarNotification statusBarNotification, NotificationData notificationData) {

        if (Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_LARGE_ICON, Constants.PREF_NOTIFICATIONS_LARGE_ICON_DEFAULT)) {
            extractLargeIcon(statusBarNotification, notificationData);
        }

        if (Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_IMAGES, Constants.PREF_NOTIFICATIONS_IMAGES_DEFAULT)) {
            extractPicture(statusBarNotification, notificationData);
        }
    }

    private static void extractLargeIcon(StatusBarNotification statusBarNotification, NotificationData notificationData) {
        Logger.trace("notification key: {}", statusBarNotification.getKey());
        try {
            Bundle bundle = statusBarNotification.getNotification().extras;
            Bitmap largeIcon = (Bitmap) bundle.get(Notification.EXTRA_LARGE_ICON);
            if (largeIcon != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                largeIcon.compress(Bitmap.CompressFormat.PNG, 80, stream);
                byte[] byteArray = stream.toByteArray();

                notificationData.setLargeIcon(byteArray);
                notificationData.setLargeIconWidth(largeIcon.getWidth());
                notificationData.setLargeIconHeight(largeIcon.getHeight());
            } else
                Logger.warn("notification key: {} null largeIcon!", statusBarNotification.getKey());
        } catch (Exception exception) {
            Logger.error(exception, exception.getMessage());
        }
    }

    private static void extractPicture(StatusBarNotification statusBarNotification, NotificationData notificationData) {
        Logger.trace("notification key: {}", statusBarNotification.getKey());
        try {
            Bundle bundle = statusBarNotification.getNotification().extras;
            Bitmap originalBitmap = (Bitmap) bundle.get(Notification.EXTRA_PICTURE);
            //Bitmap largeIconBig = (Bitmap) bundle.get(Notification.EXTRA_LARGE_ICON_BIG);
            //Logger.info("image_uri: {}", bundle.get(NotificationCompat.EXTRA_BACKGROUND_IMAGE_URI));

            if (originalBitmap != null) {
                addBitmap(originalBitmap, notificationData);
            } else
                Logger.warn("notification key: {} null picture!", statusBarNotification.getKey());

        } catch (Exception exception) {
            Logger.error(exception, exception.getMessage());
        }
    }

    private static void addBitmap(Bitmap bitmap, NotificationData notificationData) {
        if (bitmap.getWidth() > 320) {

            float horizontalScaleFactor = bitmap.getWidth() / 320f;
            float destHeight = bitmap.getHeight() / horizontalScaleFactor;
            bitmap = Bitmap.createScaledBitmap(bitmap, 320, (int) destHeight, false);
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
        byte[] byteArray = stream.toByteArray();

        notificationData.setPicture(byteArray);
        notificationData.setPictureWidth(bitmap.getWidth());
        notificationData.setPictureHeight(bitmap.getHeight());
    }

    private static void addMapBitmap(StatusBarNotification statusBarNotification, NotificationData notificationData) {
        Logger.trace("notification key: {}", statusBarNotification.getKey());
        try {
            Bundle bundle = statusBarNotification.getNotification().extras;
            Bitmap bitmap = (Bitmap) bundle.get(Notification.EXTRA_LARGE_ICON);
            if (bitmap != null) {
                if (bitmap.getWidth() > 48) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true);
                }
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] intArray = new int[width * height];
                bitmap.getPixels(intArray, 0, width, 0, 0, width, height);

                notificationData.setIcon(intArray);
                notificationData.setIconWidth(width);
                notificationData.setIconHeight(height);

            } else
                Logger.warn("notification key: {} null largeIcon!", statusBarNotification.getKey());

        } catch (Exception exception) {
            Logger.error(exception, exception.getMessage());
        }
    }

    public static NotificationData getMapNotification(Context context, StatusBarNotification statusBarNotification) {

        Logger.debug("getMapNotification package: {} key: {}", statusBarNotification.getPackageName(), statusBarNotification.getKey());

        NotificationData notificationData = null;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
             notificationData = fromStatusBarNotification(context, statusBarNotification);

            RemoteViews rmv = getContentView(context, statusBarNotification.getNotification());
            RemoteViews brmv = getBigContentView(context, statusBarNotification.getNotification());

            if (rmv == null) {
                rmv = brmv;
                Logger.debug("using BigContentView");
            } else {
                Logger.debug("using ContentView");
            }

            if (rmv != null) {

                //Get text from RemoteView using reflection
                List<String> txt = extractText(rmv);
                if ((txt.size() > 0) && (!(txt.get(0).isEmpty()))) {

                    //Get navigation icon from a child View drawn on Canvas
                    try {
                        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                        View layout = inflater.inflate(R.layout.nav_layout, null);
                        ViewGroup frame = layout.findViewById(R.id.layout_navi);
                        frame.removeAllViews();
                        View newView = rmv.apply(context, frame);
                        frame.addView(newView);
                        View viewImage = ((ViewGroup) newView).getChildAt(0);
                        //View outerLayout = ((ViewGroup) newView).getChildAt(1);
                        viewImage.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                        Bitmap bitmap = Bitmap.createBitmap(viewImage.getMeasuredWidth(), viewImage.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        viewImage.layout(0, 0, viewImage.getMeasuredWidth(), viewImage.getMeasuredHeight());
                        viewImage.draw(canvas);
                        bitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true);

                        int width = bitmap.getWidth();
                        int height = bitmap.getHeight();
                        int[] intArray = new int[width * height];
                        bitmap.getPixels(intArray, 0, width, 0, 0, width, height);
                        Logger.info("bitmap dimensions: " + width + " x " + height);

                        notificationData.setIcon(intArray);
                        notificationData.setIconWidth(width);
                        notificationData.setIconHeight(height);
                    } catch (Exception e) {
                        notificationData.setIcon(new int[]{});
                        Logger.error(e, "failed to get bitmap with exception: {}", e.getMessage());
                    }

                    notificationData.setTitle(txt.get(0));
                    if (txt.size() > 1)
                        notificationData.setText(txt.get(1));
                    else
                        notificationData.setText("");
                    notificationData.setHideReplies(true);
                    notificationData.setHideButtons(false);
                    notificationData.setForceCustom(true);
                }
                return notificationData;

            } else {
                Logger.warn("null remoteView");
                return null;
            }

        } else {
            isNormalNotification = false;
            notificationData = fromStatusBarNotification(context, statusBarNotification);
            notificationData.setHideReplies(true);
            notificationData.setHideButtons(false);
            notificationData.setForceCustom(true);
            isNormalNotification = true;
            return notificationData;
        }
    }

    private static List<String> extractText(RemoteViews views) {
        // Use reflection to examine the m_actions member of the given RemoteViews object.
        List<String> text = new ArrayList<>();
        try {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);
            //int counter = 0;

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);

            // Find the setText() reflection actions
            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                if (tag != 2) continue;

                // View ID
                parcel.readInt();

                String methodName = parcel.readString();
                if (methodName == null)
                    continue;
                    // Save strings
                else {

                    if (methodName.equals("setText")) {
                        // Parameter type (10 = Character Sequence)
                        parcel.readInt();

                        // Store the actual string
                        String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                        text.add(t);
                        //Log.d(Constants.TAG, "NotificationService extractText " + counter + " t: " + t);
                        //counter++;
                    }
                }
                parcel.recycle();
            }
        }
        // It's not usually good style to do this, but then again, neither is the use of reflection...
        catch (Exception e) {
            Logger.error(e, "extractText exception: {}", e.getMessage());
            text.add("ERROR");
        }
        return text;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static RemoteViews getBigContentView(Context context, Notification notification) {
        if(notification.bigContentView != null)
            return notification.bigContentView;
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return Notification.Builder.recoverBuilder(context, notification).createBigContentView();
        else
            return null;
    }

    private static RemoteViews getContentView(Context context, Notification notification) {
        if(notification.contentView != null)
            return notification.contentView;
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return Notification.Builder.recoverBuilder(context, notification).createContentView();
        else
            return null;
    }

}
