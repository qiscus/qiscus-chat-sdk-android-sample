package com.qiscus.mychatui.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.qiscus.mychatui.MyApplication;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.data.model.User;
import com.qiscus.mychatui.presenter.CreateGroupPresenter;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class CreateGroupActivity extends AppCompatActivity implements CreateGroupPresenter.View {
    private static final String MEMBERS_KEY = "extra_members";

    private ProgressDialog progressDialog;

    public static Intent generateIntent(Context context, List<User> members) {
        Intent intent = new Intent(context, CreateGroupActivity.class);
        intent.putParcelableArrayListExtra(MEMBERS_KEY, (ArrayList<User>) members);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        List<User> members = getIntent().getParcelableArrayListExtra(MEMBERS_KEY);
        if (members == null) {
            finish();
            return;
        }

        EditText editText = findViewById(R.id.et_group_name);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait!");

        CreateGroupPresenter createGroupPresenter = new CreateGroupPresenter(this,
                MyApplication.getInstance().getComponent().getChatRoomRepository());

        findViewById(R.id.submit).setOnClickListener(v -> {
            if (TextUtils.isEmpty(editText.getText().toString())) {
                editText.setError("Please set group name!");
            } else {
                createGroupPresenter.createGroup(editText.getText().toString(), members);
            }
        });

        findViewById(R.id.cancel).setOnClickListener(v -> onBackPressed());
    }

    @Override
    public void showGroupChatRoomPage(QiscusChatRoom chatRoom) {
        startActivity(GroupChatRoomActivity.generateIntent(this, chatRoom));
    }

    @Override
    public void showLoading() {
        progressDialog.show();
    }

    @Override
    public void dismissLoading() {
        progressDialog.dismiss();
    }

    @Override
    public void showErrorMessage(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }
}
