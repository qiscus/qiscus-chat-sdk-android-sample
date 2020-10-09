package com.qiscus.mychatui.presenter;

import com.qiscus.mychatui.util.Const;
import com.qiscus.sdk.chat.core.data.model.QChatRoom;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class StartChatWithPresenter {

    private View view;

    public StartChatWithPresenter(View view) {
        this.view = view;
    }

    public void buildChatWith(String email) {
        Const.qiscusCore2().getApi()
                .chatUser(email, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(chatRoom -> {
                   view.openChat(chatRoom);
                }, Throwable::printStackTrace);
    }

    public interface View {
        void openChat(QChatRoom chatRoom);
    }
}
