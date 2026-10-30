package com.example.astro_mobile.shared.notifications;

import android.graphics.Bitmap;
import android.service.notification.StatusBarNotification;
import android.app.NotificationChannel;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.app.NotificationManagerCompat;
import com.example.astro_mobile.MainActivity;
import com.example.astro_mobile.R;
import com.example.astro_mobile.data.api.dto.ChatPushData;
import com.example.astro_mobile.data.firebase.PushTokenRepository;
import java.time.Instant;
import java.util.Map;
import org.json.JSONArray;

public final class ChatPushNotifications {
    public static final String CHANNEL = "astro_chat_messages";
    public static final String CONVERSATION = "astro_push_conversation";
    public static final String MESSAGE = "astro_push_message";
    private static final Object LOCK = new Object();
    private static volatile boolean foreground;
    private static final String RECEIPTS = "astro_chat_push_receipts";
    private ChatPushNotifications() { }

    public static void setForeground(boolean value) { foreground = value; }
    public static void createChannel(Context context) {
        context.getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(
                CHANNEL, context.getString(R.string.push_chat_channel), NotificationManager.IMPORTANCE_DEFAULT));
    }
    public static void clear(Context context) {
        synchronized (LOCK) {
            context.getSharedPreferences(RECEIPTS, Context.MODE_PRIVATE).edit().clear().commit();
            context.getSystemService(NotificationManager.class).cancelAll();
        }
    }

    public static void show(Context context, Map<String, String> data) {
        ChatPushData message = ChatPushData.parse(data, Instant.now());
        if (message == null) return;
        PushTokenRepository repository = PushTokenRepository.getInstance();
        boolean displayed = false;
        synchronized (LOCK) {
            if (!repository.isCurrentUser(message.uid)) return;
            android.content.SharedPreferences preferences = context.getSharedPreferences(RECEIPTS, Context.MODE_PRIVATE);
            JSONArray receipts;
            try { receipts = new JSONArray(preferences.getString("messages", "[]")); }
            catch (Exception invalid) { receipts = new JSONArray(); }
            String key = message.uid + ":" + message.messageId;
            JSONArray recent = new JSONArray();
            boolean duplicate = false;
            long now = System.currentTimeMillis();
            for (int index = 0; index < receipts.length(); index++) {
                JSONArray receipt = receipts.optJSONArray(index);
                if (receipt == null || receipt.optLong(1) < now - 86400000L) continue;
                if (key.equals(receipt.optString(0))) duplicate = true;
                recent.put(receipt);
            }
            if (duplicate) return;
            JSONArray bounded = new JSONArray();
            for (int index = Math.max(0, recent.length() - 1023); index < recent.length(); index++)
                bounded.put(recent.optJSONArray(index));
            recent = bounded;
            if (!foreground && !NotificationManagerCompat.from(context).areNotificationsEnabled()) return;
            if (ChatPushPolicy.shouldDisplay(message, repository.isCurrentUser(message.uid), foreground, duplicate)) {
                Intent intent = new Intent(context, MainActivity.class)
                        .putExtra(PendingEventNotifications.RECIPIENT, message.uid)
                        .putExtra(CONVERSATION, message.conversationId).putExtra(MESSAGE, message.messageId)
                        .setData(Uri.parse("astro://chat/" + message.conversationId))
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent open = PendingIntent.getActivity(context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                Notification previous = active(context, message.conversationId);
                Notification notification = ChatNotificationFactory.create(context, message, previous, open, null, false);
                try {
                    NotificationManagerCompat.from(context).notify(CHANNEL + ":" + message.conversationId, 2001, notification);
                    displayed = true;
                } catch (SecurityException denied) { return; }
            }
            recent.put(new JSONArray().put(key).put(now));
            preferences.edit().putString("messages", recent.toString()).commit();
        }
        if (displayed) ChatNotificationAvatarWorker.enqueue(context, message);
    }

    private static Notification active(Context context, String conversationId) {
        for (StatusBarNotification item : context.getSystemService(NotificationManager.class).getActiveNotifications()) {
            if (item.getId() == 2001 && (CHANNEL + ":" + conversationId).equals(item.getTag())) return item.getNotification();
        }
        return null;
    }

    static boolean canUpdateAvatar(Context context, String uid, String conversationId, String messageId) {
        synchronized (LOCK) {
            return !foreground && PushTokenRepository.getInstance().isCurrentUser(uid)
                    && ChatNotificationFactory.isLatest(active(context, conversationId), uid, conversationId, messageId);
        }
    }

    static void updateAvatar(Context context, String uid, String conversationId, String messageId, Bitmap photo) {
        synchronized (LOCK) {
            if (!canUpdateAvatar(context, uid, conversationId, messageId)) return;
            Notification previous = active(context, conversationId);
            try {
                Notification updated = ChatNotificationFactory.withAvatar(context, previous, photo);
                // Some system panels keep the original MessagingStyle avatar when
                // reapplying an existing notification. Repost the same entry silently.
                NotificationManagerCompat.from(context).cancel(CHANNEL + ":" + conversationId, 2001);
                NotificationManagerCompat.from(context).notify(CHANNEL + ":" + conversationId, 2001,
                        updated);
            } catch (SecurityException denied) { }
        }
    }
}
