package com.amazmod.service.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.emoji.widget.EmojiTextView;

import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.tinylog.Logger;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.events.SilenceApplicationEvent;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.NotificationWearActivity;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.FragmentUtil;

import java.util.ArrayList;
import java.util.List;

import amazmod.com.models.Reply;
import amazmod.com.transport.data.NotificationData;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static android.content.Context.VIBRATOR_SERVICE;

public class NotificationFragment extends Fragment implements DelayedConfirmationView.DelayedConfirmationListener {

    LinearLayout replies_layout;
    TextView title;
    TextView time;
    TextView text;
    ImageView icon, iconBadge;
    ImageView image;
    ImageView picture;
    Button deleteButton, replyButton, muteButton, replyEditClose, replyEditSend;
    EditText replyEditText;
    BoxInsetLayout rootLayout;
    //LinearLayout repliesLayout;
    NotificationData notificationData;
    ScrollView scrollView;

    private TextView delayedConfirmationViewTitle, delayedConfirmationViewBottom;
    private DelayedConfirmationView delayedConfirmationView;

    private LinearLayout repliesListView, repliesEditTextContainer, muteListView;


    private String key, mode, notificationKey, selectedReply, selectedSilenceTime;
    private boolean enableInvertedTheme, disableDelay;
    ;
    private Context mContext;
    private FragmentUtil util;
    private SettingsManager settingsManager;

    private String action;
    private static final String ACTION_REPLY = "reply";
    private static final String ACTION_DELETE = "del";
    private static final String ACTION_MUTE = "mute";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Logger.info("NotificationFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        key = getArguments().getString(NotificationWearActivity.KEY);
        mode = getArguments().getString(NotificationWearActivity.MODE);

        Logger.info("NotificationFragment onCreate: key " + key + " | mode: " + mode);
        // todo getCustomNotification(key).getKey() returns Null if you answer a messenger/watchapp call, is this correct?
        try {
            notificationKey = NotificationStore.getCustomNotification(key).getKey();
        }catch(NullPointerException e){
            notificationKey = "null";
            Logger.error(e, e.getLocalizedMessage());
        }
        settingsManager = new SettingsManager(this.mContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Logger.info("NotificationFragment onCreateView");

        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.info("NotificationFragment onViewCreated");

        updateContent();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void updateContent() {

        final String key = getArguments().getString(NotificationWearActivity.KEY);
        final String mode = getArguments().getString(NotificationWearActivity.MODE);
        notificationData = NotificationStore.getCustomNotification(key);

        Logger.info("NotificationFragment updateContent context: " + mContext + " | key: " + key);

        util = new FragmentUtil(mContext);
        disableDelay = util.getDisableDelay();

        replies_layout = getActivity().findViewById(R.id.fragment_custom_notification_replies_layout);
        title = getActivity().findViewById(R.id.fragment_custom_notification_title);
        time = getActivity().findViewById(R.id.fragment_custom_notification_time);
        text = getActivity().findViewById(R.id.fragment_custom_notification_text);
        icon = getActivity().findViewById(R.id.fragment_custom_notification_icon);
        iconBadge = getActivity().findViewById(R.id.fragment_custom_notification_icon_badge);
        picture = getActivity().findViewById(R.id.fragment_custom_notification_picture);
        rootLayout = getActivity().findViewById(R.id.fragment_custom_root_layout);
        scrollView = getActivity().findViewById(R.id.fragment_custom_scrollview);
        image = getActivity().findViewById(R.id.fragment_custom_notification_replies_image);
        delayedConfirmationViewTitle = getActivity().findViewById(R.id.fragment_notification_delayedview_title);
        delayedConfirmationView = getActivity().findViewById(R.id.fragment_notification_delayedview);
        delayedConfirmationViewBottom = getActivity().findViewById(R.id.fragment_notification_delayedview_bottom);

        //Delete related stuff

        deleteButton = getActivity().findViewById(R.id.fragment_delete_button);{

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Logger.debug("NotificationFragment updateContent: deleteButton clicked!");
                    muteListView.setVisibility(View.GONE);
                    repliesListView.setVisibility(View.GONE);
                    sendDeleteCommand(v);
                }
            });
        }


