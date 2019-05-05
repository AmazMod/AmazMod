package com.edotassi.amazmod.adapters;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.CommandHistoryEntity;
import com.edotassi.amazmod.support.CommandHistoryBridge;

import org.tinylog.Logger;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CommandHistoryAdapter extends ArrayAdapter<CommandHistoryEntity> {

    private CommandHistoryBridge bridge;

    public CommandHistoryAdapter(CommandHistoryBridge bridge, int resource, @NonNull List<CommandHistoryEntity> objects) {
        super(bridge.getBridgeContext(), resource, objects);

        this.bridge = bridge;
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(getContext()).inflate(R.layout.row_commands, parent, false);
        }

        final CommandHistoryEntity command = getItem(position);

        ViewHolder viewHolder = new ViewHolder(command);
        ButterKnife.bind(viewHolder, listItem);

        viewHolder.value.setText(command.getCommand());

        final View lItem = listItem;

        viewHolder.value.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string = command.getCommand();
                Logger.debug("CommandHistoryActivity onClick string: " + string);
                //edit(command, lItem);
                int resultCode = 0;
                Intent resultIntent = new Intent();
                resultIntent.putExtra("COMMAND", string);
                bridge.setResult(resultCode, resultIntent);
                bridge.finish();
            }
        });

        return listItem;
    }

    public class ViewHolder {

        @BindView(R.id.row_commands_value)
        TextView value;

        private CommandHistoryEntity command;

        protected ViewHolder(CommandHistoryEntity command) {
            this.command = command;
        }

        @OnClick(R.id.row_commands_delete)
        public void onDeleteClick() {
            bridge.deleteCommand(command);
        }
    }
}