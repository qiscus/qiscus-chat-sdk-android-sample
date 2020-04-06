package com.qiscus.mychatui.ui.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.R;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.data.model.QParticipant;

import java.util.List;

/**
 * Created on : Maret 29, 2019
 * Author     : arief nur putranto
 * Name       : arief
 */
public class ParticipantsRoomInfoAdapter extends SortedRecyclerViewAdapter<QParticipant, ParticipantsRoomInfoAdapter.VHh> {

    private Context context;
    private OnItemClickListener onItemClickListener;

    public ParticipantsRoomInfoAdapter(Context context) {
        this.context = context;
    }

    @Override
    protected Class<QParticipant> getItemClass() {
        return QParticipant.class;
    }

    @Override
    protected int compare(QParticipant item1, QParticipant item2) {
        return item2.getId().compareTo(item1.getId());
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

    public void addOrUpdate(List<QParticipant> participants) {
        for (QParticipant participant : participants) {
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

        void bind(QParticipant participant) {
            Nirmana.getInstance().get()
                    .setDefaultRequestOptions(new RequestOptions()
                            .placeholder(R.drawable.ic_qiscus_avatar)
                            .error(R.drawable.ic_qiscus_avatar)
                            .dontAnimate())
                    .load(participant.getAvatarUrl())
                    .into(avatar);
            name.setText(participant.getName());

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
