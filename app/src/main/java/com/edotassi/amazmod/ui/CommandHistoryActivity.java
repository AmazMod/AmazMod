package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pixplicity.easyprefs.library.Prefs;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import amazmod.com.models.Command;
import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import de.mateware.snacky.Snacky;

public class CommandHistoryActivity extends AppCompatActivity {

    @BindView(R.id.activity_command_history_list)
    ListView listView;

    private List<Command> commandHistoryValues;
    private CommandsDragAdapter commandHistoryAdapter;

    private boolean mSortable = false;
    private Command mDragCommand;
    private int initalPosition;
    private int mPosition = -1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_history);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.command_history);

        ButterKnife.bind(this);

        commandHistoryAdapter = new CommandsDragAdapter(this, R.layout.row_commands, new ArrayList<Command>());
        listView.setAdapter(commandHistoryAdapter);

        loadCommandHistory();

        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!mSortable) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        int position = listView.pointToPosition((int) event.getX(), (int) event.getY());
                        //int position = itemNum - listView.getFirstVisiblePosition();
                        if (position < 0) {
                            break;
                        }
                        if (position != mPosition) {
                            System.out.println("AmazMod CommandHistoryActivity move mPosition: " + mPosition +
                                    " \\ position: " + position);
                            if (mPosition != -1) {
                                if (position > mPosition) {
                                    if (position - mPosition == 1)
                                        Collections.swap(commandHistoryValues, mPosition, position);
                                    else {
                                        Collections.swap(commandHistoryValues, mPosition, (position-1));
                                        Collections.swap(commandHistoryValues, position, (position-1));
                                    }
                                } else {
                                    if (mPosition - position == 1)
                                        Collections.swap(commandHistoryValues, mPosition, position);
                                    else {
                                        Collections.swap(commandHistoryValues, mPosition, (position+1));
                                        Collections.swap(commandHistoryValues, position, (position+1));
                                    }
                                }
                            }// else if (initalPosition != position)
                             //   Collections.swap(commandHistoryValues, initalPosition, position);
                            mPosition = position;
                            commandHistoryAdapter.remove(mDragCommand);
                            commandHistoryAdapter.insert(mDragCommand, mPosition);
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_OUTSIDE: {
                        System.out.println("AmazMod CommandHistoryActivity cancel initialPosition: " + initalPosition +
                                " \\ mPosition: " + mPosition);
                        stopDrag();
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public void startDrag(Command command) {
        mPosition = -1;
        mSortable = true;
        mDragCommand = command;
        commandHistoryAdapter.notifyDataSetChanged();
    }

    public void stopDrag() {
        mPosition = -1;
        mSortable = false;
        mDragCommand = null;
        commandHistoryAdapter.notifyDataSetChanged();
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
    protected void onPause() {
        System.out.println("AmazMod CommandHistoryActivity onPause");
        Gson gson = new Gson();
        String commandHistoryJson = gson.toJson(commandHistoryValues);
        Prefs.putString(Constants.PREF_COMMAND_HISTORY, commandHistoryJson);

        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public void onDeleteCommand(Command command) {
        commandHistoryValues.remove(command);
        commandHistoryAdapter.clear();
        commandHistoryAdapter.addAll(commandHistoryValues);
        commandHistoryAdapter.notifyDataSetChanged();
    }
/*
    @OnClick(R.id.activity_command_history_add)
    protected void onAddCommand() {
        new MaterialDialog.Builder(this)
                .title(R.string.add_new_reply)
                .content(R.string.enter_the_text_you_want_as_an_answer)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("", "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        Command command = new Command();
                        command.setValue(input.toString());
                        commandHistoryValues.add(command);

                        commandHistoryAdapter.clear();
                        commandHistoryAdapter.addAll(commandHistoryValues);
                        commandHistoryAdapter.notifyDataSetChanged();
                    }
                }).show();
    }
*/
    private void loadCommandHistory() {
        this.commandHistoryValues = new ArrayList<>();

        this.commandHistoryValues = loadCommandsFromPrefs();

        commandHistoryAdapter.clear();
        commandHistoryAdapter.addAll(commandHistoryValues);
        commandHistoryAdapter.notifyDataSetChanged();
    }

    private List<Command> loadCommandsFromPrefs() {
        try {
            String commandsJson = Prefs.getString(Constants.PREF_COMMAND_HISTORY, Constants.PREF_DEFAULT_COMMAND_HISTORY);
            Type listType = new TypeToken<List<Command>>() {
            }.getType();
            return new Gson().fromJson(commandsJson, listType);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public class CommandsDragAdapter extends ArrayAdapter<Command> {

        public CommandsDragAdapter(Context context, int resource, @NonNull List<Command> objects) {
            super(context, resource, objects);

        }

        @SuppressLint("ClickableViewAccessibility")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if (listItem == null) {
                listItem = LayoutInflater.from(getContext()).inflate(R.layout.row_commands, parent, false);
            }

            final Command command = getItem(position);

            ViewHolder viewHolder = new ViewHolder(command);
            ButterKnife.bind(viewHolder, listItem);

            viewHolder.value.setText(command.getValue());

            viewHolder.handle.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    initalPosition = listView.pointToPosition((int) view.getX(), (int) view.getY());
                    //initalPosition = listView.pointToPosition((int) event.getRawX(), (int) event.getRawY());
                    //initalPosition = listView.pointToPosition((int) event.getX(), (int) event.getY());
                    //initalPosition = itemNum - listView.getFirstVisiblePosition();
                    //if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        startDrag(command);
                        return true;
                    //}
                    //return false;
                }
            });

            final View lItem = listItem;
            /*
            viewHolder.value.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    lItem.setBackgroundColor(Color.parseColor("#99FF4081"));
                    String string = command.getValue();
                    System.out.println("AmazMod CommandHistoryActivity onLongClick string: " + string);
                    //edit(command, lItem);
                    Snacky.builder()
                            .setView(lItem)
                            //.setText(R.string.brightness_bad_value_entered)
                            .setText("Long Click not implemented")
                            .build()
                            .show();
                    return false;
                }
            });*/

            viewHolder.value.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String string = command.getValue();
                    System.out.println("AmazMod CommandHistoryActivity onClick string: " + string);
                    //edit(command, lItem);
                    int resultCode = 0;
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("COMMAND", string);
                    setResult(resultCode, resultIntent);
                    finish();
                }
            });


            if (mDragCommand != null && mDragCommand == command) {
                listItem.setBackgroundColor(Color.parseColor("#99303F9F"));
            } else {
                listItem.setBackgroundColor(Color.TRANSPARENT);
            }

            return listItem;
        }

        public class ViewHolder {

            @BindView(R.id.row_commands_value)
            TextView value;
            @BindView(R.id.row_commands_handle)
            TextView handle;

            private Command command;

            protected ViewHolder(Command command) {
                this.command = command;
            }

            @OnClick(R.id.row_commands_delete)
            public void onDeleteClick() {
                onDeleteCommand(command);
            }
        }
    }
}
