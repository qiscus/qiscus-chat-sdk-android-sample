package com.qiscus.mychatui.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.qiscus.jupuk.JupukConst;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.presenter.QiscusChatPresenter;
import com.qiscus.mychatui.ui.QiscusSendPhotoConfirmationActivity;
import com.qiscus.mychatui.ui.adapter.CommentsAdapter;
import com.qiscus.mychatui.ui.view.QiscusChatScrollListener;
import com.qiscus.mychatui.util.QiscusPermissionsUtil;
import com.qiscus.sdk.chat.core.data.local.QiscusCacheManager;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.data.model.QiscusPhoto;
import com.qiscus.sdk.chat.core.data.remote.QiscusPusherApi;
import com.qiscus.sdk.chat.core.util.QiscusAndroidUtil;
import com.qiscus.sdk.chat.core.util.QiscusFileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class ChatRoomFragment extends Fragment implements QiscusChatPresenter.View,
        QiscusPermissionsUtil.PermissionCallbacks, QiscusChatScrollListener.Listener {
    private static final String CHAT_ROOM_KEY = "extra_chat_room";
    private static final int REQUEST_PICK_IMAGE = 1;
    private static final int REQUEST_FILE_PERMISSION = 2;

    private static final String[] FILE_PERMISSION = {
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"
    };
    protected static final int TAKE_PICTURE_REQUEST = 3;
    protected static final int SEND_PICTURE_CONFIRMATION_REQUEST = 4;
    protected static final int SHOW_MEDIA_DETAIL = 5;

    private EditText messageField;
    private ImageView sendButton;
    private ImageView attachImageButton;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private CommentsAdapter commentsAdapter;
    private LinearLayout emptyChat;

    private QiscusChatPresenter chatPresenter;

    private QiscusChatRoom chatRoom;

    private UserTypingListener userTypingListener;
    private boolean typing;
    private Runnable stopTypingNotifyTask;

    public static ChatRoomFragment newInstance(QiscusChatRoom chatRoom) {
        ChatRoomFragment chatRoomFragment = new ChatRoomFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(CHAT_ROOM_KEY, chatRoom);
        chatRoomFragment.setArguments(bundle);
        return chatRoomFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_room, container, false);
        messageField = view.findViewById(R.id.field_message);
        sendButton = view.findViewById(R.id.button_send);
        attachImageButton = view.findViewById(R.id.button_add_image);
        progressBar = view.findViewById(R.id.progress_bar);
        recyclerView = view.findViewById(R.id.recyclerview);
        emptyChat = view.findViewById(R.id.empty_chat);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        chatRoom = getArguments().getParcelable(CHAT_ROOM_KEY);
        if (chatRoom == null) {
            throw new RuntimeException("Please provide chat room");
        }

        stopTypingNotifyTask = () -> {
            typing = false;
            notifyServerTyping(false);
        };

        messageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                typing = true;
                notifyServerTyping(true);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        sendButton.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(messageField.getText())) {
                chatPresenter.sendComment(messageField.getText().toString());
                messageField.setText("");
            }
        });

        attachImageButton.setOnClickListener(v -> {
            if (QiscusPermissionsUtil.hasPermissions(getActivity(), FILE_PERMISSION)) {
                pickImage();
            } else {
                requestReadFilePermission();
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new QiscusChatScrollListener(layoutManager, this));
        commentsAdapter = new CommentsAdapter(getActivity());
        recyclerView.setAdapter(commentsAdapter);

        commentsAdapter.setOnItemClickListener(new CommentsAdapter.RecyclerViewItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                //no action
            }

            @Override
            public void onItemLongClick(View view, int position) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());
                builder1.setMessage("Sure to delete this message??");
                builder1.setCancelable(true);

                builder1.setPositiveButton(
                        "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                chatPresenter.deleteComment(commentsAdapter.getData().get(position));
                                dialog.cancel();
                            }
                        });

                builder1.setNegativeButton(
                        "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert11 = builder1.create();
                alert11.show();

            }
        });

        chatPresenter = new QiscusChatPresenter(this, chatRoom);
        chatPresenter.loadComments(20);
    }

    private void notifyServerTyping(boolean typing) {
        QiscusPusherApi.getInstance().setUserTyping(chatRoom.getId(), typing);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof UserTypingListener) {
            userTypingListener = (UserTypingListener) getActivity();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        QiscusCacheManager.getInstance().setLastChatActivity(true, chatRoom.getId());
        notifyLatestRead();
    }

    @Override
    public void onPause() {
        super.onPause();
        QiscusCacheManager.getInstance().setLastChatActivity(false, chatRoom.getId());
    }

    @Override
    public void showLoadMoreLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void initRoomData(QiscusChatRoom chatRoom, List<QiscusComment> comments) {
        this.chatRoom = chatRoom;
        commentsAdapter.addOrUpdate(comments);

        if (comments.size() == 0) {
            emptyChat.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyChat.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRoomChanged(QiscusChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    @Override
    public void showComments(List<QiscusComment> comments) {
        commentsAdapter.addOrUpdate(comments);
    }

    @Override
    public void onLoadMore(List<QiscusComment> comments) {
        commentsAdapter.addOrUpdate(comments);
    }

    @Override
    public void onSendingComment(QiscusComment comment) {
        commentsAdapter.addOrUpdate(comment);
        recyclerView.smoothScrollToPosition(0);
        if (emptyChat.isShown()) {
            emptyChat.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSuccessSendComment(QiscusComment comment) {
        commentsAdapter.addOrUpdate(comment);
    }

    @Override
    public void onFailedSendComment(QiscusComment comment) {
        commentsAdapter.addOrUpdate(comment);
    }

    @Override
    public void onNewComment(QiscusComment comment) {
        commentsAdapter.addOrUpdate(comment);
        if (((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition() <= 2) {
            recyclerView.smoothScrollToPosition(0);
        }

        if (emptyChat.isShown()) {
            emptyChat.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCommentDeleted(QiscusComment comment) {
        commentsAdapter.remove(comment);
    }

    @Override
    public void refreshComment(QiscusComment comment) {
        commentsAdapter.addOrUpdate(comment);
    }

    @Override
    public void updateLastDeliveredComment(long lastDeliveredCommentId) {
        commentsAdapter.updateLastDeliveredComment(lastDeliveredCommentId);
    }

    @Override
    public void updateLastReadComment(long lastReadCommentId) {
        commentsAdapter.updateLastReadComment(lastReadCommentId);
    }

    @Override
    public void onFileDownloaded(File file, String mimeType) {

    }

    @Override
    public void startPhotoViewer(QiscusComment qiscusComment) {

    }

    @Override
    public void onUserTyping(String user, boolean typing) {
        if (userTypingListener != null) {
            userTypingListener.onUserTyping(user, typing);
        }
    }

    @Override
    public void showCommentsAndScrollToTop(List<QiscusComment> qiscusComments) {

    }

    @Override
    public void onRealtimeStatusChanged(boolean connected) {
        if (connected) {
            QiscusComment comment = commentsAdapter.getLatestSentComment();
            if (comment != null) {
                chatPresenter.loadCommentsAfter(comment);
            }
        }
    }

    @Override
    public void onLoadCommentsError(Throwable throwable) {
        throwable.printStackTrace();
        Log.e("ChatRoomFragment", throwable.getMessage());
    }

    @Override
    public void clearCommentsBefore(long timestamp) {

    }

    @Override
    public void showError(String errorMessage) {
        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void showDeleteLoading() {

    }

    @Override
    public void notifyDataChanged() {
        commentsAdapter.notifyDataSetChanged();
    }

    @Override
    public void dismissLoading() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            try {
                File imageFile = QiscusFileUtil.from(data.getData());
                List<QiscusPhoto> qiscusPhotos = new ArrayList<>();
                qiscusPhotos.add(new QiscusPhoto(imageFile));
                startActivityForResult(QiscusSendPhotoConfirmationActivity.generateIntent(getActivity(),
                        chatRoom, qiscusPhotos),
                        SEND_PICTURE_CONFIRMATION_REQUEST);
            } catch (Exception e) {
                showError("Failed to open image file!");
            }
        } else if (requestCode == SEND_PICTURE_CONFIRMATION_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                showError(getString(R.string.qiscus_chat_error_failed_open_picture));
                return;
            }

            Map<String, String> captions = (Map<String, String>)
                    data.getSerializableExtra(QiscusSendPhotoConfirmationActivity.EXTRA_CAPTIONS);
            List<QiscusPhoto> qiscusPhotos = data.getParcelableArrayListExtra(QiscusSendPhotoConfirmationActivity.EXTRA_QISCUS_PHOTOS);
            if (qiscusPhotos != null) {
                for (QiscusPhoto qiscusPhoto : qiscusPhotos) {
                    chatPresenter.sendFile(qiscusPhoto.getPhotoFile(), captions.get(qiscusPhoto.getPhotoFile().getAbsolutePath()));
                }
            } else {
                showError(getString(R.string.qiscus_chat_error_failed_read_picture));
            }
        }
    }

    private void requestReadFilePermission() {
        if (!QiscusPermissionsUtil.hasPermissions(getActivity(), FILE_PERMISSION)) {
            QiscusPermissionsUtil.requestPermissions(this, getString(R.string.qiscus_permission_request_title),
                    REQUEST_FILE_PERMISSION, FILE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        QiscusPermissionsUtil.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == REQUEST_FILE_PERMISSION) {
            pickImage();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        QiscusPermissionsUtil.checkDeniedPermissionsNeverAskAgain(this, getString(R.string.qiscus_permission_message),
                R.string.qiscus_grant, R.string.qiscus_denny, perms);
    }

    @Override
    public void onTopOffListMessage() {
        loadMoreComments();
    }

    @Override
    public void onMiddleOffListMessage() {

    }

    @Override
    public void onBottomOffListMessage() {

    }

    private void loadMoreComments() {
        if (progressBar.getVisibility() == View.GONE && commentsAdapter.getItemCount() > 0) {
            QiscusComment comment = commentsAdapter.getData().get(commentsAdapter.getItemCount() - 1);
            if (comment.getId() == -1 || comment.getCommentBeforeId() > 0) {
                chatPresenter.loadOlderCommentThan(comment);
            }
        }
    }

    private void notifyLatestRead() {
        QiscusComment comment = commentsAdapter.getLatestSentComment();
        if (comment != null) {
            QiscusPusherApi.getInstance()
                    .setUserRead(chatRoom.getId(), comment.getId());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        notifyLatestRead();
        chatPresenter.detachView();
    }

    public interface UserTypingListener {
        void onUserTyping(String user, boolean typing);
    }
}
