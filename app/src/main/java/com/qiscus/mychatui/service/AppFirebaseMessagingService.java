package com.qiscus.mychatui.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.qiscus.mychatui.MyApplication;
import com.qiscus.mychatui.util.Const;
/**
 * @author Yuana andhikayuana@gmail.com
 * @since Aug, Tue 14 2018 15.23
 **/
public class AppFirebaseMessagingService extends FirebaseMessagingService {

    private static String TAG = AppFirebaseMessagingService.class.getSimpleName();
    
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        final JsonObject jsonObject = new Gson().fromJson(
                remoteMessage.getData().get("payload"),
                JsonObject.class
        );
    
        final String appId = jsonObject.get("app_code").getAsString();
        if (appId != null) {
            if (appId.equals(Const.qiscusCore1().getAppId())) {
                if (Const.qiscusCore1().getFirebaseMessagingUtil().handleMessageReceived(remoteMessage)) {
                    return;
                }
            } else {
                if (Const.qiscusCore2().getFirebaseMessagingUtil().handleMessageReceived(remoteMessage)) {
                    return;
                }
            }
        } else {
            Log.e(TAG, "onMessageReceived : appId is null");
        }
       
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);

        Log.d(TAG, "onNewToken : " + s);
        Const.qiscusCore1().registerDeviceToken(s);
        Const.qiscusCore2().registerDeviceToken(s);
    }

    public static void registerDeviceToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        final String currentToken = task.getResult();

                        MyApplication.getInstance().getComponent().getUserRepository().
                                setDeviceToken(currentToken);
                        MyApplication.getInstance().getComponent().getUserRepositoryAppId2().
                                setDeviceToken(currentToken);

                        Const.qiscusCore1().registerDeviceToken(currentToken);
                        Const.qiscusCore2().registerDeviceToken(currentToken);
    
                        Log.i(TAG, "registerDeviceToken : " + currentToken);
                    }
                });
    }
    
}
