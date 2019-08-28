package com.qiscus.mychatui.ui.groupchatcreation.groupinfo;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.qiscus.mychatui.R;
import com.qiscus.mychatui.data.model.User;
import com.qiscus.mychatui.ui.adapter.OnItemClickListener;
import com.qiscus.mychatui.ui.adapter.SortedRecyclerViewAdapter;


/**
 * Created on : May 17, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class ContactAdapter extends SortedRecyclerViewAdapter<User, ContactViewHolder> {
    private Context context;
    private OnItemClickListener onItemClickListener;
    private boolean isRemoved;

    public ContactAdapter(Context context, OnItemClickListener onItemClickListener) {
        super();
        this.context = context;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    protected Class<User> getItemClass() {
        return User.class;
    }

    @Override
    protected int compare(User item1, User item2) {
        return item1.getName().compareTo(item2.getName());
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ContactViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false), onItemClickListener
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        holder.bind(getData().get(position));
        holder.needRemoveParticipant(isRemoved);
    }

    public void needRemoveParticipant(boolean isRemoved) {
       this.isRemoved = isRemoved;
    }
}
