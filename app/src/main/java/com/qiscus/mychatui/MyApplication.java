package com.qiscus.mychatui;

import android.support.multidex.MultiDexApplication;

import com.qiscus.mychatui.util.PushNotificationUtil;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.one.EmojiOneProvider;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class MyApplication extends MultiDexApplication {
    private static MyApplication instance;

    private AppComponent component;

    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        component = new AppComponent(this);
        Nirmana.init(this);
        QiscusCore.init(this, BuildConfig.QISCUS_SDK_APP_ID);
        QiscusCore.getChatConfig()
                .setEnableLog(true)
                .setNotificationListener(PushNotificationUtil::showNotification)
                .setEnableFcmPushNotification(true);
        initEmoji();
    }

    private static void initEmoji() {
        EmojiManager.install(new EmojiOneProvider());
    }

    public AppComponent getComponent() {
        return component;
    }
}
