package com.qiscus.mychatui.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.MyApplication;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.presenter.ProfilePresenter;
import com.qiscus.mychatui.util.QiscusImageUtil;
import com.qiscus.mychatui.util.QiscusPermissionsUtil;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.local.QiscusCacheManager;
import com.qiscus.sdk.chat.core.data.model.QiscusAccount;
import com.qiscus.sdk.chat.core.data.remote.QiscusApi;
import com.qiscus.sdk.chat.core.util.QiscusFileUtil;

import java.io.File;
import java.io.IOException;

import id.zelory.compressor.Compressor;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ProfileActivity extends AppCompatActivity implements ProfilePresenter.View {
    private LinearLayout logout, llBottom;
    private ImageView ivAvatar, ivEditName, btBack;
    private TextView tvName, tvUniqueID;

    private PopupWindow mPopupWindow;
    private ProfilePresenter profilePresenter;

    private static final int REQUEST_PICK_IMAGE = 1;
    private static final int REQUEST_FILE_PERMISSION = 2;
    protected static final int TAKE_PICTURE_REQUEST = 3;
    protected static final int RC_CAMERA_PERMISSION = 128;

    private static final String[] FILE_PERMISSION = {
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"
    };
    private static final String[] CAMERA_PERMISSION = {
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE",
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        ivAvatar = findViewById(R.id.ivAvatar);
        ivEditName = findViewById(R.id.ivEditName);
        tvName = findViewById(R.id.tvName);
        tvUniqueID = findViewById(R.id.tvUniqueID);
        btBack = findViewById(R.id.bt_back);
        llBottom = findViewById(R.id.llBottom);
        logout = findViewById(R.id.llLogout);

        profilePresenter = new ProfilePresenter(this,
                MyApplication.getInstance().getComponent().getUserRepository());

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profilePresenter.logout();
            }
        });

        ivAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Initialize a new instance of LayoutInflater service
                LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);

                // Inflate the custom layout/view
                View customView = inflater.inflate(R.layout.popup_window_profile, null);

                // Initialize a new instance of popup window
                mPopupWindow = new PopupWindow(
                        customView,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                );

                // Set an elevation value for popup window
                // Call requires API level 21
                if (Build.VERSION.SDK_INT >= 21) {
                    mPopupWindow.setElevation(5.0f);
                }

                // Get a reference for the custom view close button
                LinearLayout close = (LinearLayout) customView.findViewById(R.id.linCancel);
                LinearLayout linImageGallery = (LinearLayout) customView.findViewById(R.id.linImageGallery);
                LinearLayout linTakePhoto = (LinearLayout) customView.findViewById(R.id.linTakePhoto);

                linImageGallery.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //gallery
                        if (QiscusPermissionsUtil.hasPermissions(getApplication(), FILE_PERMISSION)) {
                            pickImage();
                            mPopupWindow.dismiss();
                        } else {
                            requestReadFilePermission();
                        }
                    }
                });

                linTakePhoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //camera
                        if (QiscusPermissionsUtil.hasPermissions(getApplication(), CAMERA_PERMISSION)) {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            if (intent.resolveActivity(getApplication().getPackageManager()) != null) {
                                File photoFile = null;
                                try {
                                    photoFile = QiscusImageUtil.createImageFile();
                                } catch (IOException ex) {
                                    Toast.makeText(getApplication(), "Failed to write temporary picture!", Toast.LENGTH_SHORT).show();
                                }

                                if (photoFile != null) {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                                    } else {
                                        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                                                FileProvider.getUriForFile(getApplication(), QiscusCore.getApps().getPackageName() + ".qiscus.sdk.provider", photoFile));
                                    }
                                    startActivityForResult(intent, TAKE_PICTURE_REQUEST);
                                }
                                mPopupWindow.dismiss();
                            }
                        } else {
                            requestCameraPermission();
                        }
                    }
                });

                // Set a click listener for the popup window close button
                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Dismiss the popup window
                        mPopupWindow.dismiss();
                    }
                });

                mPopupWindow.setAnimationStyle(R.style.popup_window_animation);

                mPopupWindow.showAtLocation(llBottom, Gravity.BOTTOM, 0, 0);
            }
        });

        ivEditName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //edit name activity
                Intent intent = new Intent(ProfileActivity.this, EditNameActivity.class);
                startActivity(intent);
            }
        });

        btBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        loadProfile();

    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void requestReadFilePermission() {
        if (!QiscusPermissionsUtil.hasPermissions(this, FILE_PERMISSION)) {
            QiscusPermissionsUtil.requestPermissions(this, getString(R.string.qiscus_permission_request_title),
                    REQUEST_FILE_PERMISSION, FILE_PERMISSION);
        }
    }

    protected void requestCameraPermission() {
        if (!QiscusPermissionsUtil.hasPermissions(this, CAMERA_PERMISSION)) {
            QiscusPermissionsUtil.requestPermissions(this, getString(R.string.qiscus_permission_request_title),
                    RC_CAMERA_PERMISSION, CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }


    private void loadProfile() {
        //load profile from local db
        tvUniqueID.setText(QiscusCore.getQiscusAccount().getEmail());
        tvName.setText(QiscusCore.getQiscusAccount().getUsername());

        Nirmana.getInstance().get()
                .setDefaultRequestOptions(new RequestOptions()
                        .placeholder(R.drawable.profile)
                        .error(R.drawable.profile)
                        .dontAnimate())
                .load(QiscusCore.getQiscusAccount().getAvatar())
                .into(ivAvatar);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            try {
                File imageFile = QiscusFileUtil.from(data.getData());
                updateAvatar(imageFile);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to open image file!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == TAKE_PICTURE_REQUEST && resultCode == Activity.RESULT_OK) {
            try {
                File imageFile = QiscusFileUtil.from(Uri.parse(QiscusCacheManager.getInstance().getLastImagePath()));
                updateAvatar(imageFile);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to read picture data!", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    public void updateAvatar(File file) {
        File compressedFile = file;
        if (QiscusFileUtil.isImage(file.getPath()) && !file.getName().endsWith(".gif")) {
            try {
                compressedFile = new Compressor(QiscusCore.getApps()).compressToFile(file);
            } catch (NullPointerException | IOException e) {
                Toast.makeText(this, "Can not read file, please make sure that is not corrupted file!", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            compressedFile = QiscusFileUtil.saveFile(compressedFile);
        }

        if (!file.exists()) { //File have been removed, so we can not upload it anymore
            Toast.makeText(this, "Can not read file, please make sure that is not corrupted file!", Toast.LENGTH_SHORT).show();
            return;
        }

        Subscription subscription = QiscusApi.getInstance()
                .uploadFile(compressedFile, percentage ->
                {
                    //show percentage
                })
                .doOnError(throwable -> {
                    Toast.makeText(this, "Failed to upload image!", Toast.LENGTH_SHORT).show();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uri -> {
                    QiscusCore.updateUser(null, uri.toString(), null, new QiscusCore.SetUserListener() {
                        @Override
                        public void onSuccess(QiscusAccount qiscusAccount) {
                            //do anything after it successfully updated
                            loadProfile();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            //do anything if error occurs
                        }
                    });
                }, throwable -> {
                    throwable.printStackTrace();
                    Toast.makeText(this, "Failed to upload image!", Toast.LENGTH_SHORT).show();
                });

    }

    @Override
    public void logout() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
