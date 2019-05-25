package com.qiscus.mychatui.ui.groupchatcreation.groupinfo;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.data.model.User;
import com.qiscus.mychatui.ui.adapter.OnItemClickListener;
import com.qiscus.nirmana.Nirmana;

/**
 * Created on : May 17, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class ContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private final ImageView imgRemoveParticipant;
    private TextView itemName;
    private ImageView picture;
    private boolean isRemovedParticipant;

    private OnItemClickListener onItemClickListener;

    public ContactViewHolder(View itemView, OnItemClickListener onItemClickListener) {
        super(itemView);
        this.onItemClickListener = onItemClickListener;
        itemView.setOnClickListener(this);

        itemName = itemView.findViewById(R.id.name);
        picture = itemView.findViewById(R.id.avatar);
        imgRemoveParticipant = itemView.findViewById(R.id.img_remove_contact);
    }

    public void bind(User user) {
        Nirmana.getInstance().get()
                .setDefaultRequestOptions(new RequestOptions()
                        .placeholder(R.drawable.ic_qiscus_add_image)
                        .error(R.drawable.ic_qiscus_add_image)
                        .dontAnimate())
                .load(user.getAvatarUrl())
                .into(picture);

        if (isRemovedParticipant) {
            imgRemoveParticipant.setVisibility(View.VISIBLE);
        } else {
            imgRemoveParticipant.setVisibility(View.GONE);
        }

        itemName.setText(user.getName());
    }

    @Override
    public void onClick(View v) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(getAdapterPosition());
        }
    }

    public void needRemoveParticipant(boolean isRemoved) {
        this.isRemovedParticipant = isRemoved;
    }
}
