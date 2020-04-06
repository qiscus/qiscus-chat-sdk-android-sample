package com.qiscus.mychatui.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qiscus.mychatui.R;
import com.qiscus.mychatui.presenter.StartChatWithPresenter;
import com.qiscus.mychatui.util.Const;
import com.qiscus.sdk.chat.core.data.model.QChatRoom;

public class StartChatWithActivity extends AppCompatActivity implements StartChatWithPresenter.View {

    private TextView mTextLoginAs;
    private EditText mEditEmail;
    private Button mBtnStartChat;

    private StartChatWithPresenter presenter;

    public static Intent generateIntent(Context context) {
        return new Intent(context, StartChatWithActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_chat_with);

        presenter = new StartChatWithPresenter(this);

        mTextLoginAs = findViewById(R.id.tv_login_as);
        mEditEmail = findViewById(R.id.et_email);
        mBtnStartChat = findViewById(R.id.bt_start_chat);

        mTextLoginAs.setText("You Logged in as " + Const.qiscusCore2().getQiscusAccount().getName());

        mBtnStartChat.setOnClickListener(view -> {
            if (mEditEmail.getText().toString().isEmpty()) {
                Toast.makeText(this, "Insert Email First", Toast.LENGTH_LONG).show();
            } else {
                presenter.buildChatWith(mEditEmail.getText().toString());
            }
        });
    }

    @Override
    public void openChat(QChatRoom chatRoom) {

        startActivity(ChatRoomActivity.generateIntent(this, chatRoom));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Const.setQiscusCore(Const.qiscusCore2());
    }

    public void closeActivity(View view) {
        finish();
    }
}
