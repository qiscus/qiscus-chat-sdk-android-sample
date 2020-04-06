package com.qiscus.mychatui;


import androidx.multidex.MultiDexApplication;

import com.facebook.stetho.Stetho;
import com.qiscus.jupuk.Jupuk;
import com.qiscus.mychatui.util.Const;
import com.qiscus.mychatui.util.PushNotificationUtil;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.one.EmojiOneProvider;

import static com.qiscus.mychatui.BuildConfig.QISCUS_SDK_APP_ID;
import static com.qiscus.mychatui.BuildConfig.QISCUS_SDK_APP_ID2;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class MyApplication extends MultiDexApplication {
    private static MyApplication instance;

    private AppComponent component;
    private QiscusCore qiscusCore1;
    private QiscusCore qiscusCore2;

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

        Nirmana.init(this);
//        QiscusCore.setup(this, BuildConfig.QISCUS_SDK_APP_ID);

//        QiscusCore.setupWithCustomServer(this, "dragongo", "https://dragongo.qiscus.com",
//                "ssl://realtime-stage.qiscus.com:1885", null);
//        QiscusCore.getChatConfig()
//                .enableDebugMode(true)
//                .setNotificationListener(PushNotificationUtil::showNotification)
//                .setEnableFcmPushNotification(true);

        qiscusCore1 = new QiscusCore();
        qiscusCore1.setup(this, QISCUS_SDK_APP_ID);
        qiscusCore1.getChatConfig()
                .enableDebugMode(true)
                .setNotificationListener(PushNotificationUtil::showNotification)
                .setEnableFcmPushNotification(true);

        qiscusCore2 = new QiscusCore();
//        qiscusCore2.setup(this, QISCUS_SDK_APP_ID2);
        qiscusCore2.setupWithCustomServer(this, "dragongo", "https://dragongo.qiscus.com",
                "ssl://realtime-stage.qiscus.com:1885", null);
        qiscusCore2.getChatConfig()
                .enableDebugMode(true)
                .setNotificationListener(PushNotificationUtil::showNotification)
                .setEnableFcmPushNotification(true);

        Const.setQiscusCore1(qiscusCore1);
        Const.setQiscusCore2(qiscusCore2);
        initEmoji();
        Jupuk.init(this);

        Stetho.initializeWithDefaults(this);

        Const.setQiscusCore(qiscusCore1);
    }

    public AppComponent getComponent() {
        return component;
    }
}
