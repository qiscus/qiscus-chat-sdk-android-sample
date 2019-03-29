package com.qiscus.mychatui.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.R;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.data.model.QiscusRoomMember;

import java.util.List;

/**
 * Created on : Maret 29, 2019
 * Author     : arief nur putranto
 * Name       : arief
 */
public class ParticipantsRoomInfoAdapter extends SortedRecyclerViewAdapter<QiscusRoomMember, ParticipantsRoomInfoAdapter.VHh> {

    private Context context;
    private OnItemClickListener onItemClickListener;

    public ParticipantsRoomInfoAdapter(Context context) {
        this.context = context;
    }

    @Override
    protected Class<QiscusRoomMember> getItemClass() {
        return QiscusRoomMember.class;
    }

    @Override
    protected int compare(QiscusRoomMember item1, QiscusRoomMember item2) {
        return item2.getEmail().compareTo(item1.getEmail());
    }

    @Override
    public VHh onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_participant_room_info, parent, false);
        return new VHh(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(VHh holder, int position) {
        holder.bind(getData().get(position));
    }

    public void addOrUpdate(List<QiscusRoomMember> participants) {
        for (QiscusRoomMember participant : participants) {
            int index = findPosition(participant);
            if (index == -1) {
                getData().add(participant);
            } else {
                getData().updateItemAt(index, participant);
            }
        }
        notifyDataSetChanged();
    }

    public void remove(int position){
        getData().remove(getData().get(position));
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public static class VHh extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView avatar,ivRemove;
        private TextView name;
        private OnItemClickListener onItemClickListener;

        VHh(View itemView, OnItemClickListener onItemClickListener) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.name);
            ivRemove = itemView.findViewById(R.id.ivRemove);

            this.onItemClickListener = onItemClickListener;

            itemView.setOnClickListener(this);
        }

        void bind(QiscusRoomMember participant) {
            Nirmana.getInstance().get()
                    .setDefaultRequestOptions(new RequestOptions()
                            .placeholder(R.drawable.ic_qiscus_avatar)
                            .error(R.drawable.ic_qiscus_avatar)
                            .dontAnimate())
                    .load(participant.getAvatar())
                    .into(avatar);
            name.setText(participant.getUsername());

            ivRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(getAdapterPosition());
                    }
                }
            });

        }

        @Override
        public void onClick(View v) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(getAdapterPosition());
            }
        }
    }
}
