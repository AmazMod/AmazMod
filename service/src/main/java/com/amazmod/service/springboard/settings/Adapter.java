package com.amazmod.service.springboard.settings;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.amazmod.service.helper.ItemTouchHelperAdapter;
import com.amazmod.service.helper.ItemTouchHelperViewHolder;
import com.amazmod.service.R;

import java.util.Collections;
import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> implements ItemTouchHelperAdapter {

    private final Context context;
    private final List<BaseSetting> settings;
    private ChangeListener changeListener;

    public Adapter(Context context, List<BaseSetting> settings, ChangeListener changeListener) {
        this.context = context;
        this.settings = settings;
        this.changeListener = changeListener;
    }

    public interface ChangeListener {
        void onChange();
    }

    @Override
    public Adapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //Three layouts possible - Header, icon and switch
        if (viewType == 0) {
            //Header
            return new ViewHolder(layoutInflater.inflate(R.layout.springboard_item_header, parent, false));
        } else if (viewType == 1) {
            //Icon Item
            return new ViewHolder(layoutInflater.inflate(R.layout.springboard_item_preference_icon, parent, false));
        } else if (viewType == 2) {
            //Switch Item
            return new ViewHolder(layoutInflater.inflate(R.layout.springboard_item_preference_switch, parent, false));
        } else if (viewType == 3) {
            //Text Item
            return new ViewHolder(layoutInflater.inflate(R.layout.springboard_item_preference_text, parent, false));
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        //Return the type of a given item
        BaseSetting setting = settings.get(position);
        if (setting instanceof HeaderSetting) return 0;
        if (setting instanceof IconSetting) return 1;
        if (setting instanceof SwitchSetting) return 2;
        else return 3;
    }


    @Override
    public void onBindViewHolder(final Adapter.ViewHolder holder, int position) {
        //Get base setting for position
        BaseSetting setting = settings.get(position);
        if (setting instanceof HeaderSetting) {
            //Header, just set text
            holder.title.setText(((HeaderSetting) setting).title);
        } else if (setting instanceof SwitchSetting) {
            //Switch, setup the change listener and click listener for the root view
            SwitchSetting switchSetting = (SwitchSetting) setting;
            holder.sw.setOnCheckedChangeListener(switchSetting.changeListener);
            holder.root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.sw.toggle();
                }
            });
            //Set default check
            holder.sw.setChecked(switchSetting.isChecked);
            //Setup title
            holder.title.setText(switchSetting.title);
            //Setup subtitle if required
            if (switchSetting.subtitle != null) {
                holder.subtitle.setText(switchSetting.subtitle);
                holder.subtitle.setVisibility(View.VISIBLE);
            } else {
                holder.subtitle.setText("");
                holder.subtitle.setVisibility(View.GONE);
            }
        } else if(setting instanceof TextSetting) {
            //TextSetting, just set content
            TextSetting textSetting = (TextSetting) setting;
            holder.subtitle.setText(textSetting.text);
            //And the click listener
            holder.root.setOnClickListener(textSetting.onClickListener);
        } else {
            //Icon, setup icon, click listener and title
            IconSetting iconSetting = (IconSetting) setting;
            holder.icon.setImageDrawable(iconSetting.icon);
            holder.root.setOnClickListener(iconSetting.onClickListener);
            holder.title.setText(iconSetting.title);
            //Setup subtitle if required
            if (iconSetting.subtitle != null) {
                holder.subtitle.setText(iconSetting.subtitle);
                holder.subtitle.setVisibility(View.VISIBLE);
            } else {
                holder.subtitle.setText("");
                holder.subtitle.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return settings.size();
    }

    @Override
    public void onItemDismiss(int position) {
        settings.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(settings, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        if (changeListener != null) changeListener.onChange();
        return true;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        View root;
        TextView title, subtitle;
        ImageView icon;
        Switch sw;

        public ViewHolder(View itemView) {
            super(itemView);
            //Set views
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
            icon = itemView.findViewById(R.id.icon);
            sw = itemView.findViewById(R.id.sw);
            root = itemView;
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }
    }
}