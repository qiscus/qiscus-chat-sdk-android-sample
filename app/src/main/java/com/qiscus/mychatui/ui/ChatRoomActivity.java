package com.qiscus.mychatui.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.ui.fragment.ChatRoomFragment;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.data.model.QiscusRoomMember;
import com.qiscus.sdk.chat.core.data.remote.QiscusPusherApi;
import com.qiscus.sdk.chat.core.event.QiscusUserStatusEvent;
import com.qiscus.sdk.chat.core.util.QiscusDateUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import rx.Observable;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class ChatRoomActivity extends AppCompatActivity implements ChatRoomFragment.UserTypingListener, ChatRoomFragment.CommentSelectedListener {
    private static final String CHAT_ROOM_KEY = "extra_chat_room";

    private TextView tvSubtitle, tvTitle;
    private QiscusChatRoom chatRoom;
    private String opponentEmail;
    private ImageButton btn_action_copy, btn_action_delete, btn_action_reply, btn_action_reply_cancel;
    private LinearLayout toolbar_selected_comment;
    public static Intent generateIntent(Context context, QiscusChatRoom chatRoom) {
        Intent intent = new Intent(context, ChatRoomActivity.class);
        intent.putExtra(CHAT_ROOM_KEY, chatRoom);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        chatRoom = getIntent().getParcelableExtra(CHAT_ROOM_KEY);
        if (chatRoom == null) {
            finish();
            return;
        }

        ImageView avatar = findViewById(R.id.avatar);
        ImageButton btBack = findViewById(R.id.btn_back);
        tvSubtitle = findViewById(R.id.subtitle);
        tvTitle = findViewById(R.id.tvTitle);
        btn_action_copy = findViewById(R.id.btn_action_copy);
        btn_action_delete = findViewById(R.id.btn_action_delete);
        btn_action_reply = findViewById(R.id.btn_action_reply);
        btn_action_reply_cancel = findViewById(R.id.btn_action_reply_cancel);
        toolbar_selected_comment = findViewById(R.id.toolbar_selected_comment);

        Nirmana.getInstance().get()
                .setDefaultRequestOptions(new RequestOptions()
                        .placeholder(R.drawable.ic_qiscus_avatar)
                        .error(R.drawable.ic_qiscus_avatar)
                        .dontAnimate())
                .load(chatRoom.getAvatarUrl())
                .into(avatar);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,
                        ChatRoomFragment.newInstance(chatRoom),
                        ChatRoomFragment.class.getName())
                .commit();

        getOpponentIfNotGroupEmail();

        listenUser();

        tvTitle.setText(chatRoom.getName());

        btBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        btn_action_copy.setOnClickListener(view -> getChatFragment().copyComment());
        btn_action_delete.setOnClickListener(view -> getChatFragment().deleteComment());
        btn_action_reply.setOnClickListener(view -> getChatFragment().replyComment());
        btn_action_reply_cancel.setOnClickListener(view -> getChatFragment().clearSelectedComment());
    }

    private void getOpponentIfNotGroupEmail() {
        if (!chatRoom.isGroup()) {
            opponentEmail = Observable.from(chatRoom.getMember())
                    .map(QiscusRoomMember::getEmail)
                    .filter(email -> !email.equals(QiscusCore.getQiscusAccount().getEmail()))
                    .first()
                    .toBlocking()
                    .single();
        }
    }

    private ChatRoomFragment getChatFragment() {
        return (ChatRoomFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    @Override
    protected void onDestroy() {
        unlistenUser();
        super.onDestroy();
    }

    private void listenUser() {
        if (!chatRoom.isGroup() && opponentEmail != null) {
            QiscusPusherApi.getInstance().subscribeUserOnlinePresence(opponentEmail);
        }
    }

    private void unlistenUser() {
        if (!chatRoom.isGroup() && opponentEmail != null) {
            QiscusPusherApi.getInstance().unsubscribeUserOnlinePresence(opponentEmail);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onUserStatusChanged(QiscusUserStatusEvent event) {
        String last = QiscusDateUtil.getRelativeTimeDiff(event.getLastActive());
        tvSubtitle.setText(event.isOnline() ? "Online" : "Last seen " + last);
        tvSubtitle.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUserTyping(String user, boolean typing) {
        tvSubtitle.setText(typing ? "Typing..." : "Online");
        tvSubtitle.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCommentSelected(QiscusComment selectedComment) {
        if (toolbar_selected_comment.getVisibility() == View.VISIBLE) {
            toolbar_selected_comment.setVisibility(View.GONE);
            getChatFragment().clearSelectedComment();
        } else {
            if (selectedComment.isMyComment()) {
                btn_action_delete.setVisibility(View.VISIBLE);
            } else {
                btn_action_delete.setVisibility(View.GONE);
            }

            toolbar_selected_comment.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClearSelectedComment(Boolean status) {
        toolbar_selected_comment.setVisibility(View.INVISIBLE);
    }
}
