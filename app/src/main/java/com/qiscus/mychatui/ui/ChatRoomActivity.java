package com.qiscus.mychatui.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.ui.fragment.ChatRoomFragment;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
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
public class ChatRoomActivity extends AppCompatActivity implements ChatRoomFragment.UserTypingListener {
    private static final String CHAT_ROOM_KEY = "extra_chat_room";

    private TextView tvSubtitle;
    private QiscusChatRoom chatRoom;
    private String opponentEmail;
    private LinearLayout linTitleSubtitle;

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
        TextView roomName = findViewById(R.id.room_name);
        ImageView btBack = findViewById(R.id.bt_back);
        tvSubtitle = findViewById(R.id.subtitle);
        linTitleSubtitle = findViewById(R.id.linTitleSubtile);

        Nirmana.getInstance().get()
                .setDefaultRequestOptions(new RequestOptions()
                        .placeholder(R.drawable.ic_qiscus_avatar)
                        .error(R.drawable.ic_qiscus_avatar)
                        .dontAnimate())
                .load(chatRoom.getAvatarUrl())
                .into(avatar);
        roomName.setText(chatRoom.getName());

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,
                        ChatRoomFragment.newInstance(chatRoom),
                        ChatRoomFragment.class.getName())
                .commit();

        getOpponentIfNotGroupEmail();

        listenUser();

        btBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
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

    @Override
    protected void onDestroy() {
        unlistenUser();
        super.onDestroy();
    }

    private void listenUser() {
        if (!chatRoom.isGroup() && opponentEmail != null) {
            QiscusPusherApi.getInstance().listenUserStatus(opponentEmail);
        }
    }

    private void unlistenUser() {
        if (!chatRoom.isGroup() && opponentEmail != null) {
            QiscusPusherApi.getInstance().unListenUserStatus(opponentEmail);
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
}
