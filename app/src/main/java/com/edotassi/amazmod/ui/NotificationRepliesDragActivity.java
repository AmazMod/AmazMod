package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.NotificationRepliesAdapter;
import com.edotassi.amazmod.helpers.DynamicEventsHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.tinylog.Logger;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class NotificationRepliesDragActivity extends BaseAppCompatActivity {

    private RecyclerView mRecyclerView;
    private NotificationRepliesAdapter mAdapter;

    private FloatingActionButton fabButton;

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

        fabButton = findViewById(R.id.activity_notification_replies_add);

        mRecyclerView = findViewById(R.id.activity_notification_replies_list);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        DynamicEventsHelper.DynamicEventsCallback callback = new DynamicEventsHelper.DynamicEventsCallback() {
            @Override
            public void onItemMove(int initialPosition, int finalPosition) {
                mAdapter.onItemMove(initialPosition, finalPosition);
            }

            @Override
            public void removeItem(int position) {
                mAdapter.removeItem(position);
            }
        };
        ItemTouchHelper androidItemTouchHelper = new ItemTouchHelper(new DynamicEventsHelper(callback));
        androidItemTouchHelper.attachToRecyclerView(mRecyclerView);

        mAdapter = new NotificationRepliesAdapter(this, androidItemTouchHelper);
        mRecyclerView.setAdapter(mAdapter);



        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)  && recyclerView.canScrollVertically(-1)  && newState==RecyclerView.SCROLL_STATE_IDLE ) {
                    fabButton.hide();
                }else{
                    fabButton.show();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        mAdapter.save();
        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
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
                        mAdapter.addItem(input.toString());
                    }
                }).show();
    }
}
