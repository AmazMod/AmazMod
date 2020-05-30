package com.edotassi.amazmod.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.edotassi.amazmod.R
import com.edotassi.amazmod.adapters.CommandHistoryAdapter
import com.edotassi.amazmod.db.model.CommandHistoryEntity
import com.edotassi.amazmod.db.model.CommandHistoryEntity_Table
import com.edotassi.amazmod.support.CommandHistoryBridge
import com.raizlabs.android.dbflow.sql.language.Delete
import com.raizlabs.android.dbflow.sql.language.SQLite
import kotlinx.android.synthetic.main.activity_command_history.*
import java.util.*

class CommandHistoryActivity : BaseAppCompatActivity(), CommandHistoryBridge {

    private lateinit var adapter: CommandHistoryAdapter
    private lateinit var commandList: ArrayList<CommandHistoryEntity>

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setTitle(R.string.command_history)
        setContentView(R.layout.activity_command_history)
        commandList = loadCommandHistory()
        adapter = CommandHistoryAdapter(this, commandList)
        command_list.layoutManager = LinearLayoutManager(this)
        command_list.adapter = adapter
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_command_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.activity_command_history_delete_all) {
            MaterialDialog.Builder(this)
                    .title(R.string.are_you_sure)
                    .content(R.string.cannot_be_undone)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .onPositive { _, _ ->
                        Delete.table(CommandHistoryEntity::class.java)
                        loadCommandHistory()
                    }.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun deleteCommand(command: CommandHistoryEntity) {
        //Remove From Database
        SQLite
                .delete()
                .from(CommandHistoryEntity::class.java)
                .where(CommandHistoryEntity_Table.id.eq(command.id))
                .query()
        //Remove from Array and Update Adapter
        val pos = commandList.indexOf(command)
        commandList.remove(command)
        adapter.notifyItemRemoved(pos)
    }

    private fun loadCommandHistory(): ArrayList<CommandHistoryEntity> {
        return SQLite
                .select()
                .from(CommandHistoryEntity::class.java)
                .orderBy(CommandHistoryEntity_Table.date.desc())
                .queryList() as ArrayList<CommandHistoryEntity>
    }

    override fun getBridgeContext(): Context {
        return this
    }
}