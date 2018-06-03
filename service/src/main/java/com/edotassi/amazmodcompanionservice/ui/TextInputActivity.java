package com.edotassi.amazmodcompanionservice.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.edotassi.amazmodcompanionservice.R;
import com.edotassi.amazmodcompanionservice.R2;
import com.edotassi.amazmodcompanionservice.keyboard.KeyboardCell;
import com.edotassi.amazmodcompanionservice.keyboard.KeyboardRow;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TextInputActivity extends Activity {

    @BindView(R2.id.activity_text_input_tablelayout)
    LinearLayout tableLayout;

    @BindView(R2.id.activity_inputtext_text)
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textinput);

        ButterKnife.bind(this);

        try {
            List<KeyboardRow> keyboardRows = getKeyboardConfiguration();
            populateKeyboard(keyboardRows);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        findViewById(R.id.activity_textinput_root).dispatchTouchEvent(event);
        return false;
    }

    private void populateKeyboard(List<KeyboardRow> keyboardRows) {
        int density = getResources().getDisplayMetrics().densityDpi;

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams((int) (density * 0.175), (int) (density * 0.190));
        buttonParams.setMargins(-4, -2, -4, -2);

        for (KeyboardRow keyboardRow : keyboardRows) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setGravity(Gravity.CENTER);

            for (KeyboardCell keyboardCell : keyboardRow.getCells()) {
                final Button button = new Button(this);
                button.setPadding(0, 0, 0, 0);
                //button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                button.setText(keyboardCell.getText());
                button.setLayoutParams(buttonParams);

                if (keyboardCell.getName().equals("space")) {
                    LinearLayout.LayoutParams buttonSpaceParams = new LinearLayout.LayoutParams((int) (density * 0.175 * 2), (int) (density * 0.190));
                    buttonSpaceParams.setMargins(-4, -2, -4, -2);
                    button.setLayoutParams(buttonSpaceParams);
                }

                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String previousText = textView.getText() == null ? "" : textView.getText().toString();
                        String buttonText = button.getText().toString();
                        if (buttonText.equals("←") && previousText.length() > 0) {
                            textView.setText(previousText.substring(0, previousText.length() - 1));
                        } else {
                            textView.setText(previousText + buttonText);
                        }
                    }
                });

                rowLayout.addView(button);
            }


            tableLayout.addView(rowLayout);
        }



        /*
        LinearLayout.LayoutParams tableParams = new LinearLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams();
        tableRowParams.setMargins(-4, -4, -4, -4);

        for (KeyboardRow keyboardRow : keyboardRows) {
            TableRow tableRow = new TableRow(this);
            //tableRow.setLayoutParams(tableRowParams);

            for (KeyboardCell keyboardCell : keyboardRow.getCells()) {
                Button button = new Button(this);
                button.setPadding(0, 0, 0, 0);
                //button.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
                //button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                button.setText(keyboardCell.getText());
                //button.setVisibility(View.VISIBLE);
                button.setLayoutParams(tableRowParams);
                tableRow.addView(button);
            }

            tableLayout.addView(tableRow);
        }
        */
    }

    private List<KeyboardRow> getKeyboardConfiguration() throws IOException {
        String json = readKeyboardConfiguration();

        Type listType = new TypeToken<ArrayList<KeyboardRow>>() {
        }.getType();
        return new Gson().fromJson(json, listType);
    }

    private String readKeyboardConfiguration() throws IOException {
        StringBuilder buf = new StringBuilder();
        InputStream json = getAssets().open("keyboard.json");
        BufferedReader in = new BufferedReader(new InputStreamReader(json, "UTF-8"));
        String str;

        while ((str = in.readLine()) != null) {
            buf.append(str);
        }

        in.close();

        return buf.toString();
    }
}
