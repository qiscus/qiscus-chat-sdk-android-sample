package com.qiscus.mychatui;


import androidx.multidex.MultiDexApplication;

import com.qiscus.jupuk.Jupuk;
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

    private static void initEmoji() {
        EmojiManager.install(new EmojiOneProvider());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        component = new AppComponent(this);

//        QiscusCore.initWithCustomServer(this, BuildConfig.QISCUS_SDK_APP_ID,
//                "https://qiscus-lb.stage.halodoc.com",
//                "qiscus-mqtt.stage.halodoc.com",
//                null);
//        QiscusCore.getChatConfig()
//                .enableDebugMode(true);
        Nirmana.init(this);
        QiscusCore.initWithAppId(this, BuildConfig.QISCUS_SDK_APP_ID);

        QiscusCore.getChatConfig()
                .enableDebugMode(true)
                .setNotificationListener(PushNotificationUtil::showNotification)
                .setEnableFcmPushNotification(true);
        initEmoji();
        Jupuk.init(this);
    }

    public AppComponent getComponent() {
        return component;
    }
}
