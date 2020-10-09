package com.qiscus.mychatui.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.ui.fragment.ChatRoomFragment;
import com.qiscus.mychatui.util.Const;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.data.model.QChatRoom;
import com.qiscus.sdk.chat.core.data.model.QParticipant;
import com.qiscus.sdk.chat.core.data.remote.QiscusApi;

import java.util.List;


/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class GroupChatRoomActivity extends AppCompatActivity implements ChatRoomFragment.UserTypingListener, QiscusApi.MetaRoomParticipantsListener {
    private static final String CHAT_ROOM_KEY = "extra_chat_room";

    private TextView membersView;

    private QChatRoom chatRoom;
    private String subtitle;

    public static Intent generateIntent(Context context, QChatRoom chatRoom) {
        Intent intent = new Intent(context, GroupChatRoomActivity.class);
        intent.putExtra(CHAT_ROOM_KEY, chatRoom);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat_room);

        chatRoom = getIntent().getParcelableExtra(CHAT_ROOM_KEY);
        if (chatRoom == null) {
            finish();
            return;
        }
        setParticipants();

        findViewById(R.id.back).setOnClickListener(v -> onBackPressed());
        ImageView avatar = findViewById(R.id.avatar);
        TextView roomName = findViewById(R.id.room_name);
        LinearLayout linTitleSubtitle = findViewById(R.id.linTitleSubtitle);
        membersView = findViewById(R.id.members);

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

        linTitleSubtitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(RoomInfoActivity.generateIntent(getApplication(), chatRoom));
                finish();
            }
        });
    }

    private void setParticipants() {
        Const.qiscusCore().getApi().getParticipants(chatRoom.getUniqueId(), 1, 100, null, this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(participants -> {
                    chatRoom.setParticipants(participants);
                    membersView.setText(generateSubtitle(chatRoom.getParticipants()));
                }, throwable -> {
                    throwable.printStackTrace();
                });
    }

    private String generateSubtitle(List<QParticipant> members) {
        if (!TextUtils.isEmpty(subtitle)) {
            return subtitle;
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (QParticipant member : members) {
            if (!member.getId().equalsIgnoreCase(Const.qiscusCore().getQiscusAccount().getId())) {
                count++;
                builder.append(member.getName().split(" ")[0]);
                if (count < members.size() - 1) {
                    builder.append(", ");
                }
            }
            if (count >= 10) {
                break;
            }
        }
        builder.append(String.format(" %s", getString(R.string.qiscus_group_member_closing)));
        if (count == 0) {
            builder = new StringBuilder(getString(R.string.qiscus_group_member_only_you));
        }
        subtitle = builder.toString();
        return subtitle;
    }

    @Override
    public void onUserTyping(String user, boolean typing) {
        if (typing) {
            QParticipant member = findMember(user);
            if (member != null) {
                membersView.setText(member.getName().split(" ")[0] + " is typing...");
            } else {
                membersView.setText(generateSubtitle(chatRoom.getParticipants()));
            }
        } else {
            membersView.setText(generateSubtitle(chatRoom.getParticipants()));
        }
    }

    private QParticipant findMember(String userId) {
        for (QParticipant member : chatRoom.getParticipants()) {
            if (member.getId().equals(userId)) {
                return member;
            }
        }
        return null;
    }

    @Override
    public void onMetaReceived(int currentOffset, int perPage, int total) {

    }
}
