package com.edotassi.amazmod.adapters

import amazmod.com.models.Reply
import amazmod.com.transport.Constants
import android.annotation.SuppressLint
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.edotassi.amazmod.R
import com.edotassi.amazmod.ui.NotificationRepliesDragActivity
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pixplicity.easyprefs.library.Prefs
import org.tinylog.kotlin.Logger
import java.util.*

class NotificationRepliesAdapter(private val mActivity: NotificationRepliesDragActivity, private val androidItemTouchHelper: ItemTouchHelper?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mList: MutableList<Reply>

    init {
        mList = ArrayList()
        update()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = View.inflate(mActivity, R.layout.row_drag_replies, null)
        return ReplyViewHolder(v)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {
        if (viewHolder is ReplyViewHolder) {
            val mReply = mList[i]
            viewHolder.setReply(mReply)
            viewHolder.value.text = mReply.value
            viewHolder.handle.setOnTouchListener { _, _ ->
                androidItemTouchHelper?.startDrag(viewHolder)
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun update() {
        Thread(Runnable {
            mList = loadReplies()
            mActivity.runOnUiThread { notifyDataSetChanged() }
        }).start()
    }

    private fun loadReplies(): MutableList<Reply> {
        return try {
            var repliesJson = Prefs.getString(Constants.PREF_NOTIFICATIONS_REPLIES, Constants.PREF_DEFAULT_NOTIFICATIONS_REPLIES)
            if (repliesJson == "null") {
                repliesJson = "[]"
            }
            val listType = object : TypeToken<List<Reply>>() {}.type
            Gson().fromJson(repliesJson, listType)
        } catch (ex: Exception) {
            ArrayList()
        }
    }

    fun onItemMove(initialPosition: Int, finalPosition: Int) {
        if (initialPosition < mList.size && finalPosition < mList.size) {
            if (initialPosition < finalPosition) {
                for (i in initialPosition until finalPosition) {
                    Collections.swap(mList, i, i + 1)
                }
            } else {
                for (i in initialPosition downTo finalPosition + 1) {
                    Collections.swap(mList, i, i - 1)
                }
            }
            notifyItemMoved(initialPosition, finalPosition)
        }
        Thread(Runnable { save() }).start()
    }

    fun save() {
        Logger.debug("Saving order to preferences")
        val gson = Gson()
        var repliesJson = gson.toJson(mList)
        Prefs.putString(Constants.PREF_NOTIFICATIONS_REPLIES, repliesJson)
    }

    fun addItem(reply: String?) {
        val r = Reply()
        r.value = reply
        mList.add(r)
        notifyDataSetChanged()
    }

    fun removeItem(item: Reply?) {
        val posicao = mList.indexOf(item)
        removeItem(posicao)
    }

    fun removeItem(position: Int) {
        val item = mList[position]
        mList.removeAt(position)
        notifyItemRemoved(position)
        Snackbar.make(mActivity.findViewById(R.id.activity_notification), mActivity.getString(R.string.notification_replies_cancel_removal), 5000)
                .setAction(mActivity.getString(R.string.undo)) {
                    mList.add(position, item)
                    notifyItemInserted(position)
                }.addCallback(object : BaseCallback<Snackbar?>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        //Put here what to do if snackbar is dismissed
                    }
                }).show()
    }

    fun editItem(item: Reply?) {
        val posicao = mList.indexOf(item)
        MaterialDialog.Builder(mActivity)
                .title(R.string.edit_reply)
                .content(R.string.enter_the_text_you_want_as_an_answer)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("", item!!.value) { _: MaterialDialog?, input: CharSequence ->
                    item.value = input.toString()
                    notifyItemChanged(posicao)
                }.show()
    }

    private inner class ReplyViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var value: TextView = itemView.findViewById(R.id.row_replies_value)
        var handle: TextView = itemView.findViewById(R.id.row_handle)
        var deleteButton: ImageButton = itemView.findViewById(R.id.row_replies_delete)
        var editButton: ImageButton = itemView.findViewById(R.id.row_replies_edit)

        private var reply: Reply? = null

        fun setReply(reply: Reply?) {
            this.reply = reply
        }

        init {
            deleteButton.setOnClickListener { removeItem(reply) }
            editButton.setOnClickListener {
                val string = reply!!.value
                Logger.debug("NotificationRepliesDragActivity onLongClick string: $string")
                editItem(reply)
            }
        }
    }
}