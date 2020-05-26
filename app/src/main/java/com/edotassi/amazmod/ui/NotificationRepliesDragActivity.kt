package com.edotassi.amazmod.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.edotassi.amazmod.R
import com.edotassi.amazmod.adapters.NotificationRepliesAdapter
import com.edotassi.amazmod.helpers.DynamicEventsHelper
import com.edotassi.amazmod.helpers.DynamicEventsHelper.DynamicEventsCallback
import com.edotassi.amazmod.helpers.KtLogger
import kotlinx.android.synthetic.main.activity_notification_replies.*

class NotificationRepliesDragActivity : BaseAppCompatActivity() {

    private lateinit var mAdapter: NotificationRepliesAdapter

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_replies)
        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setTitle(R.string.replies)
        } catch (exception: NullPointerException) {
            KtLogger.error("AboutActivity onCreate exception: " + exception.message)
        }
        ButterKnife.bind(this)
        val mLayoutManager = LinearLayoutManager(this)
        mLayoutManager.orientation = LinearLayoutManager.VERTICAL
        activity_notification_replies_list.setLayoutManager(mLayoutManager)
        val callback: DynamicEventsCallback = object : DynamicEventsCallback {
            override fun onItemMove(initialPosition: Int, finalPosition: Int) {
                mAdapter.onItemMove(initialPosition, finalPosition)
            }

            override fun removeItem(position: Int) {
                mAdapter.removeItem(position)
            }
        }
        val androidItemTouchHelper = ItemTouchHelper(DynamicEventsHelper(callback))
        androidItemTouchHelper.attachToRecyclerView(activity_notification_replies_list)
        mAdapter = NotificationRepliesAdapter(this, androidItemTouchHelper)
        activity_notification_replies_list.adapter = mAdapter
        activity_notification_replies_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    activity_notification_replies_add.hide()
                } else {
                    activity_notification_replies_add.show()
                }
            }
        })
    }

    override fun onPause() {
        mAdapter.save()
        super.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    @OnClick(R.id.activity_notification_replies_add)
    fun onAddReply() {
        MaterialDialog.Builder(this)
                .title(R.string.add_new_reply)
                .content(R.string.enter_the_text_you_want_as_an_answer)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("", "") { dialog, input -> mAdapter.addItem(input.toString()) }.show()
    }
}