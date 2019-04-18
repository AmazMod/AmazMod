package com.edotassi.amazmod.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Formatter;
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
    private Drawable apk;
    private Drawable image;
    private Drawable watchface;

    public FileExplorerAdapter(Context context, int resource, @NonNull List<FileData> list) {
        super(context, resource, list);

        folder = context.getDrawable(R.drawable.outline_folder_black_36);
        file = context.getDrawable(R.drawable.outline_insert_drive_file_black_36);
        apk = context.getDrawable(R.drawable.outline_app);
        image = context.getDrawable(R.drawable.outline_photo);
        watchface = context.getDrawable(R.drawable.outline_watch);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(getContext()).inflate(R.layout.row_file_explorer, parent, false);
        }

        FileData fileData = getItem(position);

        ViewHolder viewHolder = new ViewHolder(getContext(), folder, file, apk, image, watchface);
        ButterKnife.bind(viewHolder, listItem);
        viewHolder.sync(fileData);

        return listItem;
    }

    static class ViewHolder {

        @BindView(R.id.row_file_explorer_file_name)
        TextView fileName;

        @BindView(R.id.row_file_explorer_size)
        TextView size;

        @BindView(R.id.row_file_explorer_icon)
        ImageView icon;

        private Context context;
        private Drawable folder;
        private Drawable file;
        private Drawable apk;
        private Drawable image;
        private Drawable watchface;

        private ViewHolder(Context context, Drawable folder, Drawable file, Drawable apk, Drawable image, Drawable watchface) {
            this.context = context;
            this.folder = folder;
            this.file = file;
            this.apk = apk;
            this.image = image;
            this.watchface = watchface;
        }

        void sync(FileData fileData) {
            fileName.setText(fileData.getName());
            //icon.setImageDrawable(fileData.isDirectory() ? folder : file);

            if(fileData.isDirectory()){
                icon.setImageDrawable(folder);
            }else if(fileData.getExtention().equals("apk")){
                icon.setImageDrawable(apk);
            }else if(fileData.getExtention().equals("wfz")){
                icon.setImageDrawable(watchface);
            }else if(fileData.getExtention().toLowerCase().matches("png|jpeg|jpg|gif|tiff")){
                icon.setImageDrawable(image);
            }else{
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
