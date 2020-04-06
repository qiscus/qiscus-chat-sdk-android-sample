package com.qiscus.mychatui.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.qiscus.mychatui.MyApplication;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.presenter.HomePresenter;
import com.qiscus.mychatui.ui.adapter.ChatRoomAdapter;
import com.qiscus.mychatui.ui.adapter.OnItemClickListener;
import com.qiscus.mychatui.util.Const;
import com.qiscus.mychatui.util.FirebaseUtil;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.data.model.QChatRoom;
import com.qiscus.sdk.chat.core.data.model.QMessage;
import com.qiscus.sdk.chat.core.event.QMessageReceivedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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

        FirebaseUtil.sendCurrentToken();

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
//                Const.qiscusCore().getApi().chatUser("s@mail.com", null)
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(qiscusChatRoom -> {
//                            startActivity(ChatRoomActivity.generateIntent(HomeActivity.this, qiscusChatRoom));
//                        }, throwable -> {
//                            Log.e("cccc", "onClick: " + throwable);
//                        });
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
                .load(Const.qiscusCore1().getQiscusAccount().getAvatarUrl())
                .into(avatarProfile);

        avatarProfile.setOnClickListener(v -> {
            Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.custom_dialog);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

            RelativeLayout mLayoutClose = dialog.findViewById(R.id.rv_close);
            RelativeLayout mLayoutProfile = dialog.findViewById(R.id.rv_profile);
            RelativeLayout mLayoutAppId2 = dialog.findViewById(R.id.rv_appid2);

            mLayoutClose.setOnClickListener(view -> {
                dialog.dismiss();
            });

            mLayoutProfile.setOnClickListener(view -> {
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                startActivity(intent);
            });

            mLayoutAppId2.setOnClickListener(view -> {
                Const.setQiscusCore(Const.qiscusCore2());
                startActivity(new Intent(this, LoginActivityAppId2.class));
            });

            dialog.show();
        });

        recyclerView.setAdapter(chatRoomAdapter);

        homePresenter = new HomePresenter(this,
                MyApplication.getInstance().getComponent().getChatRoomRepository(),
                MyApplication.getInstance().getComponent().getUserRepository());

//        Used for fixing realtime issue in API below Lollipop(5.0)
        try {
            ProviderInstaller.installIfNeeded(getApplicationContext());
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Const.setQiscusCore(Const.qiscusCore1());
        homePresenter.loadChatRooms();
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onCommentReceivedEvent(QMessageReceivedEvent event) {
        homePresenter.loadChatRooms();
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }


    @Override
    public void showChatRooms(List<QChatRoom> chatRooms) {
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
    public void showChatRoomPage(QChatRoom chatRoom) {
        startActivity(ChatRoomActivity.generateIntent(this, chatRoom));
    }

    @Override
    public void showGroupChatRoomPage(QChatRoom chatRoom) {
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
