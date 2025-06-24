package com.qiscus.mychatui.ui.adapter;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.core.util.PatternsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.ui.view.QiscusProgressView;
import com.qiscus.mychatui.util.CustomDownloaderFileUtils;
import com.qiscus.mychatui.util.DateUtil;
import com.qiscus.mychatui.util.QiscusImageUtil;
import com.qiscus.mychatui.util.TranslateYSpan;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusAccount;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.data.remote.QiscusApi;
import com.qiscus.sdk.chat.core.util.QiscusAndroidUtil;
import com.qiscus.sdk.chat.core.util.QiscusDateUtil;

import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class CommentsAdapter extends SortedRecyclerViewAdapter<QiscusComment, CommentsAdapter.VH> {

    private static final int TYPE_MY_TEXT = 1;
    private static final int TYPE_OPPONENT_TEXT = 2;
    private static final int TYPE_MY_IMAGE = 3;
    private static final int TYPE_OPPONENT_IMAGE = 4;
    private static final int TYPE_MY_FILE = 5;
    private static final int TYPE_OPPONENT_FILE = 6;
    private static final int TYPE_MY_REPLY = 7;
    private static final int TYPE_OPPONENT_REPLY = 8;
    private static final int TYPE_AI_TEXT = 9;

    private Context context;
    private long lastDeliveredCommentId;
    private long lastReadCommentId;
    private QiscusComment selectedComment = null;

    public CommentsAdapter(Context context) {
        this.context = context;
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @Override
    protected Class<QiscusComment> getItemClass() {
        return QiscusComment.class;
    }

    @Override
    protected int compare(QiscusComment item1, QiscusComment item2) {
        if (item2.equals(item1)) { //Same comments
            return 0;
        } else if (item2.getId() == -1 && item1.getId() == -1) { //Not completed comments
            return item2.getTime().compareTo(item1.getTime());
        } else if (item2.getId() != -1 && item1.getId() != -1) { //Completed comments
            return QiscusAndroidUtil.compare(item2.getId(), item1.getId());
        } else if (item2.getId() == -1) {
            return 1;
        } else if (item1.getId() == -1) {
            return -1;
        }
        return item2.getTime().compareTo(item1.getTime());
    }

    @Override
    public int getItemViewType(int position) {
        QiscusComment comment = getData().get(position);
        switch (comment.getType()) {
            case TEXT:
                return comment.isMyComment() ? TYPE_MY_TEXT : TYPE_OPPONENT_TEXT;
            case IMAGE:
                return comment.isMyComment() ? TYPE_MY_IMAGE : TYPE_OPPONENT_IMAGE;
            case FILE:
                return comment.isMyComment() ? TYPE_MY_FILE : TYPE_OPPONENT_FILE;
            case REPLY:
                return comment.isMyComment() ? TYPE_MY_REPLY : TYPE_OPPONENT_REPLY;
            case CUSTOM:
                return TYPE_AI_TEXT;
            default:
                return comment.isMyComment() ? TYPE_MY_TEXT : TYPE_OPPONENT_TEXT;
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_MY_TEXT:
            case TYPE_OPPONENT_TEXT:
                return new TextVH(getView(parent, viewType));
            case TYPE_MY_IMAGE:
            case TYPE_OPPONENT_IMAGE:
                return new ImageVH(getView(parent, viewType));
            case TYPE_MY_FILE:
            case TYPE_OPPONENT_FILE:
                return new FileVH(getView(parent, viewType));
            case TYPE_MY_REPLY:
            case TYPE_OPPONENT_REPLY:
                return new ReplyVH(getView(parent, viewType));
            case TYPE_AI_TEXT:
                return new AITextVH(getView(parent, viewType));
            default:
                return new TextVH(getView(parent, viewType));
        }
    }

    private View getView(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_MY_TEXT:
                return LayoutInflater.from(context).inflate(R.layout.item_my_text_comment, parent, false);
            case TYPE_OPPONENT_TEXT:
                return LayoutInflater.from(context).inflate(R.layout.item_opponent_text_comment, parent, false);
            case TYPE_MY_IMAGE:
                return LayoutInflater.from(context).inflate(R.layout.item_my_image_comment, parent, false);
            case TYPE_OPPONENT_IMAGE:
                return LayoutInflater.from(context).inflate(R.layout.item_opponent_image_comment, parent, false);
            case TYPE_MY_FILE:
                return LayoutInflater.from(context).inflate(R.layout.item_my_file_comment, parent, false);
            case TYPE_OPPONENT_FILE:
                return LayoutInflater.from(context).inflate(R.layout.item_opponent_file_comment, parent, false);
            case TYPE_MY_REPLY:
                return LayoutInflater.from(context).inflate(R.layout.item_my_reply_mc, parent, false);
            case TYPE_OPPONENT_REPLY:
                return LayoutInflater.from(context).inflate(R.layout.item_opponent_reply_mc, parent, false);
            default:
                return LayoutInflater.from(context).inflate(R.layout.item_opponent_text_comment, parent, false);
        }
    }

    @Override
    public void onBindViewHolder(VH holder, @SuppressLint("RecyclerView") int position) {
        holder.bind(getData().get(position));
        holder.position = position;

        if (position == getData().size() - 1) {
            holder.setNeedToShowDate(true);
        } else {
            holder.setNeedToShowDate(!QiscusDateUtil.isDateEqualIgnoreTime(getData().get(position).getTime(),
                    getData().get(position + 1).getTime()));
        }
        setOnClickListener(holder.itemView, position);
    }

    public QiscusComment getSelectedComment() {
        return selectedComment;
    }

    public void setSelectedComment(QiscusComment comment) {
        this.selectedComment = comment;
    }

    public void addOrUpdate(List<QiscusComment> comments) {
        int index;
        for (QiscusComment comment : comments) {
            index = findPosition(comment);
            if (index == -1) getData().add(comment);
            else getData().updateItemAt(index, comment);
        }
        notifyDataSetChanged();
    }

    public void addOrUpdate(QiscusComment comment) {
        int index = findPosition(comment);
        if (index == -1) getData().add(comment);
        else getData().updateItemAt(index, comment);
        notifyDataSetChanged();
    }

    public void clearSelected() {
        int size = getData().size();
        for (int i = 0; i < size; i++) {
            if (getData().get(i).isSelected()) {
                getData().get(i).setSelected(false);
            }
        }
        notifyDataSetChanged();
    }

    public void remove(QiscusComment comment) {
        getData().remove(comment);
        notifyDataSetChanged();
    }

    public void removeAiTypingComment() {
        final int size = getData().size();
        if (size == 0) return;

        final String aiuniqueId = "ai-typing-" + 1001;
        QiscusComment comment;
        for (int i = 0; i < size; i++) {
            comment = getData().get(i);
            if (comment.getUniqueId().equals(aiuniqueId)) {
                getData().removeItemAt(i);
                notifyDataSetChanged();
                return;
            }
        }
    }

    public QiscusComment getLatestSentComment() {
        int size = getData().size();
        for (int i = 0; i < size; i++) {
            QiscusComment comment = getData().get(i);
            if (comment.getState() >= QiscusComment.STATE_ON_QISCUS) {
                return comment;
            }
        }
        return null;
    }

    public void updateLastDeliveredComment(long lastDeliveredCommentId) {
        this.lastDeliveredCommentId = lastDeliveredCommentId;
        updateCommentState();
        notifyDataSetChanged();
    }

    public void updateLastReadComment(long lastReadCommentId) {
        this.lastReadCommentId = lastReadCommentId;
        this.lastDeliveredCommentId = lastReadCommentId;
        updateCommentState();
        notifyDataSetChanged();
    }

    private void updateCommentState() {
        int size = getData().size();
        for (int i = 0; i < size; i++) {
            if (getData().get(i).getState() > QiscusComment.STATE_SENDING) {
                if (getData().get(i).getId() <= lastReadCommentId) {
                    if (getData().get(i).getState() == QiscusComment.STATE_READ) {
                        break;
                    }
                    getData().get(i).setState(QiscusComment.STATE_READ);
                } else if (getData().get(i).getId() <= lastDeliveredCommentId) {
                    if (getData().get(i).getState() == QiscusComment.STATE_DELIVERED) {
                        break;
                    }
                    getData().get(i).setState(QiscusComment.STATE_DELIVERED);
                }
            }
        }
    }

    public interface RecyclerViewItemClickListener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }

    static class VH extends RecyclerView.ViewHolder {
        public int position = 0;
        private ImageView avatar;
        private TextView sender;
        private TextView date;
        private TextView dateOfMessage;
        @Nullable
        private ImageView state;
        private int pendingStateColor;
        private int readStateColor;
        private int failedStateColor;

        VH(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            sender = itemView.findViewById(R.id.sender);
            date = itemView.findViewById(R.id.date);
            dateOfMessage = itemView.findViewById(R.id.dateOfMessage);
            state = itemView.findViewById(R.id.state);

            pendingStateColor = ContextCompat.getColor(itemView.getContext(), R.color.pending_message);
            readStateColor = ContextCompat.getColor(itemView.getContext(), R.color.read_message);
            failedStateColor = ContextCompat.getColor(itemView.getContext(), R.color.qiscus_red);

        }

        void bind(QiscusComment comment) {
            Nirmana.getInstance().get()
                    .setDefaultRequestOptions(new RequestOptions()
                            .placeholder(R.drawable.ic_qiscus_avatar)
                            .error(R.drawable.ic_qiscus_avatar)
                            .dontAnimate())
                    .load(comment.getSenderAvatar())
                    .into(avatar);
            if (sender != null) {
                sender.setText(comment.getSender());
            }
            date.setText(DateUtil.getTimeStringFromDate(comment.getTime()));
            if (dateOfMessage != null) {
                dateOfMessage.setText(DateUtil.toFullDate(comment.getTime()));
            }

            renderState(comment);

        }

        void setNeedToShowDate(Boolean showDate) {
            if (showDate == true) {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.VISIBLE);
                }
            } else {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.GONE);
                }
            }
        }

        private void renderState(QiscusComment comment) {
            if (state != null) {
                switch (comment.getState()) {
                    case QiscusComment.STATE_PENDING:
                    case QiscusComment.STATE_SENDING:
                        state.setColorFilter(pendingStateColor);
                        state.setImageResource(R.drawable.ic_qiscus_info_time);
                        break;
                    case QiscusComment.STATE_ON_QISCUS:
                        state.setColorFilter(pendingStateColor);
                        state.setImageResource(R.drawable.ic_qiscus_sending);
                        break;
                    case QiscusComment.STATE_DELIVERED:
                        state.setColorFilter(pendingStateColor);
                        state.setImageResource(R.drawable.ic_qiscus_read);
                        break;
                    case QiscusComment.STATE_READ:
                        state.setColorFilter(readStateColor);
                        state.setImageResource(R.drawable.ic_qiscus_read);
                        break;
                    case QiscusComment.STATE_FAILED:
                        state.setColorFilter(failedStateColor);
                        state.setImageResource(R.drawable.ic_qiscus_sending_failed);
                        break;
                }
            }
        }
    }

    static class TextVH extends VH {
        protected TextView message;
        private TextView sender;
        private TextView dateOfMessage;
        protected boolean isSetMessage = true;

        TextVH(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.message);
            sender = itemView.findViewById(R.id.sender);
            dateOfMessage = itemView.findViewById(R.id.dateOfMessage);
        }

        @Override
        void bind(QiscusComment comment) {
            super.bind(comment);
            if (isSetMessage) message.setText(comment.getMessage());
            QiscusChatRoom chatRoom = QiscusCore.getDataStore().getChatRoom(comment.getRoomId());

            if (sender != null && chatRoom != null) {
                if (chatRoom.isGroup() == false) {
                    sender.setVisibility(View.GONE);
                } else {
                    sender.setVisibility(View.VISIBLE);
                }
            }

            if (dateOfMessage != null) {
                dateOfMessage.setText(DateUtil.toFullDate(comment.getTime()));
            }
        }

        @Override
        void setNeedToShowDate(Boolean showDate) {
            if (showDate == true) {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.VISIBLE);
                }
            } else {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.GONE);
                }
            }
        }
    }

    static class AITextVH extends TextVH {
        private int frame = 0;
        private  ValueAnimator animator;

        AITextVH(View itemView) {
            super(itemView);
            this.isSetMessage = false;
        }

        @SuppressLint("Recycle")
        @Override
        void bind(QiscusComment comment) {
            super.bind(comment);
            final String textMessage = comment.getMessage();
            final boolean isStarting = textMessage.equals("...");

            final int textLength = textMessage.length();
            final long delay = 33L;

            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }

            int from;
            if (!isStarting
                    && this.message.getText() != null
                    && this.message.getText().length() > 0
                    && textMessage.contains(this.message.getText().toString())
            ) {
                from = this.message.getText().length() -1 ;
            } else {
                from = 0;
            }

            animator = ValueAnimator.ofInt(from, textLength);
            animator.setDuration(delay * textLength);
            animator.setRepeatCount(isStarting ? ValueAnimator.INFINITE : 0);
            animator.addUpdateListener(animation -> {
                if (isStarting) animateDots();
                else animatedSetText(from, textMessage, animation);
            });
            animator.start();
        }

        @SuppressLint("SetTextI18n")
        private void animatedSetText(int from, String textMessage, ValueAnimator animation) {
            this.message.setText(
                    textMessage.substring(0, from)
                    + textMessage.substring(from, (int) animation.getAnimatedValue())
            );
        }

        private void animateDots() {
            frame++;
            SpannableStringBuilder builder = new SpannableStringBuilder("● ● ●");
            for (int i = 0; i < 3; i++) {
                int charIndex = i * 2;
                int offset = getVerticalOffset(frame + i * 10);
                builder.setSpan(
                        new TranslateYSpan(offset),
                        charIndex,
                        charIndex + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            this.message.setText(builder);
        }

        private int getVerticalOffset(int frame) {
            double amplitude = 10;
            double frequency = 0.2;
            return (int) (Math.sin(frame * frequency) * amplitude);
        }
    }

    static class ImageVH extends VH {
        private ImageView thumbnail;
        private TextView messageCaption;
        private TextView sender;
        private TextView dateOfMessage;

        ImageVH(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            messageCaption = itemView.findViewById(R.id.messageCaption);
            sender = itemView.findViewById(R.id.sender);
            dateOfMessage = itemView.findViewById(R.id.dateOfMessage);
        }

        @Override
        void bind(QiscusComment comment) {
            super.bind(comment);

            try {
                JSONObject obj = new JSONObject(comment.getExtraPayload());
                String url = obj.getString("url");
                String caption = obj.getString("caption");
                String filename = obj.getString("file_name");

                if (url.startsWith("http")) { //We have sent it
                    showSentImage(comment, url);
                } else { //Still uploading the image
                    showSendingImage(url);
                }

                if (caption.isEmpty()) {
                    messageCaption.setVisibility(View.GONE);
                } else {
                    messageCaption.setVisibility(View.VISIBLE);
                    messageCaption.setText(caption);
                }

                QiscusChatRoom chatRoom = QiscusCore.getDataStore().getChatRoom(comment.getRoomId());

                if (sender != null) {
                    if (chatRoom.isGroup() == false) {
                        sender.setVisibility(View.GONE);
                    } else {
                        sender.setVisibility(View.VISIBLE);
                    }
                }

                if (dateOfMessage != null) {
                    dateOfMessage.setText(DateUtil.toFullDate(comment.getTime()));
                }

                thumbnail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        File localPath = QiscusCore.getDataStore().getLocalPath(comment.getId());
                        if (localPath != null) {
                            Toast.makeText(itemView.getContext(), "Image already in the gallery", Toast.LENGTH_SHORT).show();
                        } else {
                            downloadFile(comment, filename, url);
                        }
                    }
                });

            } catch (Throwable t) {
                Log.e("SampleCore", "Could not parse malformed JSON: \"" + comment.getExtraPayload() + "\"");
            }
        }

        public void downloadFile(QiscusComment qiscusComment, String fileName, String URLImage) {
            final Observable<File> observable;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                observable = CustomDownloaderFileUtils.downloadFile(
                        Environment.DIRECTORY_DOWNLOADS, URLImage, fileName, total -> {
                            // here you can get the progress total downloaded
                        });
            } else {
                observable = QiscusApi.getInstance().downloadFile(
                        URLImage, fileName, total -> {
                            // here you can get the progress total downloaded
                        });
            }
            observable.doOnNext(file -> {
                        // here we update the local path of file
                        QiscusCore.getDataStore().addOrUpdateLocalPath(
                                qiscusComment.getRoomId(), qiscusComment.getId(), file.getAbsolutePath()
                        );
                        QiscusImageUtil.addImageToGallery(file);
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(file -> {
                        //on success
                        Toast.makeText(itemView.getContext(), "success save image to gallery", Toast.LENGTH_SHORT).show();
                    }, throwable -> {
                        //on error
                    });
        }

        @Override
        void setNeedToShowDate(Boolean showDate) {
            if (showDate == true) {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.VISIBLE);
                }
            } else {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.GONE);
                }
            }
        }

        private void showSendingImage(String url) {
            File localPath = new File(url);
            showLocalImage(localPath);
        }

        private void showSentImage(QiscusComment comment, String url) {
            File localPath = QiscusCore.getDataStore().getLocalPath(comment.getId());
            if (localPath != null) {
                showLocalImage(localPath);
            } else {
                Nirmana.getInstance().get()
                        .setDefaultRequestOptions(new RequestOptions()
                                .placeholder(R.drawable.ic_qiscus_add_image)
                                .error(R.drawable.ic_qiscus_add_image)
                                .dontAnimate())
                        .load(url)
                        .into(thumbnail);
            }
        }

        private void showLocalImage(File localPath) {
            Nirmana.getInstance().get()
                    .setDefaultRequestOptions(new RequestOptions()
                            .placeholder(R.drawable.ic_qiscus_add_image)
                            .error(R.drawable.ic_qiscus_add_image)
                            .dontAnimate())
                    .load(localPath)
                    .into(thumbnail);
        }
    }

    static class FileVH extends VH implements QiscusComment.ProgressListener, QiscusComment.DownloadingListener {
        private TextView fileName;
        private TextView sender;
        private TextView dateOfMessage;
        private QiscusProgressView progress;
        private ImageView icFile;

        FileVH(View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
            sender = itemView.findViewById(R.id.sender);
            dateOfMessage = itemView.findViewById(R.id.dateOfMessage);
            progress = itemView.findViewById(R.id.progress);
            icFile = itemView.findViewById(R.id.ic_file);
        }

        @Override
        void bind(QiscusComment comment) {
            super.bind(comment);
            comment.setProgressListener(this);
            comment.setDownloadingListener(this);

            QiscusChatRoom chatRoom = QiscusCore.getDataStore().getChatRoom(comment.getRoomId());

            if (sender != null) {
                if (chatRoom.isGroup() == false) {
                    sender.setVisibility(View.GONE);
                } else {
                    sender.setVisibility(View.VISIBLE);
                }
            }

            try {
                JSONObject obj = new JSONObject(comment.getExtraPayload());
                String url = obj.getString("url");
                String filename = obj.getString("file_name");
                fileName.setText(filename);

                if (dateOfMessage != null) {
                    dateOfMessage.setText(DateUtil.toFullDate(comment.getTime()));
                }

                fileName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        File localPath = QiscusCore.getDataStore().getLocalPath(comment.getId());
                        if (localPath != null) {
                            QiscusImageUtil.addImageToGallery(localPath);
                            Toast.makeText(itemView.getContext(), "File already save", Toast.LENGTH_SHORT).show();
                        } else {
                            downloadFile(comment, filename, url);
                        }
                    }
                });

            } catch (Throwable t) {
                Log.e("SampleCore", "Could not parse malformed JSON: \"" + comment.getExtraPayload() + "\"");
            }

        }

        @Override
        void setNeedToShowDate(Boolean showDate) {
            if (showDate == true) {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.VISIBLE);
                }
            } else {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onProgress(QiscusComment qiscusComment, int percentage) {
            progress.setProgress(percentage);
            icFile.setVisibility(View.GONE);
            if (percentage == 100) {
                progress.setVisibility(View.GONE);
                icFile.setVisibility(View.VISIBLE);
            } else {
                progress.setVisibility(View.VISIBLE);
                icFile.setVisibility(View.GONE);
            }
        }

        @Override
        public void onDownloading(QiscusComment qiscusComment, boolean downloading) {
            progress.setVisibility(downloading ? View.VISIBLE : View.GONE);
        }

        public void downloadFile(QiscusComment qiscusComment, String fileName, String URLFile) {
            QiscusApi.getInstance()
                    .downloadFile(URLFile, fileName, total -> {
                        // here you can get the progress total downloaded
                    })
                    .doOnNext(file -> {
                        // here we update the local path of file
                        QiscusCore.getDataStore()
                                .addOrUpdateLocalPath(qiscusComment.getRoomId(), qiscusComment.getId(), file.getAbsolutePath());

                        QiscusImageUtil.addImageToGallery(file);
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(file -> {
                        //on success
                        Toast.makeText(itemView.getContext(), "Success save file", Toast.LENGTH_SHORT).show();
                    }, throwable -> {
                        //on error
                    });
        }
    }

    static class ReplyVH extends VH {

        private QiscusAccount qiscusAccount = QiscusCore.getQiscusAccount();
        private TextView message, sender, origin_comment, dateOfMessage;
        private ImageView icon, origin_image;

        ReplyVH(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.message);
            dateOfMessage = itemView.findViewById(R.id.dateOfMessage);
            sender = itemView.findViewById(R.id.origin_sender);
            origin_comment = itemView.findViewById(R.id.origin_comment);
            icon = itemView.findViewById(R.id.icon);
            origin_image = itemView.findViewById(R.id.origin_image);
        }

        @Override
        void bind(QiscusComment comment) {
            super.bind(comment);

            QiscusComment origin = comment.getReplyTo();

            if (qiscusAccount.getEmail().equals(origin.getSenderEmail())) {
                sender.setText("You");
            } else {
                sender.setText(origin.getSender());
            }

            origin_comment.setText(origin.getMessage());
            message.setText(comment.getMessage());
            icon.setVisibility(View.VISIBLE);
            setUpLinks();

            if (origin.getType() == QiscusComment.Type.TEXT) {
                origin_image.setVisibility(View.GONE);
                icon.setVisibility(View.GONE);
            } else if (origin.getType() == QiscusComment.Type.IMAGE) {
                origin_image.setVisibility(View.VISIBLE);
                icon.setImageResource(R.drawable.ic_gallery);

                if (origin.getCaption() == "") {
                    origin_comment.setText("Image");
                } else {
                    origin_comment.setText(origin.getCaption());
                }

                Nirmana.getInstance().get()
                        .setDefaultRequestOptions(
                                new RequestOptions()
                                        .placeholder(R.drawable.ic_qiscus_avatar)
                                        .error(R.drawable.ic_qiscus_avatar)
                                        .dontAnimate()
                        )
                        .load(origin.getAttachmentUri())
                        .into(origin_image);
            } else if (origin.getType() == QiscusComment.Type.FILE) {
                origin_image.setVisibility(View.GONE);
                icon.setVisibility(View.VISIBLE);
                origin_comment.setText(origin.getAttachmentName());
                icon.setImageResource(R.drawable.ic_file);
            } else {
                origin_image.setVisibility(View.GONE);
                icon.setVisibility(View.GONE);
                origin_comment.setText(origin.getMessage());
            }

        }

        private void setUpLinks() {
            String messageData = message.getText().toString();
            Matcher matcher = PatternsCompat.AUTOLINK_WEB_URL.matcher(messageData);
            while (matcher.find()) {
                int start = matcher.start();
                if (start > 0 && messageData.charAt(start - 1) == '@') {
                    continue;
                }
                int end = matcher.end();
                clickify(start, end, () -> {
                    String url = messageData.substring(start, end);
                    if (!url.startsWith("http")) {
                        url = "http://" + url;
                    }
                    new CustomTabsIntent.Builder()
                            .setToolbarColor(ContextCompat.getColor(QiscusCore.getApps(), R.color.qiscus_white))
                            .setShowTitle(true)
                            .addDefaultShareMenuItem()
                            .enableUrlBarHiding()
                            .build()
                            .launchUrl(message.getContext(), Uri.parse(url));
                });
            }
        }

        private void clickify(int start, int end, ClickSpan.OnClickListener listener) {
            CharSequence text = message.getText();
            ClickSpan span = new ClickSpan(listener);

            if (start == -1) {
                return;
            }

            if (text instanceof Spannable) {
                ((Spannable) text).setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                SpannableString s = SpannableString.valueOf(text);
                s.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                message.setText(s);
            }
        }

        @Override
        void setNeedToShowDate(Boolean showDate) {
            if (showDate == true) {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.VISIBLE);
                }
            } else {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.GONE);
                }
            }
        }

        private static class ClickSpan extends ClickableSpan {
            private ClickSpan.OnClickListener listener;

            public ClickSpan(ClickSpan.OnClickListener listener) {
                this.listener = listener;
            }

            @Override
            public void onClick(View widget) {
                if (listener != null) {
                    listener.onClick();
                }
            }

            public interface OnClickListener {
                void onClick();
            }
        }
    }
}
