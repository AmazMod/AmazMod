package com.edotassi.amazmod.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Displays a list of items that can be either checked or not. Also color can be set per-item basis.
 */
public class CheckableAdapter extends BaseAdapter {

    private final List<? extends Item> mValues;

    /**
     * Represents single non-checkable item by its name and color.
     */
    public static class Item {
        final String name;
        final int color;

        public Item(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }

    /**
     * Represents single checkable item. Note that client can make own implementation of Checkable
     * interface or extend this {@link CheckableItem}.
     */
    public static abstract class CheckableItem extends Item implements Checkable {

        public CheckableItem(String name, int color) {
            super(name, color);
        }

        @Override
        public void toggle() {
            setChecked(!isChecked());
        }
    }

    public CheckableAdapter(@NonNull List<? extends Item> values) {
        mValues = values;
    }

    @Override
    public int getCount() {
        return mValues.size();
    }

    @Override
    public Item getItem(int position) {
        return mValues.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        final Item item = mValues.get(position);
        boolean checkable = (item instanceof Checkable);

        View itemView = convertView != null ? convertView.findViewById(android.R.id.text1) : convertView;

        if (itemView == null || (itemView instanceof Checkable) != checkable) {
            int id = checkable ? android.R.layout.simple_list_item_multiple_choice :
                    android.R.layout.simple_list_item_1;
            convertView = LayoutInflater.from(context).inflate(id, parent, false);
            itemView = convertView.findViewById(android.R.id.text1);

            if (!checkable) {
                ((TextView) itemView).setGravity(Gravity.END);
            }
        }

        if (checkable) {
            itemView.setOnClickListener(view -> {
                boolean checked = !((Checkable) view).isChecked();

                ((Checkable) view).setChecked(checked);
                ((Checkable) item).setChecked(checked);
            });

            ((Checkable) itemView).setChecked(((Checkable) item).isChecked());
        } else {
            itemView.setOnClickListener(null);
        }

        ((TextView) itemView).setText(item.name);
        ((TextView) itemView).setTextColor(item.color);

        return convertView;
    }
}
