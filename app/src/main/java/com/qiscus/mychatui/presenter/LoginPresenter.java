package com.qiscus.mychatui.presenter;

import com.qiscus.mychatui.data.source.UserRepository;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class LoginPresenter {
    private View view;
    private UserRepository userRepository;

    public LoginPresenter(View view, UserRepository userRepository) {
        this.view = view;
        this.userRepository = userRepository;
    }

    public void start() {
        userRepository.getCurrentUser(user -> {
            if (user != null) {
                view.showHomePage();
            }
        }, throwable -> view.showErrorMessage(throwable.getMessage()));
    }

    public void login(String name, String email, String password) {
        view.showLoading();
        userRepository.login(name, email, password,
                user -> {
                    view.dismissLoading();
                    view.showHomePage();
                },
                throwable -> {
                    view.dismissLoading();
                    view.showErrorMessage(throwable.getMessage());
                });
    }

    public interface View {
        void showHomePage();

        void showLoading();

        void dismissLoading();

        void showErrorMessage(String errorMessage);
    }
}
