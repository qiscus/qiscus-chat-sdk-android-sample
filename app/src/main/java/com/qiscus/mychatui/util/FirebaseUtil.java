package com.qiscus.mychatui.util;

import com.qiscus.mychatui.service.AppFirebaseMessagingService;

public class FirebaseUtil {

    public static void sendCurrentToken() {
        AppFirebaseMessagingService.getCurrentDeviceToken();
    }
}
