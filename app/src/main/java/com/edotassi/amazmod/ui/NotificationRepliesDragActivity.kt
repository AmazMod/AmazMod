package com.edotassi.amazmod.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.edotassi.amazmod.R
import com.edotassi.amazmod.adapters.NotificationRepliesAdapter
import com.edotassi.amazmod.helpers.DynamicEventsHelper
import com.edotassi.amazmod.helpers.DynamicEventsHelper.DynamicEventsCallback
import kotlinx.android.synthetic.main.activity_notification_replies.*
import org.tinylog.kotlin.Logger

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
            Logger.error("AboutActivity onCreate exception: " + exception.message)
        }
        val mLayoutManager = LinearLayoutManager(this)
        mLayoutManager.orientation = LinearLayoutManager.VERTICAL
        replies_list.layoutManager = mLayoutManager
        val callback: DynamicEventsCallback = object : DynamicEventsCallback {
            override fun onItemMove(initialPosition: Int, finalPosition: Int) {
                mAdapter.onItemMove(initialPosition, finalPosition)
            }

            override fun removeItem(position: Int) {
                mAdapter.removeItem(position)
            }
        }
        val androidItemTouchHelper = ItemTouchHelper(DynamicEventsHelper(callback))
        androidItemTouchHelper.attachToRecyclerView(replies_list)
        mAdapter = NotificationRepliesAdapter(this, androidItemTouchHelper)
        replies_list.adapter = mAdapter
        replies_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    reply_add.hide()
                } else {
                    reply_add.show()
                }
            }
        })

        reply_add.setOnClickListener {
            MaterialDialog.Builder(this)
                    .title(R.string.add_new_reply)
                    .content(R.string.enter_the_text_you_want_as_an_answer)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .input("", "") { _, input -> mAdapter.addItem(input.toString()) }.show()
        }
    }

    override fun onPause() {
        mAdapter.save()
        super.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}