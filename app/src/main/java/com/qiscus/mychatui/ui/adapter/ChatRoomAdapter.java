package com.qiscus.mychatui.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.util.Const;
import com.qiscus.mychatui.util.DateUtil;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.data.model.QChatRoom;
import com.qiscus.sdk.chat.core.data.model.QMessage;

import java.util.List;

/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class ChatRoomAdapter extends SortedRecyclerViewAdapter<QChatRoom, ChatRoomAdapter.VH> {

    private Context context;
    private OnItemClickListener onItemClickListener;

    public ChatRoomAdapter(Context context) {
        this.context = context;
    }

    @Override
    protected Class<QChatRoom> getItemClass() {
        return QChatRoom.class;
    }

    @Override
    protected int compare(QChatRoom item1, QChatRoom item2) {
        return item2.getLastMessage().getTimestamp().compareTo(item1.getLastMessage().getTimestamp());
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_room, parent, false);
        return new VH(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.bind(getData().get(position));
    }

    public void addOrUpdate(List<QChatRoom> chatRooms) {
        for (QChatRoom chatRoom : chatRooms) {
            int index = findPosition(chatRoom);
            if (index == -1) {
                getData().add(chatRoom);
            } else {
                getData().updateItemAt(index, chatRoom);
            }
        }
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public static class VH extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView avatar;
        private TextView name;
        private com.vanniktech.emoji.EmojiTextView lastMessage;
        private TextView tv_unread_count;
        private TextView tv_time;
        private FrameLayout layout_unread_count;
        private OnItemClickListener onItemClickListener;

        VH(View itemView, OnItemClickListener onItemClickListener) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.name);
            lastMessage = itemView.findViewById(R.id.tv_last_message);
            tv_unread_count = itemView.findViewById(R.id.tv_unread_count);
            tv_time = itemView.findViewById(R.id.tv_time);
            layout_unread_count = itemView.findViewById(R.id.layout_unread_count);


            this.onItemClickListener = onItemClickListener;

            itemView.setOnClickListener(this);
        }

        void bind(QChatRoom chatRoom) {
            Nirmana.getInstance().get()
                    .setDefaultRequestOptions(new RequestOptions()
                            .placeholder(R.drawable.ic_qiscus_avatar)
                            .error(R.drawable.ic_qiscus_avatar)
                            .dontAnimate())
                    .load(chatRoom.getAvatarUrl())
                    .into(avatar);
            name.setText(chatRoom.getName());
            QMessage lastComment = chatRoom.getLastMessage();
            if (lastComment != null && lastComment.getId() > 0) {
                if (lastComment.getSender() != null) {
                    String lastMessageText = lastComment.isMyComment(
                            Const.qiscusCore().getQiscusAccount().getId()) ?
                            "You: " : lastComment.getSender().getName().split(" ")[0] + ": ";
                    lastMessageText += chatRoom.getLastMessage().getType() == QMessage.Type.IMAGE
                            ? "\uD83D\uDCF7 send an image" : lastComment.getText();
                    lastMessage.setText(lastMessageText);
                } else {
                    String lastMessageText = "";
                    lastMessageText += chatRoom.getLastMessage().getType() == QMessage.Type.IMAGE
                            ? "\uD83D\uDCF7 send an image" : lastComment.getText();
                    lastMessage.setText(lastMessageText);
                }

                tv_time.setText(DateUtil.getLastMessageTimestamp(lastComment.getTimestamp()));
            } else {
                lastMessage.setText("");
                tv_time.setText("");
            }

            tv_unread_count.setText(String.format("%d", chatRoom.getUnreadCount()));
            if (chatRoom.getUnreadCount() == 0) {
                layout_unread_count.setVisibility(View.GONE);
            } else {
                layout_unread_count.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onClick(View v) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(getAdapterPosition());
            }
        }
    }
}
