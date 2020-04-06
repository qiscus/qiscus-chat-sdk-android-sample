package com.qiscus.mychatui;

import android.content.Context;

import com.qiscus.mychatui.data.source.ChatRoomRepository;
import com.qiscus.mychatui.data.source.UserRepository;
import com.qiscus.mychatui.data.source.impl.ChatRoomRepositoryImpl;
import com.qiscus.mychatui.data.source.impl.UserRepositoryImpl;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class AppComponent {
    private final UserRepository userRepository;
    private final UserRepository userRepositoryAppId2;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomRepository chatRoomRepositoryAppId2;

    AppComponent(Context context) {
        userRepository = new UserRepositoryImpl(context);
        userRepositoryAppId2 = new UserRepositoryImpl(context);
        chatRoomRepository = new ChatRoomRepositoryImpl();
        chatRoomRepositoryAppId2 = new ChatRoomRepositoryImpl();
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public UserRepository getUserRepositoryAppId2() {
        return userRepositoryAppId2;
    }

    public ChatRoomRepository getChatRoomRepository() {
        return chatRoomRepository;
    }

    public ChatRoomRepository getChatRoomRepositoryAppId2() {
        return chatRoomRepositoryAppId2;
    }
}
