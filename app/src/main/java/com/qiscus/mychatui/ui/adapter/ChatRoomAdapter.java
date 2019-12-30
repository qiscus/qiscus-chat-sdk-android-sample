package com.qiscus.mychatui.ui.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.util.DateUtil;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;

import java.util.List;

/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class ChatRoomAdapter extends SortedRecyclerViewAdapter<QiscusChatRoom, ChatRoomAdapter.VH> {

    private Context context;
    private OnItemClickListener onItemClickListener;

    public ChatRoomAdapter(Context context) {
        this.context = context;
    }

    @Override
    protected Class<QiscusChatRoom> getItemClass() {
        return QiscusChatRoom.class;
    }

    @Override
    protected int compare(QiscusChatRoom item1, QiscusChatRoom item2) {
        return item2.getLastComment().getTime().compareTo(item1.getLastComment().getTime());
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

    public void addOrUpdate(List<QiscusChatRoom> chatRooms) {
        for (QiscusChatRoom chatRoom : chatRooms) {
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

        void bind(QiscusChatRoom chatRoom) {
            Nirmana.getInstance().get()
                    .setDefaultRequestOptions(new RequestOptions()
                            .placeholder(R.drawable.ic_qiscus_avatar)
                            .error(R.drawable.ic_qiscus_avatar)
                            .dontAnimate())
                    .load(chatRoom.getAvatarUrl())
                    .into(avatar);
            name.setText(chatRoom.getName());
            QiscusComment lastComment = chatRoom.getLastComment();
            if (lastComment != null && lastComment.getId() > 0) {
                if (lastComment.getSender() != null) {
                    String lastMessageText = lastComment.isMyComment() ? "You: " : lastComment.getSender().split(" ")[0] + ": ";
                    lastMessageText += chatRoom.getLastComment().getType() == QiscusComment.Type.IMAGE
                            ? "\uD83D\uDCF7 send an image" : lastComment.getMessage();
                    lastMessage.setText(lastMessageText);
                }else{
                    String lastMessageText = "";
                    lastMessageText += chatRoom.getLastComment().getType() == QiscusComment.Type.IMAGE
                            ? "\uD83D\uDCF7 send an image" : lastComment.getMessage();
                    lastMessage.setText(lastMessageText);
                }

                tv_time.setText(DateUtil.getLastMessageTimestamp(lastComment.getTime()));
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
