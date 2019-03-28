package com.qiscus.mychatui.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.qiscus.mychatui.R;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusAccount;
import com.qiscus.sdk.chat.core.data.remote.QiscusApi;

public class EditNameActivity extends AppCompatActivity {
    EditText etName;
    ImageView btSave, btBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_name);

        etName = findViewById(R.id.eTName);
        btBack = findViewById(R.id.bt_back);
        btSave = findViewById(R.id.bt_save);

        etName.setText(QiscusCore.getQiscusAccount().getUsername());

        btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!etName.getText().toString().isEmpty()){
                    QiscusCore.updateUser(etName.getText().toString(), null, null, new QiscusCore.SetUserListener() {
                        @Override
                        public void onSuccess(QiscusAccount qiscusAccount) {
                            //do anything after it successfully updated
                            finish();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            //do anything if error occurs
                        }
                    });

                }
            }
        });

        btBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }
}
