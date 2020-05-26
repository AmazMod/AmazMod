package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.CommandHistoryAdapter;
import com.edotassi.amazmod.databinding.ActivityCommandHistoryBinding;
import com.edotassi.amazmod.db.model.CommandHistoryEntity;
import com.edotassi.amazmod.db.model.CommandHistoryEntity_Table;
import com.edotassi.amazmod.support.CommandHistoryBridge;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.List;

public class CommandHistoryActivity extends BaseAppCompatActivity implements CommandHistoryBridge {

    private ActivityCommandHistoryBinding binding;

    private CommandHistoryAdapter commandHistoryAdapter;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommandHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.command_history);

        commandHistoryAdapter = new CommandHistoryAdapter(this, R.layout.row_commands, new ArrayList<CommandHistoryEntity>());
        binding.activityCommandHistoryList.setAdapter(commandHistoryAdapter);

        loadCommandHistory();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_command_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.activity_command_history_delete_all) {

            new MaterialDialog.Builder(this)
                    .title(R.string.are_you_sure)
                    .content(R.string.cannot_be_undone)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Delete.table(CommandHistoryEntity.class);
                            loadCommandHistory();
                        }
                    }).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void deleteCommand(CommandHistoryEntity command) {
        SQLite
                .delete()
                .from(CommandHistoryEntity.class)
                .where(CommandHistoryEntity_Table.id.eq(command.getId()))
                .query();

        loadCommandHistory();
    }

    private void loadCommandHistory() {
        List<CommandHistoryEntity> commandHistoryValues = SQLite
                .select()
                .from(CommandHistoryEntity.class)
                .orderBy(CommandHistoryEntity_Table.date.desc())
                .queryList();

        commandHistoryAdapter.clear();
        commandHistoryAdapter.addAll(commandHistoryValues);
        commandHistoryAdapter.notifyDataSetChanged();
    }

    @Override
    public Context getBridgeContext() {
        return this;
    }
}
