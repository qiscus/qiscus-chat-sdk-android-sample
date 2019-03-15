package com.qiscus.mychatui.presenter;

import com.qiscus.mychatui.data.source.ChatRoomRepository;
import com.qiscus.mychatui.data.source.UserRepository;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.data.remote.QiscusApi;

import java.util.Arrays;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class HomePresenter {
    private View view;
    private ChatRoomRepository chatRoomRepository;
    private UserRepository userRepository;

    public HomePresenter(View view, ChatRoomRepository chatRoomRepository, UserRepository userRepository) {
        this.view = view;
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
    }

    public void loadChatRooms() {
        chatRoomRepository.getChatRooms(chatRooms -> view.showChatRooms(chatRooms),
                throwable -> view.showErrorMessage(throwable.getMessage()));
    }

    public void openChatRoom(QiscusChatRoom chatRoom) {
        if (chatRoom.isGroup()) {
            view.showGroupChatRoomPage(chatRoom);
            return;
        }
        view.showChatRoomPage(chatRoom);
    }

    public void createChatRoom() {
        //show contactPage
        //view.showContactPage();

        //while is hardcode to create 1 On 1 chat, will change in next update
        QiscusApi.getInstance().getChatRoom("crowdid94", null, null)
                .subscribeOn(Schedulers.io()) //need to run this task on IO thread
                .observeOn(AndroidSchedulers.mainThread()) //deliver result on main thread or UI thread
                .subscribe(qiscusChatRoom -> {
                    // on success
                    view.showChatRoomPage(qiscusChatRoom);
                }, throwable -> {
                    // on error
                });


        //while is hardcode to create group chat, will change in next update
//        String[] userID = {
//                "crowdid92",
//                "crowdid93",
//                "crowdid95"
//        };
//        QiscusApi.getInstance().createGroupChatRoom("room name 1", Arrays.asList(userID),null,null)
//                .subscribeOn(Schedulers.io()) //need to run this task on IO thread
//                .observeOn(AndroidSchedulers.mainThread()) //deliver result on main thread or UI thread
//                .subscribe(qiscusChatRoom -> {
//                    // on success
//                    view.showGroupChatRoomPage(qiscusChatRoom);
//                }, throwable -> {
//                    // on error
//                });

    }

    public void createGroupChatRoom() {
        view.showSelectContactPage();
    }

    public void logout() {
        userRepository.logout();
        view.showLoginPage();
    }

    public interface View {
        void showChatRooms(List<QiscusChatRoom> chatRooms);

        void showChatRoomPage(QiscusChatRoom chatRoom);

        void showGroupChatRoomPage(QiscusChatRoom chatRoom);

        void showContactPage();

        void showSelectContactPage();

        void showLoginPage();

        void showErrorMessage(String errorMessage);

        void showTesting(QiscusComment qiscusComment);
    }
}
