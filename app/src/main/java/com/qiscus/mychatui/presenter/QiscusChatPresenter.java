package com.qiscus.mychatui.presenter;

import android.webkit.MimeTypeMap;

import androidx.core.util.Pair;

import com.qiscus.mychatui.R;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.local.QiscusCacheManager;
import com.qiscus.sdk.chat.core.data.model.QiscusAccount;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.data.model.QiscusContact;
import com.qiscus.sdk.chat.core.data.model.QiscusLocation;
import com.qiscus.sdk.chat.core.data.model.QiscusRoomMember;
import com.qiscus.sdk.chat.core.data.remote.QiscusApi;
import com.qiscus.sdk.chat.core.data.remote.QiscusPusherApi;
import com.qiscus.sdk.chat.core.data.remote.QiscusResendCommentHelper;
import com.qiscus.sdk.chat.core.event.QiscusClearCommentsEvent;
import com.qiscus.sdk.chat.core.event.QiscusCommentDeletedEvent;
import com.qiscus.sdk.chat.core.event.QiscusCommentReceivedEvent;
import com.qiscus.sdk.chat.core.event.QiscusCommentResendEvent;
import com.qiscus.sdk.chat.core.event.QiscusMqttStatusEvent;
import com.qiscus.sdk.chat.core.presenter.QiscusChatRoomEventHandler;
import com.qiscus.sdk.chat.core.util.QiscusAndroidUtil;
import com.qiscus.sdk.chat.core.util.QiscusFileUtil;
import com.qiscus.sdk.chat.core.util.QiscusTextUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import id.zelory.compressor.Compressor;
import retrofit2.HttpException;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * @author Yuana andhikayuana@gmail.com
 * @since Jul, Fri 27 2018 12.52
 **/
public class QiscusChatPresenter extends QiscusPresenter<QiscusChatPresenter.View> implements QiscusChatRoomEventHandler.StateListener {

    private QiscusChatRoom room;
    private QiscusAccount qiscusAccount;
    private Func2<QiscusComment, QiscusComment, Integer> commentComparator = (lhs, rhs) -> rhs.getTime().compareTo(lhs.getTime());

    private Map<QiscusComment, Subscription> pendingTask;

    private QiscusChatRoomEventHandler roomEventHandler;

    public QiscusChatPresenter(View view, QiscusChatRoom room) {
        super(view);
        this.view = view;

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        this.room = room;
        if (this.room.getMember().isEmpty()) {
            this.room = QiscusCore.getDataStore().getChatRoom(room.getId());
        }
        qiscusAccount = QiscusCore.getQiscusAccount();
        pendingTask = new HashMap<>();

        roomEventHandler = new QiscusChatRoomEventHandler(this.room, this);
    }

    private void commentSuccess(QiscusComment qiscusComment) {
        pendingTask.remove(qiscusComment);
        qiscusComment.setState(QiscusComment.STATE_ON_QISCUS);
        QiscusComment savedQiscusComment = QiscusCore.getDataStore().getComment(qiscusComment.getUniqueId());
        if (savedQiscusComment != null && savedQiscusComment.getState() > qiscusComment.getState()) {
            qiscusComment.setState(savedQiscusComment.getState());
        }
        QiscusCore.getDataStore().addOrUpdate(qiscusComment);
    }

    private boolean mustFailed(Throwable throwable, QiscusComment qiscusComment) {
        //Error response from server
        //Means something wrong with server, e.g user is not member of these room anymore
        return ((throwable instanceof HttpException && ((HttpException) throwable).code() >= 400) ||
                //if throwable from JSONException, e.g response from server not json as expected
                (throwable instanceof JSONException) ||
                // if attachment type
                qiscusComment.isAttachment());
    }

