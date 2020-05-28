package com.edotassi.amazmod.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.edotassi.amazmod.R
import com.edotassi.amazmod.db.model.CommandHistoryEntity
import com.edotassi.amazmod.ui.CommandHistoryActivity
import org.tinylog.kotlin.Logger

class CommandHistoryAdapter(private val mActivity: CommandHistoryActivity, private val commandList: List<CommandHistoryEntity>) : RecyclerView.Adapter<CommandHistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mActivity).inflate(R.layout.row_commands, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cmd = commandList[position]
        holder.setCommand(cmd)
    }

    override fun getItemCount(): Int {
        return commandList.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private lateinit var command: CommandHistoryEntity

        var value: TextView = view.findViewById(R.id.row_commands_value)
        var deleteButton: ImageButton = view.findViewById(R.id.row_commands_delete)

        init {
            deleteButton.setOnClickListener {
                mActivity.deleteCommand(command)
            }

            value.setOnClickListener {
                Logger.debug("CommandHistoryActivity onClick string: ${command.command}")
                val resultCode = 0
                val resultIntent = Intent()
                resultIntent.putExtra("COMMAND", command.command)
                mActivity.setResult(resultCode, resultIntent)
                mActivity.finish()
            }
        }

        fun setCommand(command: CommandHistoryEntity) {
            this.command = command
            value.text = command.command
        }
    }
}