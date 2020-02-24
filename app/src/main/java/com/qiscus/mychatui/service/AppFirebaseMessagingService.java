package com.qiscus.mychatui.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.qiscus.mychatui.MyApplication;
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

        Log.d("Qiscus", "onMessageReceived " + remoteMessage.getData().toString());
        if (QiscusFirebaseMessagingUtil.handleMessageReceived(remoteMessage)) {
            return;
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);

        Log.d("Qiscus", "onNewToken " + s);
        QiscusCore.registerDeviceToken(s);
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

                            QiscusCore.registerDeviceToken(currentToken);
                        }

                    }
                });
    }
}
