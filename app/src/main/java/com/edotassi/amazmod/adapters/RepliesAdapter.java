package com.edotassi.amazmod.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.support.Reply;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RepliesAdapter extends ArrayAdapter<Reply> {

    private Bridge bridge;

    public RepliesAdapter(Bridge bridge, int resource, @NonNull List<Reply> objects) {
        super(bridge.getContext(), resource, objects);

        this.bridge = bridge;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(getContext()).inflate(R.layout.row_replies, parent, false);
        }

        Reply reply = getItem(position);

        ViewHolder viewHolder = new ViewHolder(bridge, reply);
        ButterKnife.bind(viewHolder, listItem);

        viewHolder.value.setText(reply.getValue());

        return listItem;
    }

    static class ViewHolder {

        @BindView(R.id.row_replies_value)
        TextView value;

        private Bridge bridge;
        private Reply reply;

        protected ViewHolder(Bridge bridge, Reply reply) {
            this.bridge = bridge;
            this.reply = reply;
        }

        @OnClick(R.id.row_replies_delete)
        public void onDeleteClick() {
            bridge.onDeleteReply(reply);
        }
    }

    public interface Bridge {
        void onDeleteReply(Reply reply);

        Context getContext();
    }
}
