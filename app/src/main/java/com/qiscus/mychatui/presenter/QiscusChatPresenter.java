package com.qiscus.mychatui.presenter;

import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.util.Pair;

import com.qiscus.mychatui.R;
import com.qiscus.mychatui.util.Const;
import com.qiscus.mychatui.util.QiscusImageUtil;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QAccount;
import com.qiscus.sdk.chat.core.data.model.QChatRoom;
import com.qiscus.sdk.chat.core.data.model.QMessage;
import com.qiscus.sdk.chat.core.data.model.QParticipant;
import com.qiscus.sdk.chat.core.data.model.QUser;
import com.qiscus.sdk.chat.core.event.QMessageDeletedEvent;
import com.qiscus.sdk.chat.core.event.QMessageReceivedEvent;
import com.qiscus.sdk.chat.core.event.QMessageResendEvent;
import com.qiscus.sdk.chat.core.event.QiscusClearMessageEvent;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private QChatRoom room;
    private QAccount qiscusAccount;
    private Func2<QMessage, QMessage, Integer> commentComparator = (lhs, rhs) -> rhs.getTimestamp().compareTo(lhs.getTimestamp());

    private Map<QMessage, Subscription> pendingTask;

    private QiscusChatRoomEventHandler roomEventHandler;

    public QiscusChatPresenter(View view, QChatRoom room) {
        super(view);
        this.view = view;

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        this.room = room;
        if (this.room.getParticipants().isEmpty()) {
            this.room = Const.qiscusCore().getDataStore().getChatRoom(room.getId());
        }
        qiscusAccount = Const.qiscusCore().getQiscusAccount();
        pendingTask = new HashMap<>();

        roomEventHandler = new QiscusChatRoomEventHandler(Const.qiscusCore(), this.room, this);
    }

    private void commentSuccess(QMessage qiscusComment) {
        pendingTask.remove(qiscusComment);
        qiscusComment.setStatus(QMessage.STATE_SENT);
        QMessage savedQiscusComment = Const.qiscusCore().getDataStore().getComment(qiscusComment.getUniqueId());
        if (savedQiscusComment != null && savedQiscusComment.getStatus() > qiscusComment.getStatus()) {
            qiscusComment.setStatus(savedQiscusComment.getStatus());
        }
        Const.qiscusCore().getDataStore().addOrUpdate(qiscusComment);
    }

    private boolean mustFailed(Throwable throwable, QMessage qiscusComment) {
        //Error response from server
        //Means something wrong with server, e.g user is not member of these room anymore
        return ((throwable instanceof HttpException && ((HttpException) throwable).code() >= 400) ||
                //if throwable from JSONException, e.g response from server not json as expected
                (throwable instanceof JSONException) ||
                // if attachment type
                qiscusComment.isAttachment());
    }

    private void commentFail(Throwable throwable, QMessage qiscusComment) {
        pendingTask.remove(qiscusComment);
        if (!Const.qiscusCore().getDataStore().isContains(qiscusComment)) { //Have been deleted
            return;
        }

        int state = QMessage.STATE_PENDING;
        if (mustFailed(throwable, qiscusComment)) {
//            qiscusComment.setDownloading(false);
            state = QMessage.STATE_FAILED;
        }

        //Kalo ternyata comment nya udah sukses dikirim sebelumnya, maka ga usah di update
        QMessage savedQiscusComment = Const.qiscusCore().getDataStore().getComment(qiscusComment.getUniqueId());
        if (savedQiscusComment != null && savedQiscusComment.getStatus() > QMessage.STATE_SENDING) {
            return;
        }

        //Simpen statenya
        qiscusComment.setStatus(state);
        Const.qiscusCore().getDataStore().addOrUpdate(qiscusComment);
    }

    public void cancelPendingComment(QMessage qiscusComment) {
        if (pendingTask.containsKey(qiscusComment)) {
            Subscription subscription = pendingTask.get(qiscusComment);
            if (!subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
            pendingTask.remove(qiscusComment);
        }
    }

    private void sendComment(QMessage qiscusComment) {
        view.onSendingComment(qiscusComment);
        Subscription subscription = Const.qiscusCore().getApi().sendMessage(qiscusComment)
                .doOnSubscribe(() -> Const.qiscusCore().getDataStore().addOrUpdate(qiscusComment))
                .doOnNext(this::commentSuccess)
                .doOnError(throwable -> commentFail(throwable, qiscusComment))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(commentSend -> {
                    if (commentSend.getChatRoomId() == room.getId()) {
                        view.onSuccessSendComment(commentSend);
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    if (qiscusComment.getChatRoomId() == room.getId()) {
                        view.onFailedSendComment(qiscusComment);
                    }
                });

        pendingTask.put(qiscusComment, subscription);
    }

    public void sendComment(String message) {
        QMessage qMessage = QMessage.generateMessage(room.getId(), message);
        Const.qiscusCore().sendMessage(qMessage, new QiscusCore.OnSendMessageListener() {
            @Override
            public void onSending(QMessage qiscusComment) {
                view.onSendingComment(qiscusComment);
                Const.qiscusCore().getDataStore().addOrUpdate(qiscusComment);
            }

            @Override
            public void onSuccess(QMessage qiscusComment) {
                if (qiscusComment.getChatRoomId() == room.getId()) {
                    commentSuccess(qiscusComment);
                    view.onSuccessSendComment(qiscusComment);
                }
            }

            @Override
            public void onFailed(Throwable t, QMessage qiscusComment) {
                t.printStackTrace();
                if (qiscusComment.getChatRoomId() == room.getId()) {
                    view.onFailedSendComment(qiscusComment);
                }
            }
        });
    }

    public void sendCommentPostBack(String content, String payload) {
        QMessage qiscusComment = null;
        try {
            qiscusComment = QMessage.generatePostBackMessage(room.getId(), content, new JSONObject(payload));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendComment(qiscusComment);
    }

    public void sendReplyComment(String message, QMessage originComment) {
        QMessage qiscusComment = QMessage.generateReplyMessage(room.getId(), message, originComment);
        sendComment(qiscusComment);
        Const.qiscusCore().sendMessage(qiscusComment, new QiscusCore.OnSendMessageListener() {
            @Override
            public void onSending(QMessage qiscusComment) {
                view.onSendingComment(qiscusComment);
                Const.qiscusCore().getDataStore().addOrUpdate(qiscusComment);
            }

            @Override
            public void onSuccess(QMessage qiscusComment) {
                if (qiscusComment.getChatRoomId() == room.getId()) {
                    commentSuccess(qiscusComment);
                    view.onSuccessSendComment(qiscusComment);
                }
            }

            @Override
            public void onFailed(Throwable t, QMessage qiscusComment) {
                t.printStackTrace();
                if (qiscusComment.getChatRoomId() == room.getId()) {
                    view.onFailedSendComment(qiscusComment);
                }
            }
        });
    }

    public void resendComment(QMessage qiscusComment) {
        qiscusComment.setStatus(QMessage.STATE_SENDING);
        qiscusComment.setTimestamp(new Date());
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
        if (QiscusImageUtil.isImage(file) && !file.getName().endsWith(".gif")) {
            try {
                compressedFile = QiscusImageUtil.compressImage(file);
            } catch (NullPointerException e) {
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

        QMessage qiscusComment = QMessage.generateFileAttachmentMessage(room.getId(),
                compressedFile.getPath(), caption, file.getName());
//        qiscusComment.setDownloading(true);
        view.onSendingComment(qiscusComment);

        File finalCompressedFile = compressedFile;

        Const.qiscusCore().sendFileMessage(qiscusComment, finalCompressedFile, new QiscusCore.OnProgressUploadListener() {
            @Override
            public void onSending(QMessage qiscusComment) {
                view.onSendingComment(qiscusComment);
                Const.qiscusCore().getDataStore().addOrUpdate(qiscusComment);
            }

            @Override
            public void onProgress(long progress) {
//                // qiscusComment.setProgress((int) progress);
            }

            @Override
            public void onSuccess(QMessage qiscusComment) {
                if (qiscusComment.getChatRoomId() == room.getId()) {
                    commentSuccess(qiscusComment);
//                    qiscusComment.setDownloading(false);
                    view.onSuccessSendComment(qiscusComment);
                    Const.qiscusCore().getDataStore()
                            .addOrUpdateLocalPath(qiscusComment.getChatRoomId(),
                                    qiscusComment.getId(), finalCompressedFile.getAbsolutePath());
                }
            }

            @Override
            public void onFailed(Throwable t, QMessage qiscusComment1) {
                t.printStackTrace();
                if (qiscusComment1.getChatRoomId() == room.getId()) {
                    view.onFailedSendComment(qiscusComment1);
                }
            }
        });

//        pendingTask.put(qiscusComment, subscription);
    }


    private void resendFile(QMessage qiscusComment) {
        if (qiscusComment.getAttachmentUri().toString().startsWith("http")) { //We forward file message
            forwardFile(qiscusComment);
            return;
        }

        File file = new File(qiscusComment.getAttachmentUri().toString());
        if (!file.exists()) { //File have been removed, so we can not upload it anymore
            // qiscusComment.setDownloading(false);
            qiscusComment.setStatus(QMessage.STATE_FAILED);
            Const.qiscusCore().getDataStore().addOrUpdate(qiscusComment);
            view.onFailedSendComment(qiscusComment);
            return;
        }

        // qiscusComment.setDownloading(true);
        // qiscusComment.setProgress(0);
        Subscription subscription = Const.qiscusCore().getApi()
                .upload(file, percentage -> {
                    // qiscusComment.setProgress((int) percentage)
                })
                .doOnSubscribe(() -> Const.qiscusCore().getDataStore().addOrUpdate(qiscusComment))
                .flatMap(uri -> {
                    qiscusComment.updateAttachmentUrl(uri.toString());
                    return Const.qiscusCore().getApi().sendMessage(qiscusComment);
                })
                .doOnNext(commentSend -> {
                    Const.qiscusCore().getDataStore()
                            .addOrUpdateLocalPath(commentSend.getChatRoomId(), commentSend.getId(), file.getAbsolutePath());
                    // qiscusComment.setDownloading(false);
                    commentSuccess(commentSend);
                })
                .doOnError(throwable -> commentFail(throwable, qiscusComment))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(commentSend -> {
                    if (commentSend.getChatRoomId() == room.getId()) {
                        view.onSuccessSendComment(commentSend);
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    if (qiscusComment.getChatRoomId() == room.getId()) {
                        view.onFailedSendComment(qiscusComment);
                    }
                });

        pendingTask.put(qiscusComment, subscription);
    }

    private void forwardFile(QMessage qiscusComment) {
        // qiscusComment.setProgress(100);
        Subscription subscription = Const.qiscusCore().getApi().sendMessage(qiscusComment)
                .doOnSubscribe(() -> Const.qiscusCore().getDataStore().addOrUpdate(qiscusComment))
                .doOnNext(commentSend -> {
                    // qiscusComment.setDownloading(false);
                    commentSuccess(commentSend);
                })
                .doOnError(throwable -> {
                    // qiscusComment.setDownloading(false);
                    commentFail(throwable, qiscusComment);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(commentSend -> {
                    if (commentSend.getChatRoomId() == room.getId()) {
                        view.onSuccessSendComment(commentSend);
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    if (qiscusComment.getChatRoomId() == room.getId()) {
                        view.onFailedSendComment(qiscusComment);
                    }
                });

        pendingTask.put(qiscusComment, subscription);
    }

    public void deleteComment(QMessage qiscusComment) {
        cancelPendingComment(qiscusComment);
        Const.qiscusCore().getQiscusResendCommentHelper().cancelPendingComment(qiscusComment);

        // this code for delete from local
        QiscusAndroidUtil.runOnBackgroundThread(() -> Const.qiscusCore().getDataStore().delete(qiscusComment));
        if (view != null) {
            view.dismissLoading();
            view.onCommentDeleted(qiscusComment);
        }
        Observable.from(new QMessage[]{qiscusComment})
                .map(QMessage::getUniqueId)
                .toList()
                .flatMap(uniqueIds -> Const.qiscusCore().getApi().deleteMessages(uniqueIds))
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

    private Observable<Pair<QChatRoom, List<QMessage>>> getInitRoomData() {
        return Const.qiscusCore().getApi().getChatRoomWithMessages(room.getId())
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

                    Collections.sort(roomData.second, (lhs, rhs) -> rhs.getTimestamp().compareTo(lhs.getTimestamp()));

                    Const.qiscusCore().getDataStore().addOrUpdate(roomData.first);
                })
                .doOnNext(roomData -> {
                    for (QMessage qiscusComment : roomData.second) {
                        Const.qiscusCore().getDataStore().addOrUpdate(qiscusComment);
                    }
                })
                .subscribeOn(Schedulers.io())
                .onErrorReturn(throwable -> null);
    }

    private Observable<List<QMessage>> getCommentsFromNetwork(long lastCommentId) {
        return Const.qiscusCore().getApi().getPreviousMessagesById(room.getId(), 20, lastCommentId)
                .doOnNext(qiscusComment -> {
                    Const.qiscusCore().getDataStore().addOrUpdate(qiscusComment);
                    qiscusComment.setChatRoomId(room.getId());
                })
                .toSortedList(commentComparator)
                .subscribeOn(Schedulers.io());
    }

    private Observable<List<QMessage>> getLocalComments(int count, boolean forceFailedSendingComment) {
        return Const.qiscusCore().getDataStore().getObservableComments(room.getId(), 2 * count)
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

    public List<QMessage> loadLocalComments(int count) {
        return Const.qiscusCore().getDataStore().getComments(room.getId(), count);
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

    private List<QMessage> cleanFailedComments(List<QMessage> qiscusComments) {
        List<QMessage> comments = new ArrayList<>();
        for (QMessage qiscusComment : qiscusComments) {
            if (qiscusComment.getId() != -1) {
                comments.add(qiscusComment);
            }
        }
        return comments;
    }

    private boolean isValidOlderComments(List<QMessage> qiscusComments, QMessage lastQiscusComment) {
        if (qiscusComments.isEmpty()) return false;

        qiscusComments = cleanFailedComments(qiscusComments);
        boolean containsLastValidComment = qiscusComments.size() <= 0 || lastQiscusComment.getId() == -1;
        int size = qiscusComments.size();

        if (size == 1) {
            return qiscusComments.get(0).getPreviousMessageId() == 0
                    && lastQiscusComment.getPreviousMessageId() == qiscusComments.get(0).getId();
        }

        for (int i = 0; i < size - 1; i++) {
            if (!containsLastValidComment && qiscusComments.get(i).getId() == lastQiscusComment.getPreviousMessageId()) {
                containsLastValidComment = true;
            }

            if (qiscusComments.get(i).getPreviousMessageId() != qiscusComments.get(i + 1).getId()) {
                return false;
            }
        }
        return containsLastValidComment;
    }

    private boolean isValidChainingComments(List<QMessage> qiscusComments) {
        qiscusComments = cleanFailedComments(qiscusComments);
        int size = qiscusComments.size();
        for (int i = 0; i < size - 1; i++) {
            if (qiscusComments.get(i).getPreviousMessageId() != qiscusComments.get(i + 1).getId()) {
                return false;
            }
        }
        return true;
    }

    public void loadOlderCommentThan(QMessage qiscusComment) {
        view.showLoadMoreLoading();
        Const.qiscusCore().getDataStore().getObservableOlderCommentsThan(qiscusComment, room.getId(), 40)
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
                            for (QMessage localComment : comments) {
                                if (localComment.getStatus() <= QMessage.STATE_SENDING) {
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

    private void updateRepliedSender(List<QMessage> comments) {
        for (QMessage comment : comments) {
            if (comment.getType() == QMessage.Type.REPLY) {
                QMessage repliedComment = comment.getReplyTo();
                if (repliedComment != null) {
                    for (QParticipant qParticipant : room.getParticipants()) {
                        if (repliedComment.getSender().getId().equals(qParticipant.getId())) {
                            QUser qUser = new QUser();
                            qUser.setName(qParticipant.getName());
                            qUser.setId(qParticipant.getId());
                            qUser.setAvatarUrl(qParticipant.getAvatarUrl());

                            repliedComment.setSender(qUser);
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

    public void loadCommentsAfter(QMessage comment) {
        Const.qiscusCore().getApi().getNextMessagesById(room.getId(), 20, comment.getId())
                .doOnNext(qiscusComment -> qiscusComment.setChatRoomId(room.getId()))
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
    public void handleRetryCommentEvent(QMessageResendEvent event) {
        if (event.getQiscusComment().getChatRoomId() == room.getId()) {
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    view.refreshComment(event.getQiscusComment());
                }
            });
        }
    }

    @Subscribe
    public void handleDeleteCommentEvent(QMessageDeletedEvent event) {
        if (event.getQiscusComment().getChatRoomId() == room.getId()) {
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
    public void onCommentReceivedEvent(QMessageReceivedEvent event) {
        if (event.getQiscusComment().getChatRoomId() == room.getId()) {
            onGotNewComment(event.getQiscusComment());
        }
    }

    @Subscribe
    public void handleClearCommentsEvent(QiscusClearMessageEvent event) {
        if (event.getRoomId() == room.getId()) {
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    view.clearCommentsBefore(event.getTimestamp());
                }
            });
        }
    }

    private void onGotNewComment(QMessage qiscusComment) {
        if (qiscusComment.getChatRoomId() == room.getId()) {
            QiscusAndroidUtil.runOnBackgroundThread(() -> {
                if (!qiscusComment.getSender().getId().equalsIgnoreCase(qiscusAccount.getId())
                        && Const.qiscusCore().getCacheManager().getLastChatActivity().first) {
                    Const.qiscusCore().getPusherApi().markAsRead(room.getId(), qiscusComment.getId());
                }
            });
            view.onNewComment(qiscusComment);
        }

        if (qiscusComment.getSender().getId().equalsIgnoreCase(qiscusAccount.getId())) {
            QiscusAndroidUtil.runOnBackgroundThread(() -> commentSuccess(qiscusComment));
        } else {
            roomEventHandler.onGotComment(qiscusComment);
        }
    }

    public void downloadFile(final QMessage qiscusComment) {

        File file = Const.qiscusCore().getDataStore().getLocalPath(qiscusComment.getId());
        if (file == null) {
            // qiscusComment.setDownloading(true);
            Const.qiscusCore().getApi()
                    .downloadFile(qiscusComment.getAttachmentUri().toString(), qiscusComment.getAttachmentName(),
                            percentage -> {
                        // qiscusComment.setProgress((int) percentage)
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(bindToLifecycle())
                    .doOnNext(file1 -> {
                        QiscusFileUtil.notifySystem(file1);
                        // qiscusComment.setDownloading(false);
                        Const.qiscusCore().getDataStore().addOrUpdateLocalPath(qiscusComment.getChatRoomId(), qiscusComment.getId(),
                                file1.getAbsolutePath());
                    })
                    .subscribe(file1 -> {
                        view.notifyDataChanged();
                        if (qiscusComment.getType() == QMessage.Type.FILE) {
                            view.onFileDownloaded(file1, MimeTypeMap.getSingleton().getMimeTypeFromExtension(qiscusComment.getExtension()));
                        }
                    }, throwable -> {
                        throwable.printStackTrace();
                        // qiscusComment.setDownloading(false);
                        view.showError(QiscusTextUtil.getString(R.string.qiscus_failed_download_file));
                    });
        } else {
            if (qiscusComment.getType() == QMessage.Type.IMAGE) {
                view.startPhotoViewer(qiscusComment);
            } else {
                view.onFileDownloaded(file, MimeTypeMap.getSingleton().getMimeTypeFromExtension(qiscusComment.getExtension()));
            }
        }
    }

    public void loadUntilComment(QMessage qiscusComment) {
        Const.qiscusCore().getDataStore().getObservableCommentsAfter(qiscusComment, room.getId())
                .map(comments -> comments.contains(qiscusComment) ? comments : new ArrayList<QMessage>())
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
                        Observable.just(new ArrayList<QMessage>()))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(comments -> {
                    if (view != null) {
                        view.showCommentsAndScrollToTop(comments);
                    }
                }, Throwable::printStackTrace);
    }

    public void forward(List<QMessage> forwardComments) {
        for (int i = 0; i < forwardComments.size(); i++) {
            int finalI = i;
            QiscusAndroidUtil.runOnUIThread(() -> {
                QMessage forwardComment = forwardComments.get(finalI);
                QMessage qiscusComment;
                if (forwardComment.getType() == QMessage.Type.IMAGE) {
                    qiscusComment = QMessage.generateFileAttachmentMessage(room.getId(),
                            forwardComment.getAttachmentUri().toString(), forwardComment.getText(),
                            forwardComment.getAttachmentName());
                } else if (forwardComment.getType() == QMessage.Type.FILE) {
                    qiscusComment = QMessage.generateFileAttachmentMessage(room.getId(), forwardComment.getAttachmentUri().toString(),
                            forwardComment.getText(),
                            forwardComment.getAttachmentName());
                } else {
                    qiscusComment = QMessage.generateMessage(room.getId(), forwardComment.getText());
                }
                resendComment(qiscusComment);
            }, i * 100);
        }
    }

    private void clearUnreadCount() {
        room.setUnreadCount(0);
        room.setLastMessage(null);
        Const.qiscusCore().getDataStore().addOrUpdate(room);
    }

    public void detachView() {
        roomEventHandler.detach();
        clearUnreadCount();
        room = null;
        EventBus.getDefault().unregister(this);
    }

    public void deleteCommentsForEveryone(List<QMessage> comments) {
        deleteComments(comments);
    }

    /**
     * @param comments
     */
    private void deleteComments(List<QMessage> comments) {
        view.showDeleteLoading();
        Observable.from(comments)
                .map(QMessage::getUniqueId)
                .toList()
                .flatMap(uniqueIds -> Const.qiscusCore().getApi().deleteMessages(uniqueIds))
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
    public void onChatRoomMemberAdded(QParticipant member) {
        if (!room.getParticipants().contains(member)) {
            room.getParticipants().add(member);
            QiscusAndroidUtil.runOnUIThread(() -> {
                if (view != null) {
                    view.onRoomChanged(room);
                }
            });
        }
    }

    @Override
    public void onChatRoomMemberRemoved(QParticipant member) {
        int x = room.getParticipants().indexOf(member);
        if (x >= 0) {
            room.getParticipants().remove(x);
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

        void initRoomData(QChatRoom qiscusChatRoom, List<QMessage> comments);

        void onRoomChanged(QChatRoom qiscusChatRoom);

        void showComments(List<QMessage> qiscusComments);

        void onLoadMore(List<QMessage> qiscusComments);

        void onSendingComment(QMessage qiscusComment);

        void onSuccessSendComment(QMessage qiscusComment);

        void onFailedSendComment(QMessage qiscusComment);

        void onNewComment(QMessage qiscusComment);

        void onCommentDeleted(QMessage qiscusComment);

        void refreshComment(QMessage qiscusComment);

        void notifyDataChanged();

        void updateLastDeliveredComment(long lastDeliveredCommentId);

        void updateLastReadComment(long lastReadCommentId);

        void onFileDownloaded(File file, String mimeType);

        void startPhotoViewer(QMessage qiscusComment);

        void onUserTyping(String user, boolean typing);

        void showCommentsAndScrollToTop(List<QMessage> qiscusComments);

        void onRealtimeStatusChanged(boolean connected);

        void onLoadCommentsError(Throwable throwable);

        void clearCommentsBefore(long timestamp);

    }
}

