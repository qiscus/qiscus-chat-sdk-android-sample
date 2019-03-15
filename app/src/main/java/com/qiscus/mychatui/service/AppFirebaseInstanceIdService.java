package com.qiscus.mychatui.service;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.qiscus.sdk.chat.core.QiscusCore;

/**
 * @author Yuana andhikayuana@gmail.com
 * @since Aug, Tue 14 2018 15.21
 **/
public class AppFirebaseInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        // Notify Qiscus about FCM token
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        QiscusCore.setFcmToken(refreshedToken);

        //TODO Application part here
    }
}
