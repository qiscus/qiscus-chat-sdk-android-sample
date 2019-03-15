package com.qiscus.mychatui.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.qiscus.mychatui.MyApplication;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.presenter.LoginPresenter;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class LoginActivity extends AppCompatActivity implements LoginPresenter.View {
    private EditText name;
    private EditText email;
    private EditText password;
    private LinearLayout loginButton;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait!");

        LoginPresenter loginPresenter = new LoginPresenter(this,
                MyApplication.getInstance().getComponent().getUserRepository());
        loginPresenter.start();

        loginButton.setOnClickListener(v -> {
            if (TextUtils.isEmpty(name.getText().toString())) {
                name.setError("Must not empty!");
            } else if (TextUtils.isEmpty(email.getText().toString())) {
                email.setError("Must not empty!");
            } else if (TextUtils.isEmpty(password.getText().toString())) {
                password.setError("Must not empty!");
            } else {
                loginPresenter.login(
                        name.getText().toString(),
                        email.getText().toString(),
                        password.getText().toString()
                );
            }
        });
    }

    @Override
    public void showHomePage() {
        startActivity(new Intent(this, HomeActivity.class));
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
