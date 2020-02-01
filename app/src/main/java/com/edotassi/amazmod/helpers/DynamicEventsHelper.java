package com.edotassi.amazmod.helpers;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class DynamicEventsHelper extends ItemTouchHelper.Callback {

    private DynamicEventsCallback callback;
    private boolean canSwipe;

    public DynamicEventsHelper(DynamicEventsCallback callback) {
        this.callback = callback;
        this.canSwipe = true;
    }

    public DynamicEventsHelper(DynamicEventsCallback callback, boolean canSwipe) {
        this.callback = callback;
        this.canSwipe = canSwipe;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }





    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = 0;
        if (this.canSwipe) {
            swipeFlags = ItemTouchHelper.END | ItemTouchHelper.START;
        }
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        callback.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        callback.removeItem(viewHolder.getAdapterPosition());
    }


    public interface DynamicEventsCallback {
        void onItemMove(int initialPosition, int finalPosition);
        void removeItem(int position);
    }


}
