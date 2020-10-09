package com.qiscus.mychatui.presenter;

import android.widget.Toast;

import com.qiscus.mychatui.data.source.UserRepository;
import com.qiscus.mychatui.util.AvatarUtil;
import com.qiscus.mychatui.util.Const;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


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

    public void login(String email, String password, String name) {
//        loginAppId2();
        view.showLoading();
        userRepository.login(email, password, name,
                user -> {
                    view.dismissLoading();
                    view.showHomePage();
                },
                throwable -> {
                    view.dismissLoading();
                    view.showErrorMessage(throwable.getMessage());
                });
    }

    private void loginAppId2() {
        Const.qiscusCore2()
                .setUser("w@mail.com", "12345678")
                .withUsername("Warmonger")
                .withAvatarUrl(AvatarUtil.generateAvatar("Warmonger"))
                .save()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(qiscusAccount -> {
                    Toast.makeText(Const.qiscusCore2().getApps(), "Login AppId 2 Success As : " + qiscusAccount.getName(), Toast.LENGTH_LONG).show();
                }, throwable -> Toast.makeText(Const.qiscusCore2().getApps(), "Login AppId 2 Failed : " + throwable, Toast.LENGTH_LONG).show());
    }

    public interface View {
        void showHomePage();

        void showLoading();

        void dismissLoading();

        void showErrorMessage(String errorMessage);
    }
}
