package com.qiscus.mychatui.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.MyApplication;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.presenter.HomePresenter;
import com.qiscus.mychatui.ui.adapter.ChatRoomAdapter;
import com.qiscus.mychatui.ui.adapter.OnItemClickListener;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.event.QiscusCommentReceivedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

public class HomeActivity extends AppCompatActivity implements HomePresenter.View, OnItemClickListener {
    private RecyclerView recyclerView;
    private LinearLayout linEmptyChatRooms;
    private ChatRoomAdapter chatRoomAdapter;
    private ImageView createChat, avatarProfile;
    private Button btStartChat;
    private HomePresenter homePresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        linEmptyChatRooms = findViewById(R.id.linEmptyChatRooms);
        createChat = findViewById(R.id.create_chat);
        avatarProfile = findViewById(R.id.avatar_profile);
        btStartChat = findViewById(R.id.bt_start_chat);

        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        chatRoomAdapter = new ChatRoomAdapter(this);
        chatRoomAdapter.setOnItemClickListener(this);

        createChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeActivity.this, ContactActivity.class));
            }
        });

        btStartChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, ContactActivity.class));
            }
        });

        Nirmana.getInstance().get()
                .setDefaultRequestOptions(new RequestOptions()
                        .placeholder(R.drawable.ic_qiscus_avatar)
                        .error(R.drawable.ic_qiscus_avatar)
                        .dontAnimate())
                .load(QiscusCore.getQiscusAccount().getAvatar())
                .into(avatarProfile);

        avatarProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(chatRoomAdapter);

        homePresenter = new HomePresenter(this,
                MyApplication.getInstance().getComponent().getChatRoomRepository(),
                MyApplication.getInstance().getComponent().getUserRepository());
    }

    @Override
    protected void onResume() {
        super.onResume();
        homePresenter.loadChatRooms();
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onCommentReceivedEvent(QiscusCommentReceivedEvent event) {
        homePresenter.loadChatRooms();
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }


    @Override
    public void showChatRooms(List<QiscusChatRoom> chatRooms) {
        if (chatRooms.size() == 0) {
            recyclerView.setVisibility(View.GONE);
            linEmptyChatRooms.setVisibility(View.VISIBLE);
        } else {
            linEmptyChatRooms.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        chatRoomAdapter.addOrUpdate(chatRooms);
    }

    @Override
    public void showChatRoomPage(QiscusChatRoom chatRoom) {
        startActivity(ChatRoomActivity.generateIntent(this, chatRoom));
    }

    @Override
    public void showGroupChatRoomPage(QiscusChatRoom chatRoom) {
        startActivity(GroupChatRoomActivity.generateIntent(this, chatRoom));
    }

    @Override
    public void showErrorMessage(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(int position) {
        homePresenter.openChatRoom(chatRoomAdapter.getData().get(position));
    }
}
