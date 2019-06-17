package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.CommandHistoryAdapter;
import com.edotassi.amazmod.db.model.CommandHistoryEntity;
import com.edotassi.amazmod.db.model.CommandHistoryEntity_Table;
import com.edotassi.amazmod.support.CommandHistoryBridge;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CommandHistoryActivity extends BaseAppCompatActivity implements CommandHistoryBridge {

    @BindView(R.id.activity_command_history_list)
    ListView listView;

    private CommandHistoryAdapter commandHistoryAdapter;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_history);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.command_history);

        ButterKnife.bind(this);

        commandHistoryAdapter = new CommandHistoryAdapter(this, R.layout.row_commands, new ArrayList<CommandHistoryEntity>());
        listView.setAdapter(commandHistoryAdapter);

        loadCommandHistory();
    }

    /*:
    @TODO kept code here so a command can be corrected and run again
    public void edit(final Reply command, final View v) {
        new MaterialDialog.Builder(this)
                .title(R.string.edit_reply)
                .content(R.string.enter_the_text_you_want_as_an_answer)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("", command.getValue(), new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                        command.setValue(input.toString());

                        commandHistoryAdapter.clear();
                        commandHistoryAdapter.addAll(commandHistoryValues);
                        commandHistoryAdapter.notifyDataSetChanged();

                        v.setBackgroundColor(Color.TRANSPARENT);
                    }
                }).show();
    }*/

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
