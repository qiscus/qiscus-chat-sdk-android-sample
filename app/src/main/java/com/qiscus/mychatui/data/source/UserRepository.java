package com.qiscus.mychatui.data.source;

import com.qiscus.mychatui.data.model.User;
import com.qiscus.mychatui.util.Action;
import com.qiscus.sdk.chat.core.data.model.QiscusAccount;

import java.util.List;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public interface UserRepository {
    void login(String email, String password, String name, Action<User> onSuccess, Action<Throwable> onError);

    void getCurrentUser(Action<User> onSuccess, Action<Throwable> onError);

    void getUsers(long page, int limit, String query, Action<List<User>> onSuccess, Action<Throwable> onError);

    void updateProfile(String name, Action<User> onSuccess, Action<Throwable> onError);

    void logout();
}
