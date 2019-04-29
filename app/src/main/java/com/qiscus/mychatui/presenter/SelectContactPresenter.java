package com.qiscus.mychatui.presenter;

import com.qiscus.mychatui.data.model.User;
import com.qiscus.mychatui.data.source.UserRepository;

import java.util.List;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class SelectContactPresenter {
    private View view;
    private UserRepository userRepository;

    public SelectContactPresenter(View view, UserRepository userRepository) {
        this.view = view;
        this.userRepository = userRepository;
    }

    public void loadContacts(int page, int limit, String query) {
        userRepository.getUsers(page, limit, query, users -> {
                view.showContacts(users);
                }, throwable -> {
                    view.showErrorMessage(throwable.getMessage());
                });
//        userRepository.getUsers(users -> view.showContacts(users),
//                throwable -> view.showErrorMessage(throwable.getMessage()));
    }

    public void selectContacts(List<User> selectedContacts) {
        if (selectedContacts.isEmpty()) {
            view.showErrorMessage("Please select at least one contact!");
            return;
        }
        view.showCreateGroupPage(selectedContacts);
    }

    public interface View {
        void showContacts(List<User> contacts);

        void showCreateGroupPage(List<User> members);

        void showErrorMessage(String errorMessage);
    }
}
