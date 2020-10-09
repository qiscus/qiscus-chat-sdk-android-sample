package com.qiscus.mychatui.data.source.impl;

import com.qiscus.mychatui.data.model.User;
import com.qiscus.mychatui.data.source.ChatRoomRepository;
import com.qiscus.mychatui.util.Action;
import com.qiscus.mychatui.util.AvatarUtil;
import com.qiscus.mychatui.util.Const;
import com.qiscus.sdk.chat.core.data.model.QChatRoom;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.Observable;


/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class ChatRoomRepositoryImpl implements ChatRoomRepository {

    @Override
    public void getChatRooms(Action<List<QChatRoom>> onSuccess, Action<Throwable> onError) {
        Observable.fromIterable(Const.qiscusCore().getDataStore().getChatRooms(100))
                .filter(chatRoom -> chatRoom.getLastMessage() != null)
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onSuccess::call, onError::call);

        Const.qiscusCore().getApi()
                .getAllChatRooms(true, false, true, 1, 100)
                .flatMap(Observable::fromIterable)
                .doOnNext(qiscusChatRoom -> Const.qiscusCore().getDataStore().addOrUpdate(qiscusChatRoom))
                .filter(chatRoom -> chatRoom.getLastMessage().getId() != 0)
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onSuccess::call, onError::call);
    }

    @Override
    public void createChatRoom(User user, Action<QChatRoom> onSuccess, Action<Throwable> onError) {
        QChatRoom savedChatRoom = Const.qiscusCore().getDataStore().getChatRoom(user.getId());
        if (savedChatRoom != null) {
            onSuccess.call(savedChatRoom);
            return;
        }

        Const.qiscusCore().getApi()
                .chatUser(user.getId(), null)
                .doOnNext(chatRoom -> Const.qiscusCore().getDataStore().addOrUpdate(chatRoom))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onSuccess::call, onError::call);
    }

    @Override
    public void createGroupChatRoom(String name, List<User> members, Action<QChatRoom> onSuccess, Action<Throwable> onError) {
        List<String> ids = new ArrayList<>();
        for (User member : members) {
            ids.add(member.getId());
        }

        Const.qiscusCore().getApi()
                .createGroupChat(name, ids, AvatarUtil.generateAvatar(name), null)
                .doOnNext(chatRoom -> Const.qiscusCore().getDataStore().addOrUpdate(chatRoom))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onSuccess::call, onError::call);
    }

    @Override
    public void addParticipant(long roomId, List<User> members, Action<Void> onSuccess, Action<Throwable> onError) {
        List<String> ids = new ArrayList<>();
        for (User member : members) {
            ids.add(member.getId());
        }
        Const.qiscusCore().getApi().addParticipants(roomId, ids)
                .doOnNext(chatRoom -> Const.qiscusCore().getDataStore().addOrUpdate(chatRoom))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(chatRoom -> onSuccess.call(null), onError::call);
    }

    @Override
    public void removeParticipant(long roomId, List<User> members, Action<Void> onSuccess, Action<Throwable> onError) {
        List<String> ids = new ArrayList<>();
        for (User member : members) {
            ids.add(member.getId());
        }
        Const.qiscusCore().getApi().addParticipants(roomId, ids)
                .doOnNext(chatRoom -> Const.qiscusCore().getDataStore().addOrUpdate(chatRoom))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(chatRoom -> onSuccess.call(null), onError::call);
    }

}
