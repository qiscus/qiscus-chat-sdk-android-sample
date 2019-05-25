package com.qiscus.mychatui.ui.adapter;

import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public abstract class SortedRecyclerViewAdapter<Item, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private CommentsAdapter.RecyclerViewItemClickListener recyclerViewItemClickListener;

    private SortedList<Item> data = new SortedList<>(getItemClass(), new SortedList.Callback<Item>() {
        @Override
        public int compare(Item o1, Item o2) {
            return SortedRecyclerViewAdapter.this.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            SortedRecyclerViewAdapter.this.onChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Item oldItem, Item newItem) {
            return SortedRecyclerViewAdapter.this.areContentsTheSame(oldItem, newItem);
        }

        @Override
        public boolean areItemsTheSame(Item item1, Item item2) {
            return SortedRecyclerViewAdapter.this.areItemsTheSame(item1, item2);
        }

        @Override
        public void onInserted(int position, int count) {
            SortedRecyclerViewAdapter.this.onInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            SortedRecyclerViewAdapter.this.onRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            SortedRecyclerViewAdapter.this.onMoved(fromPosition, toPosition);
        }
    });

    protected abstract Class<Item> getItemClass();

    protected abstract int compare(Item item1, Item item2);

    protected void onChanged(int position, int count) {

    }

    protected boolean areContentsTheSame(Item oldItem, Item newItem) {
        return oldItem.equals(newItem);
    }

    protected boolean areItemsTheSame(Item item1, Item item2) {
        return item1.equals(item2);
    }

    protected void onInserted(int position, int count) {

    }

    protected void onRemoved(int position, int count) {

    }

    protected void onMoved(int fromPosition, int toPosition) {

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public SortedList<Item> getData() {
        return data;
    }

    public int findPosition(Item item) {
        if (data == null) {
            return -1;
        }

        int size = data.size() - 1;
        for (int i = size; i >= 0; i--) {
            if (data.get(i).equals(item)) {
                return i;
            }
        }

        return -1;
    }

    public int add(Item item) {
        int i = data.add(item);
        notifyItemInserted(i);
        return i;
    }

    public void add(final List<Item> items) {
        data.addAll(items);
        notifyDataSetChanged();
    }

    public void addOrUpdate(Item item) {
        int i = findPosition(item);
        if (i >= 0) {
            data.updateItemAt(i, item);
            notifyDataSetChanged();
        } else {
            add(item);
        }
    }

    public void addOrUpdate(final List<Item> items) {
        for (Item item : items) {
            int i = findPosition(item);
            if (i >= 0) {
                data.updateItemAt(i, item);
            } else {
                data.add(item);
            }
        }
        notifyDataSetChanged();
    }

    public void remove(int position) {
        if (position >= 0 && position < data.size()) {
            data.removeItemAt(position);
            notifyItemRemoved(position);
        }
    }

    public void remove(Item item) {
        int position = findPosition(item);
        remove(position);
    }

    //Set method of OnItemClickListener object
    public void setOnItemClickListener(CommentsAdapter.RecyclerViewItemClickListener recyclerViewItemClickListener){
        this.recyclerViewItemClickListener=recyclerViewItemClickListener;
    }

    public void setOnClickListener(View view, int position){
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //When item view is clicked, trigger the itemclicklistener
                recyclerViewItemClickListener.onItemClick(v,position);
            }
        });
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //When item view is clicked long, trigger the itemclicklistener
                recyclerViewItemClickListener.onItemLongClick(v,position);
                return true;
            }
        });
    }

    public void clear() {
        data.clear();
    }


}
