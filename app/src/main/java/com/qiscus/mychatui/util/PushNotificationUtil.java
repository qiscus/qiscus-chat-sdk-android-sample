package com.qiscus.mychatui.util;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.annotation.RequiresPermission;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yuana andhikayuana@gmail.com
 * @since Aug, Tue 14 2018 13.09
 **/
public final class PushNotificationUtil {

    private PushNotificationUtil() {
    }

    private static final Map<String, List<String>> roomMessages = new HashMap<>();
    private static final int MAX_MESSAGES = 5; // maksimal pesan ditampilkan per room (FIFO)

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public static void showNotification(Context context, QiscusComment qiscusComment) {
        if (QiscusCore.getDataStore().isContains(qiscusComment)) {
            return;
        }

        QiscusCore.getDataStore().addOrUpdate(qiscusComment);

        String notificationChannelId = QiscusCore.getApps().getPackageName() + ".qiscus.sdk.notification.channel";
        if (BuildVersionUtil.isOreoOrHigher()) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(notificationChannelId, "Chat", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        // ==== Simpan pesan ke Map (FIFO per roomId) ====
        String roomId = ""+qiscusComment.getRoomId();
        String groupKey = "CHAT_ROOM_" + roomId;

        List<String> messages = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            messages = roomMessages.getOrDefault(roomId, new ArrayList<>());
        }else{
            if (roomMessages.containsKey(roomId)) {
                messages = roomMessages.get(roomId);
            } else {
                messages = new ArrayList<>();
            }
        }
        messages.add(qiscusComment.getSender() + ": " + qiscusComment.getMessage());

        // FIFO: jaga hanya MAX_MESSAGES terakhir
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
        roomMessages.put(roomId, messages);

        // ==== Bikin InboxStyle isi pesan2 terbaru ====
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                .setBigContentTitle(qiscusComment.getRoomName());
        for (String msg : messages) {
            inboxStyle.addLine(msg);
        }

        // ==== PendingIntent untuk buka chat ====
        PendingIntent pendingIntent;
        Intent openIntent = new Intent(context, NotificationClickReceiver.class);
        openIntent.putExtra("data", qiscusComment);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    QiscusNumberUtil.convertToInt(qiscusComment.getRoomId()),
                    openIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT
            );
        } else {
            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    QiscusNumberUtil.convertToInt(qiscusComment.getRoomId()),
                    openIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        }

        // ==== Build summary notif per room (replace lama dengan baru) ====
        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, notificationChannelId)
                .setContentTitle(qiscusComment.getRoomName())
                .setContentText(messages.size() + " pesan baru")
                .setSmallIcon(R.drawable.logo)
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setStyle(inboxStyle)
                .setContentIntent(pendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // ID unik per room → notifikasi lama diganti FIFO
        int summaryId = QiscusNumberUtil.convertToInt(qiscusComment.getRoomId());
        notificationManager.notify(summaryId, summaryBuilder.build());
    }

    // Opsional: clear kalau user sudah buka chat
    public static void clearRoom(String roomId) {
        roomMessages.remove(roomId);
    }
}
