package com.qiscus.mychatui.presenter;

import android.widget.Toast;

import com.qiscus.mychatui.data.source.UserRepository;
import com.qiscus.mychatui.util.AvatarUtil;
import com.qiscus.mychatui.util.Const;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class LoginPresenterAppId2 {
    private View view;
    private UserRepository userRepository;

    public LoginPresenterAppId2(View view, UserRepository userRepository) {
        this.view = view;
        this.userRepository = userRepository;
    }

    public void start() {
        if (Const.qiscusCore2().hasSetupUser()) {
            view.showHomePage();
        }
    }

    public void login(String email, String password, String name) {
        loginAppId2(email, password, name);
        view.showLoading();
//        userRepository.login(email, password, name,
//                user -> {
//                    view.dismissLoading();
//                    view.showHomePage();
//                },
//                throwable -> {
//                    view.dismissLoading();
//                    view.showErrorMessage(throwable.getMessage());
//                });
    }

    private void loginAppId2(String email, String password, String name) {
        Const.qiscusCore2()
                .setUser(email, password)
                .withUsername(name)
                .withAvatarUrl(AvatarUtil.generateAvatar("Warmonger"))
                .save()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(qiscusAccount -> {
                    view.dismissLoading();
                    view.showHomePage();
//                    Toast.makeText(Const.qiscusCore2().getApps(), "Login AppId 2 Success As : " + qiscusAccount.getUsername(), Toast.LENGTH_LONG).show();
                }, throwable -> {
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
