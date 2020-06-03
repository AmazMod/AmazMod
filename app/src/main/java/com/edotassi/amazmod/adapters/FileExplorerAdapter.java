package com.edotassi.amazmod.adapters;

import android.graphics.drawable.Drawable;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.databinding.RowFileExplorerBinding;
import com.edotassi.amazmod.ui.FileExplorerActivity;

import java.util.List;

import amazmod.com.transport.data.FileData;

public class FileExplorerAdapter extends ArrayAdapter<FileData> {
    private RowFileExplorerBinding binding;
    private Drawable folder;
    private Drawable file;
    private Drawable apk;
    private Drawable image;
    private Drawable watchface;
    private FileExplorerActivity mActivity;

    public FileExplorerAdapter(FileExplorerActivity activity, int resource, @NonNull List<FileData> list) {
        super(activity, resource, list);
        mActivity = activity;
        folder = activity.getDrawable(R.drawable.outline_folder);
        file = activity.getDrawable(R.drawable.outline_insert_drive_file);
        apk = activity.getDrawable(R.drawable.outline_app);
        image = activity.getDrawable(R.drawable.outline_photo);
        watchface = activity.getDrawable(R.drawable.outline_watch);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        binding = RowFileExplorerBinding.inflate(mActivity.getLayoutInflater());
        View listItem = convertView;
        if (listItem == null) {
            listItem = binding.getRoot();
        }
        FileData fileData = getItem(position);
        ViewHolder viewHolder = new ViewHolder(mActivity, folder, file, apk, image, watchface);
        viewHolder.sync(fileData);
        return listItem;
    }

    static class ViewHolder {
        TextView fileName;
        TextView size;
        ImageView icon;
        private FileExplorerActivity context;
        private Drawable folder;
        private Drawable file;
        private Drawable apk;
        private Drawable image;
        private Drawable watchface;

        private ViewHolder(FileExplorerActivity mActivity, Drawable folder, Drawable file, Drawable apk, Drawable image, Drawable watchface) {
            this.context = mActivity;
            this.folder = folder;
            this.file = file;
            this.apk = apk;
            this.image = image;
            this.watchface = watchface;
            fileName = mActivity.findViewById(R.id.row_file_explorer_file_name);
            size = mActivity.findViewById(R.id.row_file_explorer_size);
            icon = mActivity.findViewById(R.id.row_file_explorer_icon);
        }

        void sync(FileData fileData) {
            fileName.setText(fileData.getName());
            //icon.setImageDrawable(fileData.isDirectory() ? folder : file);

            if (fileData.isDirectory()) {
                icon.setImageDrawable(folder);
            } else if (fileData.getExtention().equals("apk")) {
                icon.setImageDrawable(apk);
            } else if (fileData.getExtention().equals("wfz")) {
                icon.setImageDrawable(watchface);
            } else if (fileData.getExtention().toLowerCase().matches("png|jpeg|jpg|gif|tiff")) {
                icon.setImageDrawable(image);
            } else {
                icon.setImageDrawable(file);
            }
            if (!fileData.isDirectory()) {
                size.setVisibility(View.VISIBLE);
                size.setText(Formatter.formatShortFileSize(context, fileData.getSize()));
            } else {
                size.setVisibility(View.GONE);
            }
        }
    }
}
