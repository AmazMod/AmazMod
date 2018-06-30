package com.edotassi.amazmod.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.edotassi.amazmod.support.Reply;

import java.util.List;

public class RepliesAdapter extends ArrayAdapter<Reply> {

    public RepliesAdapter(@NonNull Context context, int resource, @NonNull List<Reply> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }
}
