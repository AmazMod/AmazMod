package com.amazmod.service.adapters;

import android.content.Context;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.amazmod.service.R;
import com.amazmod.service.models.MenuItems;

import java.util.List;


public class MenuListAdapter extends WearableListView.Adapter {
	private final List<MenuItems> items;
	private final LayoutInflater mInflater;

    private static final float NO_ALPHA = 1.0f, PARTIAL_ALPHA = 0.80f;
    private static final float NO_SCALE = 1.0f, SCALE = 0.9f;
    private static final float NO_X_TRANSLATION = 0f, X_TRANSLATION = 20f;

	public MenuListAdapter(Context context, List<MenuItems> items) {
		mInflater = LayoutInflater.from(context);
		this.items = items;
	}

	@Override
	public @NonNull WearableListView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
		return new ItemViewHolder(mInflater.inflate(R.layout.list_item, null));
	}

	@Override
	public void onBindViewHolder(@NonNull WearableListView.ViewHolder viewHolder, int position) {
		//System.out.println("MenuListAdapter onBindViewHodler position: " + position);
		ItemViewHolder itemViewHolder = (ItemViewHolder) viewHolder;
		final MenuItems item = items.get(position);
		TextView textView = itemViewHolder.mItemTextView;
		textView.setText(item.getTitle());
		CircledImageView mImageView = itemViewHolder.mCircledImageView;
		mImageView.setImageResource(item.getIcon());
		if (position == 1) {
            mImageView.animate().scaleX(SCALE).scaleY(SCALE).translationX(X_TRANSLATION).alpha(PARTIAL_ALPHA).setDuration(50L);
            textView.animate().scaleX(SCALE).scaleY(SCALE).translationX(X_TRANSLATION).alpha(PARTIAL_ALPHA).setDuration(50L);
            //mImageView.animate().alpha(PARTIAL_ALPHA).translationX(X_TRANSLATION).start();
            //textView.animate().alpha(PARTIAL_ALPHA).translationX(X_TRANSLATION).start();
        }

	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	private static class ItemViewHolder extends WearableListView.ViewHolder {
		private TextView mItemTextView;
		private CircledImageView mCircledImageView;

		public ItemViewHolder(View itemView) {
			super(itemView);
			mItemTextView = itemView.findViewById(R.id.text);
			mCircledImageView = itemView.findViewById(R.id.image);
		}
	}
}