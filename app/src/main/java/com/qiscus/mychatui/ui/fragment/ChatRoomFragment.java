package com.qiscus.mychatui.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qiscus.jupuk.JupukBuilder;
import com.qiscus.jupuk.JupukConst;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.presenter.QiscusChatPresenter;
import com.qiscus.mychatui.ui.QiscusSendPhotoConfirmationActivity;
import com.qiscus.mychatui.ui.adapter.CommentsAdapter;
import com.qiscus.mychatui.ui.view.QiscusChatScrollListener;
import com.qiscus.mychatui.util.ActivityResultHandler;
import com.qiscus.mychatui.util.QiscusImageUtil;
import com.qiscus.mychatui.util.QiscusPermissionsUtil;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.local.QiscusCacheManager;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.data.model.QiscusPhoto;
import com.qiscus.sdk.chat.core.data.remote.QiscusPusherApi;
import com.qiscus.sdk.chat.core.util.QiscusFileUtil;

import java.io.File;
import java.io.IOException;
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
        QiscusChatScrollListener.Listener, ActivityResultHandler.IActivityOnResult {

    private static final String CHAT_ROOM_KEY = "extra_chat_room";
    protected static final int REQUEST_CODE_SEND_PICTURE_CONFIRMATION = 124;

    private EditText messageField;
    private ImageView sendButton, attachImageButton, originImage, btnCancelReply;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private CommentsAdapter commentsAdapter;
    private LinearLayout emptyChat, linAttachment, linTakePhoto, linImageGallery, linFileDocument, linCancel, rootViewSender;
    private TextView originSender, originContent;
    private QiscusChatPresenter chatPresenter;
    private QiscusChatRoom chatRoom;
    private UserTypingListener userTypingListener;
    private boolean typing;
    private Runnable stopTypingNotifyTask;
    private QiscusComment selectedComment = null;
    private CommentSelectedListener commentSelectedListener = null;
    private ActivityResultHandler activityResultHandler;
    private Activity activity;

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
        linAttachment = view.findViewById(R.id.linAttachment);
        linTakePhoto = view.findViewById(R.id.linTakePhoto);
        linImageGallery = view.findViewById(R.id.linImageGallery);
        linFileDocument = view.findViewById(R.id.linFileDocument);
        linCancel = view.findViewById(R.id.linCancel);
        rootViewSender = view.findViewById(R.id.rootViewSender);
        originSender = view.findViewById(R.id.originSender);
        originImage = view.findViewById(R.id.originImage);
        originContent = view.findViewById(R.id.originContent);
        btnCancelReply = view.findViewById(R.id.btnCancelReply);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.activityResultHandler = new ActivityResultHandler(this, this);
        this.activityResultHandler.registerLauncher();
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
                if (rootViewSender.getVisibility() == View.VISIBLE) {
                    if (selectedComment != null) {
                        chatPresenter.sendReplyComment(messageField.getText().toString(), selectedComment);
                    }

                    rootViewSender.setVisibility(View.GONE);
                    selectedComment = null;
                } else {
                    chatPresenter.sendComment(messageField.getText().toString());
                }

                messageField.setText("");
            }
        });

        btnCancelReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rootViewSender.setVisibility(View.GONE);
            }
        });

        attachImageButton.setOnClickListener(v -> {
            if (linAttachment.isShown()) {
                hideAttachmentPanel();
            } else {
                showAttachmentPanel();
            }

        });

        linTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //camera
                if (QiscusPermissionsUtil.hasPermissions(activity, QiscusPermissionsUtil.CAMERA_PERMISSION)) {
                    openCamera();
                } else {
                    requestCameraPermission();
                }
            }
        });


        linImageGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //gallery
                if (QiscusPermissionsUtil.hasPermissions(activity, QiscusPermissionsUtil.FILE_PERMISSION)) {
                    pickImage();
                    hideAttachmentPanel();
                } else {
                    requestReadFilePermission();
                }
            }
        });

        linFileDocument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //file
                if (QiscusPermissionsUtil.hasPermissions(activity, QiscusPermissionsUtil.FILE_PERMISSION)) {
                    final Intent intent = new JupukBuilder().setMaxCount(1)
                            .setColorPrimary(ContextCompat.getColor(activity, R.color.colorPrimary))
                            .setColorPrimaryDark(ContextCompat.getColor(activity, R.color.colorPrimaryDark))
                            .setColorAccent(ContextCompat.getColor(activity, R.color.colorAccent))
                            .generateIntent(activity, QiscusPermissionsUtil.JUPUK_DOC_PICKER);

                    activityResultHandler.setRequestCode(QiscusPermissionsUtil.PICK_FILE_REQUEST_CODE)
                            .openActivityForResult(intent);
                    hideAttachmentPanel();
                } else {
                    requestReadFilePermission();
                }
            }
        });

        linCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linAttachment.setVisibility(View.GONE);
            }
        });


        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
        //recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new QiscusChatScrollListener(layoutManager, this));
        commentsAdapter = new CommentsAdapter(activity);
        recyclerView.setAdapter(commentsAdapter);

        commentsAdapter.setOnItemClickListener(new CommentsAdapter.RecyclerViewItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                //no action
            }

            @Override
            public void onItemLongClick(View view, int position) {
                toggleSelectedComment(commentsAdapter.getData().get(position));
            }
        });

        chatPresenter = new QiscusChatPresenter(this, chatRoom);
        chatPresenter.loadComments(20);
    }

    public void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = QiscusImageUtil.createImageFile();
            } catch (IOException ex) {
                showError(getString(R.string.qiscus_chat_error_failed_write));
            }

            if (photoFile != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                } else {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(
                            activity,
                            QiscusCore.getApps().getPackageName() + ".qiscus.sdk.provider",
                            photoFile
                    ));
                }
                activityResultHandler.setRequestCode(QiscusPermissionsUtil.TAKE_PICTURE_REQUEST_CODE)
                        .openActivityForResult(intent);
            }
            hideAttachmentPanel();
        }
    }

    public void toggleSelectedComment(QiscusComment comment) {
        if (comment.getType() != QiscusComment.Type.SYSTEM_EVENT || comment.getType() != QiscusComment.Type.CARD) {
            comment.setSelected(true);
            commentsAdapter.addOrUpdate(comment);
            commentsAdapter.setSelectedComment(comment);
            if (commentSelectedListener != null) {
                commentSelectedListener.onCommentSelected(comment);
            }

        }
    }

     public void replyComment() {
        clearSelectedComment();
        selectedComment = commentsAdapter.getSelectedComment();
        if (selectedComment == null) {
            rootViewSender.setVisibility(View.GONE);
        } else {
            rootViewSender.setVisibility(View.VISIBLE);
        }
        
        if (selectedComment != null) {
            bindReplyView(selectedComment);
        }
    }

    private void bindReplyView(QiscusComment origin) {
        originSender.setText(origin.getSender());

        if (origin.getType() == QiscusComment.Type.IMAGE) {
            originImage.setVisibility(View.VISIBLE);
            Nirmana.getInstance().get()
                    .load(origin.getAttachmentUri())
                    .into(originImage);
            originContent.setText(origin.getCaption());
        } else if (origin.getType() == QiscusComment.Type.FILE) {
            originContent.setText(origin.getAttachmentName());
            originImage.setVisibility(View.GONE);
        } else {
            originImage.setVisibility(View.GONE);
            originContent.setText(origin.getMessage());
        }
    }


    public void deleteComment() {
        clearSelectedComment();

        QiscusComment selectedComment = commentsAdapter.getSelectedComment();

        if (selectedComment != null) {
            showDialogDeleteComment(selectedComment);
        }
    }

    private void showDialogDeleteComment(QiscusComment qiscusComment) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
        builder1.setMessage("Sure to delete this message??");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        chatPresenter.deleteComment(qiscusComment);
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

    public void clearSelectedComment() {
        commentSelectedListener.onClearSelectedComment(true);
        commentsAdapter.clearSelected();
    }

    public void copyComment() {
        clearSelectedComment();

        QiscusComment
                commentSelected = commentsAdapter.getSelectedComment();

        String textCopied = "";

        if (commentSelected.getType() == QiscusComment.Type.FILE) {
            textCopied = commentSelected.getAttachmentName();
        } else if (commentSelected.getType() == QiscusComment.Type.IMAGE) {
            textCopied =  commentSelected.getCaption();
        } else {
            textCopied = commentSelected.getMessage();
        }

        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(
                "Message",
                textCopied
        );
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getContext(), "messages copied!", Toast.LENGTH_SHORT).show();
    }

    private void hideAttachmentPanel() {
        linAttachment.setVisibility(View.GONE);
    }

    private void showAttachmentPanel() {
        linAttachment.setVisibility(View.VISIBLE);
    }

    private void notifyServerTyping(boolean typing) {
        QiscusPusherApi.getInstance().publishTyping(chatRoom.getId(), typing);
    }

    public void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        this.activityResultHandler.setRequestCode(QiscusPermissionsUtil.REQUEST_CODE_PICK_IMAGE)
                .openActivityForResult(intent);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.activity = (Activity) context;

        if (activity instanceof CommentSelectedListener) {
            commentSelectedListener = (CommentSelectedListener) activity;
            userTypingListener = (UserTypingListener) activity;
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
        notifyLatestRead();
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
        Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
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

    @SuppressLint("RestrictedApi")
    @Override
    public void handleOnActivityResult(int resultCode, int requestCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QiscusPermissionsUtil.REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            try {
                File imageFile = QiscusFileUtil.from(data.getData());
                List<QiscusPhoto> qiscusPhotos = new ArrayList<>();
                qiscusPhotos.add(new QiscusPhoto(imageFile));

                this.activityResultHandler.setRequestCode(REQUEST_CODE_SEND_PICTURE_CONFIRMATION)
                        .openActivityForResult(
                                QiscusSendPhotoConfirmationActivity.generateIntent(
                                        activity, chatRoom, qiscusPhotos
                                )
                        );
            } catch (Exception e) {
                showError("Failed to open image file!");
            }
        } else if (requestCode == REQUEST_CODE_SEND_PICTURE_CONFIRMATION && resultCode == Activity.RESULT_OK) {
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
        } else if (requestCode == QiscusPermissionsUtil.PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                showError(getString(R.string.qiscus_chat_error_failed_open_file));
                return;
            }
            ArrayList<String> paths = data.getStringArrayListExtra(JupukConst.KEY_SELECTED_DOCS);
            if (paths.size() > 0) {
                chatPresenter.sendFile(new File(paths.get(0)));
            }
        } else if (requestCode == QiscusPermissionsUtil.TAKE_PICTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                File imageFile = QiscusFileUtil.from(Uri.parse(QiscusCacheManager.getInstance().getLastImagePath()));
                List<QiscusPhoto> qiscusPhotos = new ArrayList<>();
                qiscusPhotos.add(new QiscusPhoto(imageFile));

                this.activityResultHandler.setRequestCode(REQUEST_CODE_SEND_PICTURE_CONFIRMATION)
                        .openActivityForResult(
                                QiscusSendPhotoConfirmationActivity.generateIntent(
                                        activity, chatRoom, qiscusPhotos
                                )
                        );
            } catch (Exception e) {
                showError(getString(R.string.qiscus_chat_error_failed_read_picture));
            }
        }
    }

    private void requestReadFilePermission() {
        if (!QiscusPermissionsUtil.hasPermissions(activity, QiscusPermissionsUtil.FILE_PERMISSION)) {
            QiscusPermissionsUtil.requestPermissions(activity, getString(R.string.qiscus_permission_request_title),
                    QiscusPermissionsUtil.REQUEST_CODE_PICK_FILE, QiscusPermissionsUtil.FILE_PERMISSION);
        }
    }

    protected void requestCameraPermission() {
        if (!QiscusPermissionsUtil.hasPermissions(activity, QiscusPermissionsUtil.CAMERA_PERMISSION)) {
            QiscusPermissionsUtil.requestPermissions(activity, getString(R.string.qiscus_permission_request_title),
                    QiscusPermissionsUtil.REQUEST_CODE_OPEN_CAMERA, QiscusPermissionsUtil.CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        QiscusPermissionsUtil.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
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
                    .markAsRead(chatRoom.getId(), comment.getId());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        notifyLatestRead();
        chatPresenter.detachView();
    }

    public interface CommentSelectedListener {
        void onCommentSelected(QiscusComment selectedComment);
        void onClearSelectedComment(Boolean status);
    }

    public interface UserTypingListener {
        void onUserTyping(String user, boolean typing);
    }
}
