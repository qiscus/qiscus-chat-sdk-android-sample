package com.qiscus.mychatui.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.qiscus.mychatui.R;
import com.qiscus.mychatui.service.NotificationClickReceiver;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.model.QiscusComment;
import com.qiscus.sdk.chat.core.util.BuildVersionUtil;
import com.qiscus.sdk.chat.core.util.QiscusAndroidUtil;
import com.qiscus.sdk.chat.core.util.QiscusNumberUtil;

/**
 * @author Yuana andhikayuana@gmail.com
 * @since Aug, Tue 14 2018 13.09
 **/
public final class PushNotificationUtil {

    private PushNotificationUtil() {
    }

    public static void showNotification(Context context, QiscusComment qiscusComment) {
        if (QiscusCore.getDataStore().isContains(qiscusComment)) {
            return;
        }

        QiscusCore.getDataStore().addOrUpdate(qiscusComment);

        String notificationChannelId = QiscusCore.getApps().getPackageName() + ".qiscus.sdk.notification.channel";
        if (BuildVersionUtil.isOreoOrHigher()) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(notificationChannelId, "Chat", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        PendingIntent pendingIntent;
        Intent openIntent = new Intent(context, NotificationClickReceiver.class);
        openIntent.putExtra("data", qiscusComment);
        pendingIntent = PendingIntent.getBroadcast(context, QiscusNumberUtil.convertToInt(qiscusComment.getRoomId()),
                openIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, notificationChannelId);
        notificationBuilder.setContentTitle(qiscusComment.getSender())
                .setContentIntent(pendingIntent)
                .setContentText(qiscusComment.getMessage())
                .setTicker(qiscusComment.getMessage())
                .setSmallIcon(R.drawable.ic_jupuk_play_icon)
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setGroup("CHAT_NOTIF_" + qiscusComment.getRoomId())
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        QiscusAndroidUtil.runOnUIThread(() -> NotificationManagerCompat.from(context)
                .notify(QiscusNumberUtil.convertToInt(qiscusComment.getRoomId()), notificationBuilder.build()));
    }
}
