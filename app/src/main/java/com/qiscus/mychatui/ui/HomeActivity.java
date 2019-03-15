package com.qiscus.mychatui.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.qiscus.mychatui.MyApplication;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.presenter.HomePresenter;
import com.qiscus.mychatui.ui.adapter.ChatRoomAdapter;
import com.qiscus.mychatui.ui.adapter.OnItemClickListener;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.event.QiscusCommentReceivedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

public class HomeActivity extends AppCompatActivity implements HomePresenter.View, OnItemClickListener {
    private RecyclerView recyclerView;
    private LinearLayout linEmptyChatRooms;
    private ChatRoomAdapter chatRoomAdapter;
    private ImageView create_chat, avatar_profile;
    private HomePresenter homePresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        linEmptyChatRooms = findViewById(R.id.linEmptyChatRooms);
        create_chat = findViewById(R.id.create_chat);
        avatar_profile = findViewById(R.id.avatar_profile);

        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        chatRoomAdapter = new ChatRoomAdapter(this);
        chatRoomAdapter.setOnItemClickListener(this);

        create_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(HomeActivity.this)
                        .setTitle("Choose")
                        .setMessage("What you want??")
                        .setPositiveButton("Create Chat", (dialog, which) -> homePresenter.createChatRoom())
                        .setNegativeButton("Logout", (dialog, which) -> homePresenter.logout())
                        .show();
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

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = new MenuInflater(this);
//        inflater.inflate(R.menu.home, menu);
//        return super.onCreateOptionsMenu(menu);
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.new_chat) {
//            homePresenter.createChatRoom();
//        } else if (item.getItemId() == R.id.new_group) {
//            homePresenter.createGroupChatRoom();
//        } else if (item.getItemId() == R.id.logout) {
//            new AlertDialog.Builder(this)
//                    .setTitle("Logout")
//                    .setMessage("Are you sure wants to logout?")
//                    .setPositiveButton("Logout", (dialog, which) -> homePresenter.logout())
//                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
//                    .show();
//        }
//        return super.onOptionsItemSelected(item);
//    }

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
    public void showContactPage() {
        startActivity(new Intent(this, ContactActivity.class));
    }

    @Override
    public void showSelectContactPage() {
        startActivity(new Intent(this, SelectContactActivity.class));
    }

    @Override
    public void showLoginPage() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void showErrorMessage(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showTesting(QiscusComment qiscusComment) {
        Toast.makeText(this, "hai dari subscribe", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(int position) {
        homePresenter.openChatRoom(chatRoomAdapter.getData().get(position));
    }
}
