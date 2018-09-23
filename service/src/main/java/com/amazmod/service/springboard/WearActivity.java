package com.amazmod.service.springboard;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.R;

import java.util.ArrayList;
import java.util.List;

public class WearActivity extends Activity implements WearableListView.ClickListener,
        DelayedConfirmationView.DelayedConfirmationListener{

    private View mainLayout, confirmView;
    private ViewGroup viewGroup;
	private WearableListView listView;
	private String[] mItems = {"Wi-Fi", "Enable L.P.M.", "Set Device Owner", "Reboot", "Enter Fastboot"};
	private int[] mImages = {R.drawable.ic_action_locate, R.drawable.ic_action_star,
			R.drawable.ic_action_done, R.drawable.ic_action_refresh, R.drawable.ic_action_select_all};
	private TextView mHeader,textView1, textView2;

	private boolean confirmed = false;

    private DelayedConfirmationView delayedConfirmationView;
    private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_wear);

        mContext = this;
        mainLayout = findViewById(R.id.main_layout);
        confirmView = findViewById(R.id.confirm_layout);
		mHeader = findViewById(R.id.header);
		listView = findViewById(R.id.list);
        textView1 = findViewById(R.id.confirm_text);
        textView2 = findViewById(R.id.cancel_text);

        hideConfirm();
        delayedConfirmationView = findViewById(R.id.delayedView);
        delayedConfirmationView.setTotalTimeMs(3000);

        textView1.setText("Proceeding in 3s…");
        textView2.setText("Tap button to cancel");
		loadAdapter("AmazMod");

	}

	private void loadAdapter(String s) {
	    mHeader.setText(s);
		List<MenuItems> items = new ArrayList<>();
		for (int i=0; i<mItems.length; i++){
			items.add(new MenuItems(mImages[i], mItems[i]));
		}

		CustomListAdapter mAdapter = new CustomListAdapter(this, items);

		listView.setAdapter(mAdapter);
		listView.addOnScrollListener(mOnScrollListener);
		listView.setClickListener(this);
	}

	@Override
    protected void onResume() {
	    super.onResume();
    }

	@Override
    public void onClick(WearableListView.ViewHolder viewHolder) {

        switch (viewHolder.getPosition()) {
            case 0:
                beginCountdown();
                if (confirmed) {
                    confirmed = false;
                }
                break;
            case 1:
                beginCountdown();
                if (confirmed) {
                    confirmed = false;
                }
                break;

            default:
                Toast.makeText(this,
                        String.format("You selected item #%s",
                                viewHolder.getPosition()),
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

	@Override
	public void onTopEmptyRegionClick() {
		//Prevent NullPointerException
		Toast.makeText(this,
				"Top empty area tapped", Toast.LENGTH_SHORT).show();
	}

	// The following code ensures that the title scrolls as the user scrolls up
	// or down the list
	private WearableListView.OnScrollListener mOnScrollListener =
			new WearableListView.OnScrollListener() {
				@Override
				public void onAbsoluteScrollChange(int i) {
					// Only scroll the title up from its original base position
					// and not down.
					if (i > 0) {
						mHeader.setY(-i);
					}
				}

				@Override
				public void onScroll(int i) {
					// Placeholder
				}

				@Override
				public void onScrollStateChanged(int i) {
					// Placeholder
				}

				@Override
				public void onCentralPositionChanged(int i) {
					// Placeholder
				}
			};

    /**
     * Starts the DelayedConfirmationView when user presses "Start Timer" button.
     */
    public void beginCountdown() {
        //button.setVisibility(View.GONE);
        showConfirm();
        confirmed = false;
        delayedConfirmationView.setPressed(false);
        delayedConfirmationView.start();
        delayedConfirmationView.setListener(this);
        System.out.println("AmazMod WearActivity beginCountdown v.isPressed: " + delayedConfirmationView.toString() +
                " / " + delayedConfirmationView.isPressed());
    }

    @Override
    public void onTimerSelected(View v) {
        v.setPressed(true);
        delayedConfirmationView.reset();
        // Prevent onTimerFinished from being heard.
        ((DelayedConfirmationView) v).setListener(null);
        hideConfirm();
        System.out.println("AmazMod WearActivity onTimerSelected v.isPressed: " + v.toString() + " / " + v.isPressed());
    }

    @Override
    public void onTimerFinished(View v) {
        System.out.println("AmazMod WearActivity onTimerFinished v.isPressed: " + v.toString() + " / " + v.isPressed());
        //if (!v.isPressed()) {
        //}
        //v.setPressed(false);
        //delayedConfirmationView.reset();
        confirmed = true;
        ((DelayedConfirmationView) v).setListener(null);
        SystemClock.sleep(1000);
        hideConfirm();
    }

    /*
    public void beginCountdown(View view) {
        button.setVisibility(View.GONE);
        delayedView.setVisibility(View.VISIBLE);
        delayedView.setListener(new DelayedConfirmationView.DelayedConfirmationListener() {
            @Override
            public void onTimerFinished(View view) {
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
                showOnlyButton();
            }

            @Override
            public void onTimerSelected(View view) {
                showOnlyButton();
                finish();
            }
        });
        delayedView.start();
    } */

    public void hideConfirm() {
        //confirmView.getAnimation().setFillAfter(false);
        confirmView.setVisibility(View.GONE);
        mHeader.setVisibility(View.VISIBLE);
        listView.setVisibility(View.VISIBLE);
        confirmView.setClickable(false);
        confirmView.clearAnimation();
        listView.requestFocus();
        listView.setClickable(true);
    }

    public void showConfirm() {
        //listView.getAnimation().setFillAfter(false);
        listView.setVisibility(View.GONE);
        mHeader.setVisibility(View.GONE);
        confirmView.setVisibility(View.VISIBLE);
        listView.setClickable(false);
        listView.clearAnimation();
        confirmView.requestFocus();
        confirmView.setClickable(true);
    }
}