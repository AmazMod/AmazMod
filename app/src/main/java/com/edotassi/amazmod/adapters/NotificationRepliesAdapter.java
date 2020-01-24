package com.edotassi.amazmod.adapters;

import android.app.Activity;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.R;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
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

public class NotificationRepliesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ItemTouchHelper androidItemTouchHelper;

    private List<Reply> mList;
    private final Activity mActivity;

    public NotificationRepliesAdapter(Activity mActivity, ItemTouchHelper androidItemTouchHelper) {
        this.androidItemTouchHelper = androidItemTouchHelper;
        this.mActivity = mActivity;
        mList = new ArrayList<>();
        update();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = View.inflate(mActivity, R.layout.row_drag_replies, null);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if (viewHolder instanceof MyViewHolder) {
            final MyViewHolder holder = (MyViewHolder) viewHolder;

            Reply mReply = mList.get(i);

            holder.setReply(mReply);
            holder.value.setText(mReply.getValue());
            holder.handle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (androidItemTouchHelper != null) androidItemTouchHelper.startDrag(holder);
                    return true;
                }
            });


        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }


    public void update() {
        new Thread(() -> {
            mList = loadReplies();
            mActivity.runOnUiThread(() -> {
                notifyDataSetChanged();
            });
        }).start();
    }


    private List<Reply> loadReplies() {
        try {
            String repliesJson = Prefs.getString(Constants.PREF_NOTIFICATIONS_REPLIES, Constants.PREF_DEFAULT_NOTIFICATIONS_REPLIES);
            if (repliesJson.equals("null")) {
                repliesJson = "[]";
            }
            Type listType = new TypeToken<List<Reply>>() {
            }.getType();
            return new Gson().fromJson(repliesJson, listType);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public void onItemMove(final int initialPosition, final int finalPosition) {


        if (initialPosition < mList.size() && finalPosition < mList.size()) {

            if (initialPosition < finalPosition) {

                for (int i = initialPosition; i < finalPosition; i++) {
                    Collections.swap(mList, i, i + 1);
                }
            } else {
                for (int i = initialPosition; i > finalPosition; i--) {
                    Collections.swap(mList, i, i - 1);
                }

            }

            notifyItemMoved(initialPosition, finalPosition);

        }

        new Thread(() -> {
            save();
        }).start();
    }

    public void save() {
        Logger.debug("Saving order to preferences");
        Gson gson = new Gson();
        String repliesJson = "[]";
        if (mList != null) {
            repliesJson = gson.toJson(mList);
        }
        Prefs.putString(Constants.PREF_NOTIFICATIONS_REPLIES, repliesJson);
    }

    public void addItem(String reply) {
        Reply r = new Reply();
        r.setValue(reply);
        mList.add(r);
        notifyDataSetChanged();
    }

    public void removeItem(Reply item) {
        int posicao = mList.indexOf(item);
        removeItem(posicao);
    }

    public void removeItem(final int position) {
        Snackbar.make(mActivity.findViewById(R.id.activity_notification), mActivity.getString(R.string.notification_replies_confirm_removal), 2500)
                .setAction(mActivity.getString(R.string.remove), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mList.remove(position);
                        notifyItemRemoved(position);
                    }
                }).addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                notifyItemChanged(position);
            }
        }).show();
    }


    public void editItem(final Reply item) {
        int posicao = mList.indexOf(item);
        new MaterialDialog.Builder(mActivity)
                .title(R.string.edit_reply)
                .content(R.string.enter_the_text_you_want_as_an_answer)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("", item.getValue(), (dialog, input) -> {
                    item.setValue(input.toString());
                    notifyItemChanged(posicao);
                }).show();
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        TextView value;
        TextView handle;
        ImageButton deleteButton, editButton;


        private Reply reply;

        MyViewHolder(View itemView) {
            super(itemView);
            value = itemView.findViewById(R.id.row_replies_value);
            handle = itemView.findViewById(R.id.row_handle);
            deleteButton = itemView.findViewById(R.id.row_replies_delete);
            editButton = itemView.findViewById(R.id.row_replies_edit);

            deleteButton.setOnClickListener(v -> removeItem(reply));

            editButton.setOnClickListener(v -> {
                String string = reply.getValue();
                Logger.debug("NotificationRepliesDragActivity onLongClick string: " + string);
                editItem(reply);
            });

        }

        public void setReply(Reply reply) {
            this.reply = reply;
        }
    }


}