    private void commentFail(Throwable throwable, QiscusComment qiscusComment) {
        pendingTask.remove(qiscusComment);
        if (!QiscusCore.getDataStore().isContains(qiscusComment)) { //Have been deleted
            return;
        }

        int state = QiscusComment.STATE_PENDING;
        if (mustFailed(throwable, qiscusComment)) {
            qiscusComment.setDownloading(false);
            state = QiscusComment.STATE_FAILED;
        }

        //Kalo ternyata comment nya udah sukses dikirim sebelumnya, maka ga usah di update
        QiscusComment savedQiscusComment = QiscusCore.getDataStore().getComment(qiscusComment.getUniqueId());
        if (savedQiscusComment != null && savedQiscusComment.getState() > QiscusComment.STATE_SENDING) {
            return;
        }

        //Simpen statenya
        qiscusComment.setState(state);
        QiscusCore.getDataStore().addOrUpdate(qiscusComment);
    }

    public void cancelPendingComment(QiscusComment qiscusComment) {
        if (pendingTask.containsKey(qiscusComment)) {
            Subscription subscription = pendingTask.get(qiscusComment);
            if (!subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
            pendingTask.remove(qiscusComment);
        }
    }

    private void sendComment(QiscusComment qiscusComment) {
        view.onSendingComment(qiscusComment);
        Subscription subscription = QiscusApi.getInstance().sendMessage(qiscusComment)
                .doOnSubscribe(() -> QiscusCore.getDataStore().addOrUpdate(qiscusComment))
                .doOnNext(this::commentSuccess)
                .doOnError(throwable -> commentFail(throwable, qiscusComment))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(commentSend -> {
                    if (commentSend.getRoomId() == room.getId()) {
                        view.onSuccessSendComment(commentSend);
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    if (qiscusComment.getRoomId() == room.getId()) {
                        view.onFailedSendComment(qiscusComment);
                    }
                });

        pendingTask.put(qiscusComment, subscription);
    }

    public void sendComment(String content) {
        QiscusComment qiscusComment = QiscusComment.generateMessage(room.getId(), content);
        sendComment(qiscusComment);
    }

    public void sendContact(QiscusContact contact) {
        QiscusComment qiscusComment = QiscusComment.generateContactMessage(room.getId(), contact);
        sendComment(qiscusComment);
    }

    public void sendLocation(QiscusLocation location) {
        QiscusComment qiscusComment = QiscusComment.generateLocationMessage(room.getId(), location);
        sendComment(qiscusComment);
    }

    public void sendCommentPostBack(String content, String payload) {
        QiscusComment qiscusComment = QiscusComment.generatePostBackMessage(room.getId(), content, payload);
        sendComment(qiscusComment);
    }

    public void sendReplyComment(String content, QiscusComment originComment) {
        QiscusComment qiscusComment = QiscusComment.generateReplyMessage(room.getId(), content, originComment);
        sendComment(qiscusComment);
    }

    public void resendComment(QiscusComment qiscusComment) {
        qiscusComment.setState(QiscusComment.STATE_SENDING);
        qiscusComment.setTime(new Date());
        if (qiscusComment.isAttachment()) {
            resendFile(qiscusComment);
        } else {
            sendComment(qiscusComment);
        }
    }

    public void sendFile(File file) {
        sendFile(file, null);
    }

    public void sendFile(File file, String caption) {
        File compressedFile = file;
        Boolean isImage = false;
        String fileAttachment = "image";
        String sendMassage = "Send Image";
        if (QiscusFileUtil.isImage(file.getPath()) && !file.getName().endsWith(".gif")) {
            try {
                isImage = true;
                compressedFile = new Compressor(QiscusCore.getApps()).compressToFile(file);
            } catch (NullPointerException | IOException e) {
                view.showError(QiscusTextUtil.getString(R.string.qiscus_corrupted_file));
                return;
            }
        } else {
            compressedFile = QiscusFileUtil.saveFile(compressedFile);
        }

        if (!file.exists()) { //File have been removed, so we can not upload it anymore
            view.showError(QiscusTextUtil.getString(R.string.qiscus_corrupted_file));
            return;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("url", compressedFile.getPath())
                    .put("caption", caption)
                    .put("file_name", file.getName());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (isImage == false) {
            fileAttachment = "file";
            sendMassage = "Send FIle";
        }

        QiscusComment qiscusComment = QiscusComment.generateCustomMessage(room.getId(), sendMassage, fileAttachment, json);

        qiscusComment.setDownloading(true);
        view.onSendingComment(qiscusComment);

        File finalCompressedFile = compressedFile;
        Subscription subscription = QiscusApi.getInstance()
                .upload(compressedFile, percentage ->
                {
                    qiscusComment.setProgress((int) percentage);
                })
                .doOnSubscribe(() -> QiscusCore.getDataStore().addOrUpdate(qiscusComment))
                .flatMap(uri -> {
                    try {
                        JSONObject jsonData = new JSONObject(qiscusComment.getExtraPayload());
                        JSONObject content = jsonData.getJSONObject("content");
                        content.put("url", uri);
                        qiscusComment.setExtraPayload(jsonData.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return QiscusApi.getInstance().sendMessage(qiscusComment);
                })
                .doOnNext(commentSend -> {
                    QiscusCore.getDataStore()
                            .addOrUpdateLocalPath(commentSend.getRoomId(), commentSend.getId(), finalCompressedFile.getAbsolutePath());
                    qiscusComment.setDownloading(false);
                    commentSuccess(commentSend);
                })
                .doOnError(throwable -> commentFail(throwable, qiscusComment))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(commentSend -> {
                    if (commentSend.getRoomId() == room.getId()) {
                        view.onSuccessSendComment(commentSend);
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    if (qiscusComment.getRoomId() == room.getId()) {
                        view.onFailedSendComment(qiscusComment);
                    }
                });

        pendingTask.put(qiscusComment, subscription);
    }


    private void resendFile(QiscusComment qiscusComment) {
        if (qiscusComment.getAttachmentUri().toString().startsWith("http")) { //We forward file message
            forwardFile(qiscusComment);
            return;
        }

        File file = new File(qiscusComment.getAttachmentUri().toString());
        if (!file.exists()) { //File have been removed, so we can not upload it anymore
            qiscusComment.setDownloading(false);
            qiscusComment.setState(QiscusComment.STATE_FAILED);
            QiscusCore.getDataStore().addOrUpdate(qiscusComment);
            view.onFailedSendComment(qiscusComment);
            return;
        }

        qiscusComment.setDownloading(true);
        qiscusComment.setProgress(0);
        Subscription subscription = QiscusApi.getInstance()
                .upload(file, percentage -> qiscusComment.setProgress((int) percentage))
                .doOnSubscribe(() -> QiscusCore.getDataStore().addOrUpdate(qiscusComment))
                .flatMap(uri -> {
                    qiscusComment.updateAttachmentUrl(uri.toString());
                    return QiscusApi.getInstance().sendMessage(qiscusComment);
                })
                .doOnNext(commentSend -> {
                    QiscusCore.getDataStore()
                            .addOrUpdateLocalPath(commentSend.getRoomId(), commentSend.getId(), file.getAbsolutePath());
                    qiscusComment.setDownloading(false);
                    commentSuccess(commentSend);
                })
                .doOnError(throwable -> commentFail(throwable, qiscusComment))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(commentSend -> {
                    if (commentSend.getRoomId() == room.getId()) {
                        view.onSuccessSendComment(commentSend);
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    if (qiscusComment.getRoomId() == room.getId()) {
                        view.onFailedSendComment(qiscusComment);
                    }
                });

        pendingTask.put(qiscusComment, subscription);
    }

    private void forwardFile(QiscusComment qiscusComment) {
        qiscusComment.setProgress(100);
        Subscription subscription = QiscusApi.getInstance().sendMessage(qiscusComment)
                .doOnSubscribe(() -> QiscusCore.getDataStore().addOrUpdate(qiscusComment))
                .doOnNext(commentSend -> {
                    qiscusComment.setDownloading(false);
                    commentSuccess(commentSend);
                })
                .doOnError(throwable -> {
                    qiscusComment.setDownloading(false);
                    commentFail(throwable, qiscusComment);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(commentSend -> {
                    if (commentSend.getRoomId() == room.getId()) {
                        view.onSuccessSendComment(commentSend);
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    if (qiscusComment.getRoomId() == room.getId()) {
                        view.onFailedSendComment(qiscusComment);
                    }
                });

        pendingTask.put(qiscusComment, subscription);
    }

    public void deleteComment(QiscusComment qiscusComment) {
        cancelPendingComment(qiscusComment);
        QiscusResendCommentHelper.cancelPendingComment(qiscusComment);

        // this code for delete from local
        QiscusAndroidUtil.runOnBackgroundThread(() -> QiscusCore.getDataStore().delete(qiscusComment));
        if (view != null) {
            view.dismissLoading();
            view.onCommentDeleted(qiscusComment);
        }
        Observable.from(new QiscusComment[]{qiscusComment})
                .map(QiscusComment::getUniqueId)
                .toList()
                .flatMap(uniqueIds -> QiscusApi.getInstance().deleteMessages(uniqueIds))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(deletedComments -> {
                    if (view != null) {
                        view.dismissLoading();
                        view.onCommentDeleted(qiscusComment);
                    }
                }, throwable -> {
                    if (view != null) {
                        view.dismissLoading();
                        //view.showError(QiscusTextUtil.getString(R.string.failed_to_delete_messages));
                    }

                });
    }

    private Observable<Pair<QiscusChatRoom, List<QiscusComment>>> getInitRoomData() {
        return QiscusApi.getInstance().getChatRoomWithMessages(room.getId())
                .doOnError(throwable -> {
                    throwable.printStackTrace();
                    QiscusAndroidUtil.runOnUIThread(() -> {
                        if (view != null) {
                            view.onLoadCommentsError(throwable);
                        }
                    });
                })
                .doOnNext(roomData -> {
                    roomEventHandler.setChatRoom(roomData.first);

                    Collections.sort(roomData.second, (lhs, rhs) -> rhs.getTime().compareTo(lhs.getTime()));

                    QiscusCore.getDataStore().addOrUpdate(roomData.first);
                })
                .doOnNext(roomData -> {
                    for (QiscusComment qiscusComment : roomData.second) {
                        QiscusCore.getDataStore().addOrUpdate(qiscusComment);
                    }
                })
                .subscribeOn(Schedulers.io())
                .onErrorReturn(throwable -> null);
    }

    private Observable<List<QiscusComment>> getCommentsFromNetwork(long lastCommentId) {
        return QiscusApi.getInstance().getPreviousMessagesById(room.getId(), 20, lastCommentId)
                .doOnNext(qiscusComment -> {
                    QiscusCore.getDataStore().addOrUpdate(qiscusComment);
                    qiscusComment.setRoomId(room.getId());
                })
                .toSortedList(commentComparator)
                .subscribeOn(Schedulers.io());
    }

    private Observable<List<QiscusComment>> getLocalComments(int count, boolean forceFailedSendingComment) {
        return QiscusCore.getDataStore().getObservableComments(room.getId(), 2 * count)
                .flatMap(Observable::from)
                .toSortedList(commentComparator)
                .map(comments -> {
                    if (comments.size() > count) {
                        return comments.subList(0, count);
                    }
                    return comments;
                })
                .subscribeOn(Schedulers.io());
    }

    public List<QiscusComment> loadLocalComments(int count) {
        return QiscusCore.getDataStore().getComments(room.getId(), count);
    }

    public void loadComments(int count) {
        Observable.merge(getInitRoomData(), getLocalComments(count, true)
                .map(comments -> Pair.create(room, comments)))
                .filter(qiscusChatRoomListPair -> qiscusChatRoomListPair != null)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(roomData -> {
                    if (view != null) {
                        room = roomData.first;
                        view.initRoomData(roomData.first, roomData.second);
                        view.dismissLoading();
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    if (view != null) {
                        view.onLoadCommentsError(throwable);
                        view.dismissLoading();
                    }
                });
    }

    private List<QiscusComment> cleanFailedComments(List<QiscusComment> qiscusComments) {
        List<QiscusComment> comments = new ArrayList<>();
        for (QiscusComment qiscusComment : qiscusComments) {
            if (qiscusComment.getId() != -1) {
                comments.add(qiscusComment);
            }
        }
        return comments;
    }

    private boolean isValidOlderComments(List<QiscusComment> qiscusComments, QiscusComment lastQiscusComment) {
        if (qiscusComments.isEmpty()) return false;

        qiscusComments = cleanFailedComments(qiscusComments);
        boolean containsLastValidComment = qiscusComments.size() <= 0 || lastQiscusComment.getId() == -1;
        int size = qiscusComments.size();

        if (size == 1) {
            return qiscusComments.get(0).getCommentBeforeId() == 0
                    && lastQiscusComment.getCommentBeforeId() == qiscusComments.get(0).getId();
        }

        for (int i = 0; i < size - 1; i++) {
            if (!containsLastValidComment && qiscusComments.get(i).getId() == lastQiscusComment.getCommentBeforeId()) {
                containsLastValidComment = true;
            }

            if (qiscusComments.get(i).getCommentBeforeId() != qiscusComments.get(i + 1).getId()) {
                return false;
            }
        }
        return containsLastValidComment;
    }

    private boolean isValidChainingComments(List<QiscusComment> qiscusComments) {
        qiscusComments = cleanFailedComments(qiscusComments);
        int size = qiscusComments.size();
        for (int i = 0; i < size - 1; i++) {
            if (qiscusComments.get(i).getCommentBeforeId() != qiscusComments.get(i + 1).getId()) {
                return false;
            }
        }
        return true;
    }

    public void loadOlderCommentThan(QiscusComment qiscusComment) {
        view.showLoadMoreLoading();
        QiscusCore.getDataStore().getObservableOlderCommentsThan(qiscusComment, room.getId(), 40)
                .flatMap(Observable::from)
                .filter(qiscusComment1 -> qiscusComment.getId() == -1 || qiscusComment1.getId() < qiscusComment.getId())
                .toSortedList(commentComparator)
                .map(comments -> {
                    if (comments.size() >= 20) {
                        return comments.subList(0, 20);
                    }
                    return comments;
                })
                .doOnNext(this::updateRepliedSender)
                .flatMap(comments -> isValidOlderComments(comments, qiscusComment) ?
                        Observable.from(comments).toSortedList(commentComparator) :
                        getCommentsFromNetwork(qiscusComment.getId()).map(comments1 -> {
                            for (QiscusComment localComment : comments) {
                                if (localComment.getState() <= QiscusComment.STATE_SENDING) {
                                    comments1.add(localComment);
                                }
                            }
                            return comments1;
                        }))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(comments -> {
                    if (view != null) {
                        view.onLoadMore(comments);
                        view.dismissLoading();
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    if (view != null) {
                        view.onLoadCommentsError(throwable);
                        view.dismissLoading();
                    }
                });
    }

    private void updateRepliedSender(List<QiscusComment> comments) {
        for (QiscusComment comment : comments) {
            if (comment.getType() == QiscusComment.Type.REPLY) {
                QiscusComment repliedComment = comment.getReplyTo();
                if (repliedComment != null) {
                    for (QiscusRoomMember qiscusRoomMember : room.getMember()) {
                        if (repliedComment.getSenderEmail().equals(qiscusRoomMember.getEmail())) {
                            repliedComment.setSender(qiscusRoomMember.getUsername());
                            comment.setReplyTo(repliedComment);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    public void onMqttEvent(QiscusMqttStatusEvent event) {
        view.onRealtimeStatusChanged(event == QiscusMqttStatusEvent.CONNECTED);
    }

    public void loadCommentsAfter(QiscusComment comment) {
        QiscusApi.getInstance().getNextMessagesById(room.getId(), 20, comment.getId())
                .doOnNext(qiscusComment -> qiscusComment.setRoomId(room.getId()))
                .toSortedList(commentComparator)
                .doOnNext(Collections::reverse)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(comments -> {
                    if (view != null) {
                        view.onLoadMore(comments);
                    }
                }, Throwable::printStackTrace);
    }

    @Subscribe
    public void handleRetryCommentEvent(QiscusCommentResendEvent event) {
        if (event.getQiscusComment().getRoomId() == room.getId()) {
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    view.refreshComment(event.getQiscusComment());
                }
            });
        }
    }

    @Subscribe
    public void handleDeleteCommentEvent(QiscusCommentDeletedEvent event) {
        if (event.getQiscusComment().getRoomId() == room.getId()) {
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    if (event.isHardDelete()) {
                        view.onCommentDeleted(event.getQiscusComment());
                    } else {
                        view.refreshComment(event.getQiscusComment());
                    }
                }
            });
        }
    }

    @Subscribe
    public void onCommentReceivedEvent(QiscusCommentReceivedEvent event) {
        if (event.getQiscusComment().getRoomId() == room.getId()) {
            onGotNewComment(event.getQiscusComment());
        }
    }

    @Subscribe
    public void handleClearCommentsEvent(QiscusClearCommentsEvent event) {
        if (event.getRoomId() == room.getId()) {
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    view.clearCommentsBefore(event.getTimestamp());
                }
            });
        }
    }

    private void onGotNewComment(QiscusComment qiscusComment) {
        if (qiscusComment.getSenderEmail().equalsIgnoreCase(qiscusAccount.getEmail())) {
            QiscusAndroidUtil.runOnBackgroundThread(() -> commentSuccess(qiscusComment));
        } else {
            roomEventHandler.onGotComment(qiscusComment);
        }

        if (qiscusComment.getRoomId() == room.getId()) {
            QiscusAndroidUtil.runOnBackgroundThread(() -> {
                if (!qiscusComment.getSenderEmail().equalsIgnoreCase(qiscusAccount.getEmail())
                        && QiscusCacheManager.getInstance().getLastChatActivity().first) {
                    QiscusPusherApi.getInstance().markAsRead(room.getId(), qiscusComment.getId());
                }
            });
            view.onNewComment(qiscusComment);
        }
    }

    public void downloadFile(final QiscusComment qiscusComment) {
        if (qiscusComment.isDownloading()) {
            return;
        }

        File file = QiscusCore.getDataStore().getLocalPath(qiscusComment.getId());
        if (file == null) {
            qiscusComment.setDownloading(true);
            QiscusApi.getInstance()
                    .downloadFile(qiscusComment.getAttachmentUri().toString(), qiscusComment.getAttachmentName(),
                            percentage -> qiscusComment.setProgress((int) percentage))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(bindToLifecycle())
                    .doOnNext(file1 -> {
                        QiscusFileUtil.notifySystem(file1);
                        qiscusComment.setDownloading(false);
                        QiscusCore.getDataStore().addOrUpdateLocalPath(qiscusComment.getRoomId(), qiscusComment.getId(),
                                file1.getAbsolutePath());
                    })
                    .subscribe(file1 -> {
                        view.notifyDataChanged();
                        if (qiscusComment.getType() == QiscusComment.Type.AUDIO) {
                            qiscusComment.playAudio();
                        } else if (qiscusComment.getType() == QiscusComment.Type.FILE
                                || qiscusComment.getType() == QiscusComment.Type.VIDEO) {
                            view.onFileDownloaded(file1, MimeTypeMap.getSingleton().getMimeTypeFromExtension(qiscusComment.getExtension()));
                        }
                    }, throwable -> {
                        throwable.printStackTrace();
                        qiscusComment.setDownloading(false);
                        view.showError(QiscusTextUtil.getString(R.string.qiscus_failed_download_file));
                    });
        } else {
            if (qiscusComment.getType() == QiscusComment.Type.AUDIO) {
                qiscusComment.playAudio();
            } else if (qiscusComment.getType() == QiscusComment.Type.IMAGE) {
                view.startPhotoViewer(qiscusComment);
            } else {
                view.onFileDownloaded(file, MimeTypeMap.getSingleton().getMimeTypeFromExtension(qiscusComment.getExtension()));
            }
        }
    }

    public void loadUntilComment(QiscusComment qiscusComment) {
        QiscusCore.getDataStore().getObservableCommentsAfter(qiscusComment, room.getId())
                .map(comments -> comments.contains(qiscusComment) ? comments : new ArrayList<QiscusComment>())
                .doOnNext(qiscusComments -> {
                    if (qiscusComments.isEmpty()) {
                        QiscusAndroidUtil.runOnUIThread(() -> {
                            if (view != null) {
                                view.showError(QiscusTextUtil.getString(R.string.qiscus_message_too_far));
                            }
                        });
                    }
                })
                .flatMap(Observable::from)
                .toSortedList(commentComparator)
                .flatMap(comments -> isValidChainingComments(comments) ?
                        Observable.from(comments).toSortedList(commentComparator) :
                        Observable.just(new ArrayList<QiscusComment>()))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(comments -> {
                    if (view != null) {
                        view.showCommentsAndScrollToTop(comments);
                    }
                }, Throwable::printStackTrace);
    }

    public void forward(List<QiscusComment> forwardComments) {
        for (int i = 0; i < forwardComments.size(); i++) {
            int finalI = i;
            QiscusAndroidUtil.runOnUIThread(() -> {
                QiscusComment forwardComment = forwardComments.get(finalI);
                QiscusComment qiscusComment;
                if (forwardComment.getType() == QiscusComment.Type.CONTACT) {
                    qiscusComment = QiscusComment.generateContactMessage(room.getId(), forwardComment.getContact());
                } else if (forwardComment.getType() == QiscusComment.Type.LOCATION) {
                    qiscusComment = QiscusComment.generateLocationMessage(room.getId(), forwardComment.getLocation());
                } else if (forwardComment.getType() == QiscusComment.Type.IMAGE) {
                    qiscusComment = QiscusComment.generateFileAttachmentMessage(room.getId(),
                            forwardComment.getAttachmentUri().toString(), forwardComment.getCaption(),
                            forwardComment.getAttachmentName());
                } else if (forwardComment.getType() == QiscusComment.Type.AUDIO) {
                    qiscusComment = QiscusComment.generateFileAttachmentMessage(room.getId(), forwardComment.getAttachmentUri().toString(),
                            forwardComment.getCaption(),
                            forwardComment.getAttachmentName());
                } else if (forwardComment.getType() == QiscusComment.Type.FILE) {
                    qiscusComment = QiscusComment.generateFileAttachmentMessage(room.getId(), forwardComment.getAttachmentUri().toString(),
                            forwardComment.getCaption(),
                            forwardComment.getAttachmentName());
                } else if (forwardComment.getType() == QiscusComment.Type.VIDEO) {
                    qiscusComment = QiscusComment.generateFileAttachmentMessage(room.getId(), forwardComment.getAttachmentUri().toString(),
                            forwardComment.getCaption(),
                            forwardComment.getAttachmentName());
                } else {
                    qiscusComment = QiscusComment.generateMessage(room.getId(), forwardComment.getMessage());
                }
                resendComment(qiscusComment);
            }, i * 100);
        }
    }

    private void clearUnreadCount() {
        room.setUnreadCount(0);
        room.setLastComment(null);
        QiscusCore.getDataStore().addOrUpdate(room);
    }

    public void detachView() {
        roomEventHandler.detach();
        clearUnreadCount();
        room = null;
        EventBus.getDefault().unregister(this);
    }

    public void deleteCommentsForEveryone(List<QiscusComment> comments) {
        deleteComments(comments);
    }

    /**
     * @param comments
     */
    private void deleteComments(List<QiscusComment> comments) {
        view.showDeleteLoading();
        Observable.from(comments)
                .map(QiscusComment::getUniqueId)
                .toList()
                .flatMap(uniqueIds -> QiscusApi.getInstance().deleteMessages(uniqueIds))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(deletedComments -> {
                    if (view != null) {
                        view.dismissLoading();
                    }
                }, throwable -> {
                    if (view != null) {
                        view.dismissLoading();
                        view.showError(QiscusTextUtil.getString(R.string.failed_to_delete_messages));
                    }
                });
    }

    @Override
    public void onChatRoomNameChanged(String name) {
        room.setName(name);
        QiscusAndroidUtil.runOnUIThread(() -> {
            if (view != null) {
                view.onRoomChanged(room);
            }
        });
    }

    @Override
    public void onChatRoomMemberAdded(QiscusRoomMember member) {
        if (!room.getMember().contains(member)) {
            room.getMember().add(member);
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    view.onRoomChanged(room);
                }
            });
        }
    }

    @Override
    public void onChatRoomMemberRemoved(QiscusRoomMember member) {
        int x = room.getMember().indexOf(member);
        if (x >= 0) {
            room.getMember().remove(x);
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    view.onRoomChanged(room);
                }
            });
        }
    }

    @Override
    public void onUserTypng(String email, boolean typing) {
        QiscusAndroidUtil.runOnUIThread(() -> {
            if (view != null) {
                view.onUserTyping(email, typing);
            }
        });
    }

    @Override
    public void onChangeLastDelivered(long lastDeliveredCommentId) {
        QiscusAndroidUtil.runOnUIThread(() -> {
            if (view != null) {
                view.updateLastDeliveredComment(lastDeliveredCommentId);
            }
        });
    }

    @Override
    public void onChangeLastRead(long lastReadCommentId) {
        QiscusAndroidUtil.runOnUIThread(() -> {
            if (view != null) {
                view.updateLastReadComment(lastReadCommentId);
            }
        });
    }

    public interface View extends QiscusPresenter.View {

        void dismissLoading();

        void showError(String msg);

        void showLoadMoreLoading();

        void showDeleteLoading();

        void initRoomData(QiscusChatRoom qiscusChatRoom, List<QiscusComment> comments);

        void onRoomChanged(QiscusChatRoom qiscusChatRoom);

        void showComments(List<QiscusComment> qiscusComments);

        void onLoadMore(List<QiscusComment> qiscusComments);

        void onSendingComment(QiscusComment qiscusComment);

        void onSuccessSendComment(QiscusComment qiscusComment);

        void onFailedSendComment(QiscusComment qiscusComment);

        void onNewComment(QiscusComment qiscusComment);

        void onCommentDeleted(QiscusComment qiscusComment);

        void refreshComment(QiscusComment qiscusComment);

        void notifyDataChanged();

        void updateLastDeliveredComment(long lastDeliveredCommentId);

        void updateLastReadComment(long lastReadCommentId);

        void onFileDownloaded(File file, String mimeType);

        void startPhotoViewer(QiscusComment qiscusComment);

        void onUserTyping(String user, boolean typing);

        void showCommentsAndScrollToTop(List<QiscusComment> qiscusComments);

        void onRealtimeStatusChanged(boolean connected);

        void onLoadCommentsError(Throwable throwable);

        void clearCommentsBefore(long timestamp);

    }
}

