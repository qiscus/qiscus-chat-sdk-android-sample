package com.qiscus.mychatui.presenter;

import com.qiscus.mychatui.data.source.UserRepository;

public class ProfilePresenter {

    private View view;
    private UserRepository userRepository;

    public ProfilePresenter(View view, UserRepository userRepository) {
        this.view = view;
        this.userRepository = userRepository;
    }

    public void logout() {
        userRepository.logout();
        view.logout();
    }


    public interface View {
        void logout();
    }
}