        //Replies related stuff
        repliesListView = getActivity().findViewById(R.id.fragment_reply_list);
        repliesEditTextContainer = getActivity().findViewById(R.id.fragment_notifications_replies_edittext_container);
        replyEditText = getActivity().findViewById(R.id.fragment_notifications_replies_edittext);
        replyEditClose = getActivity().findViewById(R.id.fragment_notifications_replies_edittext_button_close);
        replyEditSend = getActivity().findViewById(R.id.fragment_notifications_replies_edittext_button_reply);
        replyButton = getActivity().findViewById(R.id.fragment_notification_reply_button);
        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.debug("NotificationFragment updateContent: replyButton clicked!");
                if (repliesListView.getVisibility() == View.VISIBLE) {
                    repliesListView.setVisibility(View.GONE);
                    focusOnViewBottom(scrollView, replyButton);
                } else {
                    // Prepare the View for the animation
                    repliesListView.setVisibility(View.VISIBLE);
                    muteListView.setVisibility(View.GONE);
                    focusOnView(scrollView, replyButton);
                }
            }
        });

        //Mute related stuff
        muteListView = getActivity().findViewById(R.id.fragment_mute_list);
        muteButton = getActivity().findViewById(R.id.fragment_notification_mute_button);
        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.debug("NotificationFragment updateContent: muteButton clicked!");
                if (muteListView.getVisibility() == View.VISIBLE) {
                    muteListView.setVisibility(View.GONE);
                } else {
                    //Prepare the View for the animation
                    muteListView.setVisibility(View.VISIBLE);
                    repliesListView.setVisibility(View.GONE);
                    focusOnView(scrollView, muteButton);
                }
            }
        });

        //Load preferences
        boolean disableNotificationText = util.getDisableNotificationText();
        enableInvertedTheme = util.getInvertedTheme();

        //Increase minimum height so reply button stays at the Verges bottom of screen, just as on Pace and Stratos
        if (DeviceUtil.isVerge()){
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());
            replies_layout.setMinimumHeight(px);
        }

        //Load Replies and Mute Options
        loadReplies();
        loadMuteOptions();

        // Set theme and font size
        //Log.d(Constants.TAG, "NotificationActivity enableInvertedTheme: " + enableInvertedTheme + " / fontSize: " + fontSize);
        if (enableInvertedTheme) {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.white));
            time.setTextColor(getResources().getColor(R.color.black));
            title.setTextColor(getResources().getColor(R.color.black));
            text.setTextColor(getResources().getColor(R.color.black));
            iconBadge.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
            delayedConfirmationViewTitle.setTextColor(getResources().getColor(R.color.black));
            delayedConfirmationViewBottom.setTextColor(getResources().getColor(R.color.black));
        } else
            rootLayout.setBackgroundColor(getResources().getColor(R.color.black));

        time.setTextSize(util.getFontSizeSP());
        title.setTextSize(util.getFontSizeSP());
        text.setTextSize(util.getFontSizeSP());

        try {
            Logger.info("NotificationFragment updateContent try");

            //hideReplies = notificationData.getHideReplies();

            populateNotificationIcon(icon, iconBadge, notificationData);
            populateNotificationPicture(picture, notificationData);

            if (!hasPicture(notificationData)) {
                title.setText(notificationData.getTitle());
                time.setText(notificationData.getTime());

                util.setFontLocale(text, util.getDefaultLocale());
                text.setText(notificationData.getText());
            } else {
                title.setText(notificationData.getTitle() + " - " + notificationData.getTime());
                time.setVisibility(View.GONE);
                text.setVisibility(View.GONE);
            }

            if (notificationData.getVibration() > 0 && NotificationWearActivity.MODE_ADD.equals(mode)) {
                Vibrator vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);
                try {
                    if (vibrator != null) {
                        vibrator.vibrate(notificationData.getVibration());
                    }
                } catch (RuntimeException ex) {
                    Logger.error("NotificationFragment updateContent - Exception: " + ex.toString()
                            + " notificationData: " + notificationData);
                }
            }
        } catch (NullPointerException ex) {
            Logger.error("NotificationFragment updateContent - Exception: " + ex.toString()
                    + " notificationData: " + notificationData);
            title.setText("AmazMod");
            text.setText("Welcome to AmazMod");
            time.setText("00:00");
            icon.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.amazmod));
            //hideReplies = true;
        }

        if (disableNotificationText) {
            text.setVisibility(View.GONE);
            picture.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
            if (enableInvertedTheme)
                image.setImageDrawable(getResources().getDrawable(R.drawable.outline_screen_lock_portrait_black_48));
            else
                image.setImageDrawable(getResources().getDrawable(R.drawable.outline_screen_lock_portrait_white_48));
        }
    }


    private final void focusOnView(final ScrollView scroll, final View view) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                int vPosition = view.getTop();
                scroll.smoothScrollTo(0, vPosition);
            }
        });
    }


    private final void focusOnViewBottom(final ScrollView scroll, final View view) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics metrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int height = metrics.heightPixels;
                //int width = metrics.widthPixels;
                int vPosition = view.getTop() + view.getHeight() - height;
                scroll.smoothScrollTo(0, vPosition);
            }
        });
    }

    public static NotificationFragment newInstance(String key, String mode) {

        Logger.info("NotificationFragment newInstance key: " + key);
        NotificationFragment myFragment = new NotificationFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NotificationWearActivity.KEY, key);
        bundle.putString(NotificationWearActivity.MODE, mode);
        myFragment.setArguments(bundle);

        return myFragment;
    }

    private void populateNotificationIcon(ImageView iconView, ImageView iconAppView, NotificationData notificationData) {
        try {
            byte[] largeIconData = notificationData.getLargeIcon();
            if ((largeIconData != null) && (largeIconData.length > 0)) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(largeIconData, 0, largeIconData.length);

                RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);

                roundedBitmapDrawable.setCircular(true);
                roundedBitmapDrawable.setAntiAlias(true);

                iconView.setImageDrawable(roundedBitmapDrawable);
                setIconBadge(iconAppView);
            } else {
                setIconBadge(iconView);
                iconAppView.setVisibility(View.GONE);
            }
        } catch (Exception exception) {
            Logger.debug(exception.getMessage(), exception);
        }
    }

    private void setIconBadge(ImageView iconView) {
        int[] iconData = notificationData.getIcon();
        int iconWidth = notificationData.getIconWidth();
        int iconHeight = notificationData.getIconHeight();
        Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);

        //Invert color (works if the bitmap is in ARGB_8888 format)
        if (enableInvertedTheme) {
            int[] invertedIconData = new int[iconData.length];
            for (int i = 0; i < iconData.length; i++) {
                if (iconData[i] == 0xffffffff)
                    invertedIconData[i] = 0xff000000;
                else
                    invertedIconData[i] = iconData[i];
            }
            bitmap.setPixels(invertedIconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);

        } else
            bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);

        iconView.setImageBitmap(bitmap);
    }

    private boolean hasPicture(NotificationData notificationData) {
        byte[] pictureData = notificationData.getPicture();
        return (pictureData != null) && (pictureData.length > 0);
    }

    private void populateNotificationPicture(ImageView pictureView, NotificationData notificationData) {
        try {
            if (hasPicture(notificationData)) {
                byte[] pictureData = notificationData.getPicture();
                Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                pictureView.setImageBitmap(bitmap);
                pictureView.setVisibility(View.VISIBLE);
            }
        } catch (Exception exception) {
            Logger.debug(exception.getMessage(), exception);
        }
    }


    @SuppressLint("CheckResult")
    public void loadReplies() {
        Logger.info("NotificationFragment loadReplies");
        List<Reply> replyList = util.listReplies();
        final LayoutInflater inflater = LayoutInflater.from(NotificationFragment.this.mContext);
        for (final Reply reply : replyList) {
            final View row = inflater.inflate(R.layout.row_reply, repliesListView, false);
            EmojiTextView replyView = row.findViewById(R.id.row_reply_text);
            replyView.setText(reply.getValue());
            if (enableInvertedTheme) {
                replyView.setTextColor(getResources().getColor(R.color.black));
                replyView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.send, 0, 0, 0);
            }
            replyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedReply = reply.getValue();
                    sendReply(view);
                    Logger.debug("NotificationFragment replyView OnClick: " + selectedReply);
                }
            });
            // set item content in view
            repliesListView.addView(row);
        }
        final View row = inflater.inflate(R.layout.row_reply, repliesListView, false);
        EmojiTextView replyView = row.findViewById(R.id.row_reply_text);
        replyView.setText(getResources().getString(R.string.keyboard));
        if (enableInvertedTheme) {
            replyView.setTextColor(getResources().getColor(R.color.black));
            replyView.setCompoundDrawables(getResources().getDrawable(R.drawable.send), null, null, null);
        }
        //replyView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        replyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logger.debug("NotificationFragment replyView OnClick: KEYBOARD");
                scrollView.setVisibility(View.GONE);
                repliesEditTextContainer.setVisibility(View.VISIBLE);
                ((NotificationWearActivity) getActivity()).stopTimerFinish();
                ((NotificationWearActivity) getActivity()).setKeyboardVisible(true);

                replyEditSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectedReply = replyEditText.getText().toString();
                        repliesEditTextContainer.setVisibility(View.GONE);
                        sendReply(v);
                    }
                });
                //Cancel button
                replyEditClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scrollView.setVisibility(View.VISIBLE);
                        repliesEditTextContainer.setVisibility(View.GONE);
                        ((NotificationWearActivity) getActivity()).setKeyboardVisible(false);
                        ((NotificationWearActivity) getActivity()).startTimerFinish();
                    }
                });
            }
        });
        // set item content in view
        repliesListView.addView(row);
    }


    private void loadMuteOptions() {

        List<Integer> silenceList = new ArrayList<>();
        silenceList.add(5);
        silenceList.add(15);
        silenceList.add(30);
        silenceList.add(60);

        LayoutInflater inflater = LayoutInflater.from(NotificationFragment.this.mContext);

        //Add one View for each item in SilenceList
        for (final Integer silence : silenceList) {
            final View row = inflater.inflate(R.layout.row_mute, muteListView, false);
            EmojiTextView muteView = row.findViewById(R.id.row_mute_value);
            muteView.setText(silence.toString() + " minutes");
            if (enableInvertedTheme) {
                muteView.setTextColor(getResources().getColor(R.color.black));
            }
            muteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedSilenceTime = silence.toString();
                    sendMuteCommand(view);
                    Logger.debug("NotificationFragment loadMuteOptions muteView OnClick: " + selectedSilenceTime);
                }
            });
            // set item content in view
            muteListView.addView(row);
        }

        //Create a Item for Muting App for One Day
        final View row_day = inflater.inflate(R.layout.row_mute, muteListView, false);
        EmojiTextView muteView = row_day.findViewById(R.id.row_mute_value);
        muteView.setText("One Day");
        if (enableInvertedTheme) {
            muteView.setTextColor(getResources().getColor(R.color.black));
        }
        muteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedSilenceTime = "1440";
                sendMuteCommand(view);
                Logger.debug("NotificationFragment loadMuteOptions muteView OnClick: " + selectedSilenceTime);
            }
        });
        muteListView.addView(row_day);


        //Create a Item for BLOCKING APP (Removes it From List of Apps)
        final View row_block = inflater.inflate(R.layout.row_mute, muteListView, false);
        muteView = row_block.findViewById(R.id.row_mute_value);
        muteView.setText("BLOCK APP");
        if (enableInvertedTheme)
            muteView.setTextColor(getResources().getColor(R.color.dark_red));
        else
            muteView.setTextColor(getResources().getColor(R.color.red));
        muteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedSilenceTime = Constants.BLOCK_APP;
                sendMuteCommand(view);
                Logger.debug("NotificationFragment loadMuteOptions muteView OnClick: " + selectedSilenceTime);
            }
        });
        muteListView.addView(row_block);
    }


    private void sendDeleteCommand(View v) {
        sendCommand(ACTION_DELETE, v);
    }

    private void sendReply(View v) {
        ((NotificationWearActivity) getActivity()).setKeyboardVisible(false);
        sendCommand(ACTION_REPLY, v);
    }

    private void sendMuteCommand(View v) {
        sendCommand(ACTION_MUTE, v);
    }

    private void sendCommand(String command, View v) {
        action = command;
        String confirmationMessage;
        switch (action) {
            case ACTION_DELETE:
                delayedConfirmationView.setTotalTimeMs(1500);
                confirmationMessage = "REMOVING";
                break;
            case ACTION_MUTE:
                delayedConfirmationView.setTotalTimeMs(3000);
                confirmationMessage = "MUTING";
                break;
            case ACTION_REPLY:
                delayedConfirmationView.setTotalTimeMs(3000);
                confirmationMessage = "SENDING REPLY";
                break;
            default:
                return;
        }
        ((NotificationWearActivity) getActivity()).stopTimerFinish();
        if (disableDelay) {
            Logger.info("NotificationFragment sendCommand without delay : command '" + command + "'");
            onTimerFinished(v);
        } else {
            Logger.debug("NotificationFragment sendCommand with delay : command '" + command + "'");
            util.setParamMargins(0, 24, 0, 4);
            showDelayed(confirmationMessage);
            Logger.info("NotificationFragment sendSilenceCommand isPressed: " + delayedConfirmationView.isPressed());
        }
    }

    @Override
    public void onTimerSelected(View v) {
        action = "";
        v.setPressed(true);
        delayedConfirmationView.reset();
        // Prevent onTimerFinished from being heard.
        ((DelayedConfirmationView) v).setListener(null);
        ((NotificationWearActivity) getActivity()).startTimerFinish();
        hideDelayed();
        Logger.info("NotificationFragment onTimerSelected isPressed: " + v.isPressed());
    }

    private void showDelayed(String text) {
        scrollView.setVisibility(View.GONE);
        repliesEditTextContainer.setVisibility(View.GONE);
        delayedConfirmationViewTitle.setText(text);
        delayedConfirmationViewTitle.setVisibility(View.VISIBLE);
        delayedConfirmationViewBottom.setVisibility(View.VISIBLE);
        delayedConfirmationView.setVisibility(View.VISIBLE);
        delayedConfirmationView.setPressed(false);
        delayedConfirmationView.start();
        delayedConfirmationView.setListener(this);

    }

    private void hideDelayed() {
        scrollView.setVisibility(View.VISIBLE);
        delayedConfirmationView.setVisibility(View.GONE);
        delayedConfirmationViewTitle.setVisibility(View.GONE);
        delayedConfirmationViewBottom.setVisibility(View.GONE);
    }

    @Override
    public void onTimerFinished(View v) {
        Logger.info("NotificationFragment onTimerFinished isPressed: " + v.isPressed());

        if (v instanceof DelayedConfirmationView)
            ((DelayedConfirmationView) v).setListener(null);

        String confirmationMessage;

        switch (action) {
            case ACTION_DELETE:
                confirmationMessage = "Deleted!";
                break;
            case ACTION_MUTE:
                confirmationMessage = "Muted!";
                break;
            case ACTION_REPLY:
                confirmationMessage = "Reply Sent!";
                break;
            default:
                return;
        }
        Logger.info("NotificationFragment onTimerFinished action :" + action);

        Intent intent = new Intent(mContext, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, confirmationMessage);
        startActivity(intent);

        NotificationStore.removeCustomNotification(key);
        NotificationStore.setNotificationCount(mContext);
        if (NotificationWearActivity.MODE_VIEW.equals(mode))
            WearNotificationsFragment.getInstance().loadNotifications();

        switch (action) {
            case ACTION_DELETE:
                break;
            case ACTION_MUTE:
                EventBus.getDefault().post(new SilenceApplicationEvent(notificationKey, selectedSilenceTime));
                break;
            case ACTION_REPLY:
                EventBus.getDefault().post(new ReplyNotificationEvent(notificationKey, selectedReply));
                break;
        }
        getActivity().finish();
    }

}
