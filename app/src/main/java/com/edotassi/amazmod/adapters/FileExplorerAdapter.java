package com.edotassi.amazmod.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.edotassi.amazmod.R;

import java.util.List;

import amazmod.com.transport.data.FileData;
import butterknife.BindView;
import butterknife.ButterKnife;

public class FileExplorerAdapter extends ArrayAdapter<FileData> {

    private Drawable folder;
    private Drawable file;

    public FileExplorerAdapter(Context context, int resource, @NonNull List<FileData> list) {
        super(context, resource, list);

        folder = context.getDrawable(R.drawable.outline_folder_black_36);
        file = context.getDrawable(R.drawable.outline_insert_drive_file_black_36);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(getContext()).inflate(R.layout.row_file_explorer, parent, false);
        }

        FileData fileData = getItem(position);

        ViewHolder viewHolder = new ViewHolder();
        ButterKnife.bind(viewHolder, listItem);

        viewHolder.fileName.setText(fileData.getName());
        viewHolder.icon.setImageDrawable(fileData.isDirectory() ? folder : file);

        return listItem;
    }

    static class ViewHolder {

        @BindView(R.id.row_file_explorer_file_name)
        TextView fileName;

        @BindView(R.id.row_file_explorer_icon)
        ImageView icon;
    }
}
