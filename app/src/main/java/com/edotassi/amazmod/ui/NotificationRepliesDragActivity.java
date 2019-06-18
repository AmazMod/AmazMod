package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pixplicity.easyprefs.library.Prefs;

import org.tinylog.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import amazmod.com.models.Reply;
import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NotificationRepliesDragActivity extends BaseAppCompatActivity {

    @BindView(R.id.activity_notification_replies_list)
    ListView listView;

    private List<Reply> repliesValues;
    private RepliesDragAdapter repliesAdapter;

    private boolean mSortable = false;
    private Reply mDragReply;
    private int initalPosition;
    private int mPosition = -1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_replies);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.replies);
        } catch (NullPointerException exception) {
            Logger.error("AboutActivity onCreate exception: " + exception.getMessage());
        }

        ButterKnife.bind(this);

        repliesAdapter = new RepliesDragAdapter(this, R.layout.row_drag_replies, new ArrayList<Reply>());
        listView.setAdapter(repliesAdapter);

        loadReplies();

        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!mSortable) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        int position = listView.pointToPosition((int) event.getX(), (int) event.getY());
                        //int position = itemNum - listView.getFirstVisiblePosition();
                        if (position < 0) {
                            break;
                        }
                        if (position != mPosition) {
                            Logger.debug("NotificationRepliesDragActivity move mPosition: " + mPosition +
                                    " \\ position: " + position);
                            if (mPosition != -1) {
                                if (position > mPosition) {
                                    if (position - mPosition == 1)
                                        Collections.swap(repliesValues, mPosition, position);
                                    else {
                                        Collections.swap(repliesValues, mPosition, (position-1));
                                        Collections.swap(repliesValues, position, (position-1));
                                    }
                                } else {
                                    if (mPosition - position == 1)
                                        Collections.swap(repliesValues, mPosition, position);
                                    else {
                                        Collections.swap(repliesValues, mPosition, (position+1));
                                        Collections.swap(repliesValues, position, (position+1));
                                    }
                                }
                            }// else if (initalPosition != position)
                             //   Collections.swap(repliesValues, initalPosition, position);
                            mPosition = position;
                            repliesAdapter.remove(mDragReply);
                            repliesAdapter.insert(mDragReply, mPosition);
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_OUTSIDE: {
                        Logger.debug("NotificationRepliesDragActivity cancel initialPosition: " + initalPosition +
                                " \\ mPosition: " + mPosition);
                        stopDrag();
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public void startDrag(Reply reply) {
        mPosition = -1;
        mSortable = true;
        mDragReply = reply;
        repliesAdapter.notifyDataSetChanged();
    }

    public void stopDrag() {
        mPosition = -1;
        mSortable = false;
        mDragReply = null;
        repliesAdapter.notifyDataSetChanged();
    }

    public void edit(final Reply reply, final View v) {
        new MaterialDialog.Builder(this)
                .title(R.string.edit_reply)
                .content(R.string.enter_the_text_you_want_as_an_answer)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("", reply.getValue(), new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                        reply.setValue(input.toString());

                        repliesAdapter.clear();
                        repliesAdapter.addAll(repliesValues);
                        repliesAdapter.notifyDataSetChanged();

                        v.setBackgroundColor(Color.TRANSPARENT);
                    }
                }).show();
    }

    @Override
    protected void onPause() {
        Logger.debug("NotificationRepliesDragActivity onPause");
        Gson gson = new Gson();
        String repliesJson = gson.toJson(repliesValues);
        Prefs.putString(Constants.PREF_NOTIFICATIONS_REPLIES, repliesJson);

        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public void onDeleteReply(Reply reply) {
        repliesValues.remove(reply);
        repliesAdapter.clear();
        repliesAdapter.addAll(repliesValues);
        repliesAdapter.notifyDataSetChanged();
    }

    @OnClick(R.id.activity_notification_replies_add)
    protected void onAddReply() {
        new MaterialDialog.Builder(this)
                .title(R.string.add_new_reply)
                .content(R.string.enter_the_text_you_want_as_an_answer)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("", "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        Reply reply = new Reply();
                        reply.setValue(input.toString());
                        repliesValues.add(reply);

                        repliesAdapter.clear();
                        repliesAdapter.addAll(repliesValues);
                        repliesAdapter.notifyDataSetChanged();
                    }
                }).show();
    }

    private void loadReplies() {
        this.repliesValues = new ArrayList<>();

        this.repliesValues = loadRepliesFromPrefs();

        repliesAdapter.clear();
        repliesAdapter.addAll(repliesValues);
        repliesAdapter.notifyDataSetChanged();
    }

    private List<Reply> loadRepliesFromPrefs() {
        try {
            String repliesJson = Prefs.getString(Constants.PREF_NOTIFICATIONS_REPLIES, Constants.PREF_DEFAULT_NOTIFICATIONS_REPLIES);
            Type listType = new TypeToken<List<Reply>>() {
            }.getType();
            return new Gson().fromJson(repliesJson, listType);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }


    public class RepliesDragAdapter extends ArrayAdapter<Reply> {

        public RepliesDragAdapter(Context context, int resource, @NonNull List<Reply> objects) {
            super(context, resource, objects);

        }

        @SuppressLint("ClickableViewAccessibility")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if (listItem == null) {
                listItem = LayoutInflater.from(getContext()).inflate(R.layout.row_drag_replies, parent, false);
            }

            final Reply reply = getItem(position);

            ViewHolder viewHolder = new ViewHolder(reply);
            ButterKnife.bind(viewHolder, listItem);

            viewHolder.value.setText(reply.getValue());

            viewHolder.handle.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    initalPosition = listView.pointToPosition((int) view.getX(), (int) view.getY());
                    //initalPosition = listView.pointToPosition((int) event.getRawX(), (int) event.getRawY());
                    //initalPosition = listView.pointToPosition((int) event.getX(), (int) event.getY());
                    //initalPosition = itemNum - listView.getFirstVisiblePosition();
                    //if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        startDrag(reply);
                        return true;
                    //}
                    //return false;
                }
            });

            final View lItem = listItem;
            viewHolder.value.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    lItem.setBackgroundColor(Color.parseColor("#99FF4081"));
                    String string = reply.getValue();
                    Logger.debug("NotificationRepliesDragActivity onLongClick string: " + string);
                    edit(reply, lItem);
                    return false;
                }
            });

            if (mDragReply != null && mDragReply == reply) {
                listItem.setBackgroundColor(Color.parseColor("#99303F9F"));
            } else {
                listItem.setBackgroundColor(Color.TRANSPARENT);
            }

            return listItem;
        }

        public class ViewHolder {

            @BindView(R.id.row_replies_value)
            TextView value;
            @BindView(R.id.row_handle)
            TextView handle;

            private Reply reply;

            protected ViewHolder(Reply reply) {
                this.reply = reply;
            }

            @OnClick(R.id.row_replies_delete)
            public void onDeleteClick() {
                onDeleteReply(reply);
            }
        }

    }
}
