package com.qiscus.mychatui.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.qiscus.mychatui.MyApplication;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.data.model.User;
import com.qiscus.mychatui.presenter.SelectContactPresenter;
import com.qiscus.mychatui.ui.adapter.SelectContactAdapter;

import java.util.List;

/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class SelectContactActivity extends AppCompatActivity implements SelectContactPresenter.View {
    private RecyclerView recyclerView;
    private SelectContactAdapter selectContactAdapter;

    private SelectContactPresenter selectContactPresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contact);

        findViewById(R.id.back).setOnClickListener(v -> onBackPressed());
        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        selectContactAdapter = new SelectContactAdapter(this);
        recyclerView.setAdapter(selectContactAdapter);

        findViewById(R.id.next).setOnClickListener(v -> submit());

        selectContactPresenter = new SelectContactPresenter(this,
                MyApplication.getInstance().getComponent().getUserRepository());
        selectContactPresenter.loadContacts(1, 100, "");
    }

    @Override
    public void showContacts(List<User> contacts) {
        selectContactAdapter.addOrUpdate(contacts);
    }

    @Override
    public void showCreateGroupPage(List<User> members) {
        startActivity(CreateGroupActivity.generateIntent(this, members));
    }

    @Override
    public void showErrorMessage(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void submit() {
        selectContactPresenter.selectContacts(selectContactAdapter.getSelectedContacts());
    }
}
