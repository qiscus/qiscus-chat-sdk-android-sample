package com.qiscus.mychatui.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.qiscus.mychatui.MyApplication;
import com.qiscus.mychatui.util.Const;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.util.QiscusFirebaseMessagingUtil;

/**
 * @author Yuana andhikayuana@gmail.com
 * @since Aug, Tue 14 2018 15.23
 **/
public class AppFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Gson gson = new Gson();
        gson.toJson(remoteMessage.getData().get("payload"));

        JsonObject jsonObject = new Gson().fromJson(remoteMessage.getData().get("payload"), JsonObject.class);

        if (jsonObject.get("app_code").toString().equals(Const.qiscusCore1().getAppId())) {
            if (Const.qiscusCore1().getFirebaseMessagingUtil().handleMessageReceived(remoteMessage)) {
                return;
            }
        } else {
            if (Const.qiscusCore2().getFirebaseMessagingUtil().handleMessageReceived(remoteMessage)) {
                return;
            }
        }

    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);

        Log.d("Qiscus", "onNewToken " + s);
        Const.qiscusCore1().registerDeviceToken(s);
        Const.qiscusCore2().registerDeviceToken(s);
    }

    public static void getCurrentDeviceToken() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.e("Qiscus", "getCurrentDeviceToken Failed : " +
                                    task.getException());
                            return;
                        }

                        if (task.getResult() != null) {
                            String currentToken = task.getResult().getToken();

                            MyApplication.getInstance().getComponent().getUserRepository().
                                    setDeviceToken(currentToken);
                            MyApplication.getInstance().getComponent().getUserRepositoryAppId2().
                                    setDeviceToken(currentToken);

                            Const.qiscusCore1().registerDeviceToken(currentToken);
                            Const.qiscusCore2().registerDeviceToken(currentToken);
                        }

                    }
                });
    }
}
