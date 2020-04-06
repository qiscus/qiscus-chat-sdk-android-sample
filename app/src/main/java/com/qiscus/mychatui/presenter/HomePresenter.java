package com.qiscus.mychatui.presenter;

import com.qiscus.mychatui.data.source.ChatRoomRepository;
import com.qiscus.mychatui.data.source.UserRepository;
import com.qiscus.sdk.chat.core.data.model.QChatRoom;

import java.util.List;


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

    public void openChatRoom(QChatRoom chatRoom) {
        if (chatRoom.getType().equals("group")) {
            view.showGroupChatRoomPage(chatRoom);
            return;
        }
        view.showChatRoomPage(chatRoom);
    }

    public interface View {
        void showChatRooms(List<QChatRoom> chatRooms);

        void showChatRoomPage(QChatRoom chatRoom);

        void showGroupChatRoomPage(QChatRoom chatRoom);

        void showErrorMessage(String errorMessage);
    }
}
