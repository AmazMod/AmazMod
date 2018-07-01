package com.edotassi.amazmod.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.RepliesAdapter;
import com.edotassi.amazmod.support.Reply;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pixplicity.easyprefs.library.Prefs;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NotificationRepliesActivity extends AppCompatActivity implements RepliesAdapter.Bridge {

    @BindView(R.id.activity_notification_replies_list)
    ListView listView;

    private List<Reply> repliesValues;
    private RepliesAdapter repliesAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_replies);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.replies);

        ButterKnife.bind(this);

        repliesAdapter = new RepliesAdapter(this, R.layout.row_replies, new ArrayList<Reply>());
        listView.setAdapter(repliesAdapter);

        loadReplies();
    }

    @Override
    protected void onPause() {
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

    @Override
    public void onDeleteReply(Reply reply) {
        repliesValues.remove(reply);
        repliesAdapter.clear();
        repliesAdapter.addAll(repliesValues);
        repliesAdapter.notifyDataSetChanged();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @OnClick(R.id.activity_notification_replies_add)
    protected void onAddReply() {
        new MaterialDialog.Builder(this)
                .title(R.string.add_new_reply)
                .content(R.string.enter_the_text_you_want_as_an_answer)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("", "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        // Do something
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
}
