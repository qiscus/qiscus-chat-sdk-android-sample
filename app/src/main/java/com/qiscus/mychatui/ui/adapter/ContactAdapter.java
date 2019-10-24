package com.qiscus.mychatui.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.data.model.User;
import com.qiscus.nirmana.Nirmana;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class ContactAdapter extends SortedRecyclerViewAdapter<User, ContactAdapter.VH> {

    private Context context;
    private OnItemClickListener onItemClickListener;

    public ContactAdapter(Context context) {
        this.context = context;
    }

    @Override
    protected Class<User> getItemClass() {
        return User.class;
    }

    @Override
    protected int compare(User item1, User item2) {
        return item1.compareTo(item2);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
        return new VH(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.bind(getData().get(position));
    }

    public void addOrUpdate(List<User> contacts) {
        for (User contact : contacts) {
            int index = findPosition(contact);
            if (index == -1) {
                getData().add(contact);
            } else {
                getData().updateItemAt(index, contact);
            }
        }
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    static class VH extends RecyclerView.ViewHolder implements View.OnClickListener {
        private CircleImageView avatar;
        private TextView name;
        private OnItemClickListener onItemClickListener;

        VH(View itemView, OnItemClickListener onItemClickListener) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.name);
            this.onItemClickListener = onItemClickListener;

            itemView.setOnClickListener(this);
        }

        void bind(User user) {
            Nirmana.getInstance().get()
                    .setDefaultRequestOptions(new RequestOptions()
                            .placeholder(R.drawable.ic_qiscus_avatar)
                            .error(R.drawable.ic_qiscus_avatar)
                            .dontAnimate())
                    .load(user.getAvatarUrl())
                    .into(avatar);
            name.setText(user.getName());
        }

        @Override
        public void onClick(View v) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(getAdapterPosition());
            }
        }
    }
}
