package com.qiscus.mychatui.ui;

import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.qiscus.mychatui.R;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.remote.QiscusApi;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EditRoomNameActivity extends AppCompatActivity {
    private static final String CHAT_ROOM_KEY = "extra_chat_room";
    private QiscusChatRoom chatRoom;
    ImageView bt_back, bt_save;
    EditText etName;

    public static Intent generateIntent(Context context, QiscusChatRoom chatRoom) {
        Intent intent = new Intent(context, EditRoomNameActivity.class);
        intent.putExtra(CHAT_ROOM_KEY, chatRoom);
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_room_name);

        chatRoom = getIntent().getParcelableExtra(CHAT_ROOM_KEY);
        if (chatRoom == null) {
            finish();
            return;
        }

        setupUI();
        loadData();
    }

    private void setupUI(){
        bt_back = findViewById(R.id.bt_back);
        bt_save = findViewById(R.id.bt_save);
        etName = findViewById(R.id.eTName);



        bt_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        bt_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (etName.getText().toString().isEmpty()){
                    return;
                }

                QiscusApi.getInstance().updateChatRoom(chatRoom.getId(), etName.getText().toString(), null,null)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(newChatroom -> {
                            startActivity(RoomInfoActivity.generateIntent(getApplication(), newChatroom));
                            finish();
                        }, throwable -> {
                            throwable.printStackTrace();
                        });

            }
        });
    }

    private void loadData(){
        etName.setText(chatRoom.getName());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(RoomInfoActivity.generateIntent(getApplication(), chatRoom));
        finish();
    }
}
