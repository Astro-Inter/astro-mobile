package com.example.astro_mobile.shared.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;
import com.example.astro_mobile.R;
import com.example.astro_mobile.data.api.dto.ChatPushData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class ChatNotificationFactory {
    private static final String RECIPIENT = "astro_chat_notification_uid";
    private static final String LAST_MESSAGE = "astro_chat_notification_message";
    private ChatNotificationFactory() { }

    static boolean isLatest(Notification notification, ChatPushData message) {
        return isLatest(notification, message.uid, message.conversationId, message.messageId);
    }

    static boolean isLatest(Notification notification, String uid, String conversationId, String messageId) {
        return notification != null && uid.equals(notification.extras.getString(RECIPIENT))
                && conversationId.equals(notification.extras.getString(ChatPushNotifications.CONVERSATION))
                && messageId.equals(notification.extras.getString(LAST_MESSAGE));
    }

    private static boolean belongsTo(Notification notification, ChatPushData message) {
        return notification != null && message.uid.equals(notification.extras.getString(RECIPIENT))
                && message.conversationId.equals(notification.extras.getString(ChatPushNotifications.CONVERSATION));
    }

    static Notification create(Context context, ChatPushData message, Notification previous,
                               PendingIntent open, Bitmap photo, boolean avatarUpdate) {
        Bitmap avatar = photo;
        if (avatar == null && belongsTo(previous, message)) {
            NotificationCompat.MessagingStyle prior = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(previous);
            if (prior != null && !prior.getMessages().isEmpty()) {
                Person lastSender = prior.getMessages().get(prior.getMessages().size() - 1).getPerson();
                if (lastSender != null && matchesSender(lastSender.getKey(), message.senderId) && lastSender.getIcon() != null) {
                    Drawable retained = lastSender.getIcon().loadDrawable(context);
                    if (retained instanceof BitmapDrawable) avatar = ((BitmapDrawable) retained).getBitmap();
                }
            }
        }
        if (avatar == null) avatar = initials(context, message.senderName);
        Person sender = new Person.Builder().setName(message.senderName).setKey(Long.toString(message.senderId))
                .setIcon(IconCompat.createWithBitmap(avatar)).build();
        Person self = new Person.Builder().setName(context.getString(R.string.human_reply_you)).setKey(message.uid).build();
        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(self).setGroupConversation(false);
        List<NotificationCompat.MessagingStyle.Message> messages = new ArrayList<>();
        if (belongsTo(previous, message)) {
            NotificationCompat.MessagingStyle prior = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(previous);
            if (prior != null) {
                for (NotificationCompat.MessagingStyle.Message item : prior.getMessages()) {
                    if (item.getTimestamp() >= System.currentTimeMillis() - 86400000L)
                        messages.add(new NotificationCompat.MessagingStyle.Message(item.getText(), item.getTimestamp(), sender));
                }
            }
        }
        if (!isLatest(previous, message))
            messages.add(new NotificationCompat.MessagingStyle.Message(message.preview, message.createdAt.toEpochMilli(), sender));
        messages.sort(Comparator.comparingLong(NotificationCompat.MessagingStyle.Message::getTimestamp));
        for (int index = Math.max(0, messages.size() - 5); index < messages.size(); index++) style.addMessage(messages.get(index));
        Bundle extras = new Bundle();
        extras.putString(RECIPIENT, message.uid);
        extras.putString(LAST_MESSAGE, message.messageId);
        extras.putString(ChatPushNotifications.CONVERSATION, message.conversationId);
        long timestamp = messages.isEmpty() ? message.createdAt.toEpochMilli() : messages.get(messages.size() - 1).getTimestamp();
        return build(context, message.senderName, message.preview, timestamp, open, extras, style, avatar, avatarUpdate);
    }

    private static Notification build(Context context, CharSequence title, CharSequence text, long timestamp,
                                      PendingIntent open, Bundle extras, NotificationCompat.MessagingStyle style,
                                      Bitmap avatar, boolean avatarUpdate) {
        return new NotificationCompat.Builder(context, ChatPushNotifications.CHANNEL)
                .setSmallIcon(R.drawable.ic_push_notification).setLargeIcon(avatar)
                .setColor(context.getColor(R.color.astro_primary_purple))
                .setContentTitle(title).setContentText(text)
                .setStyle(style).setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setWhen(timestamp).setShowWhen(true).setContentIntent(open).setAutoCancel(true)
                .setOnlyAlertOnce(avatarUpdate).setSilent(avatarUpdate).setAllowSystemGeneratedContextualActions(false)
                .addExtras(extras).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(new NotificationCompat.Builder(context, ChatPushNotifications.CHANNEL)
                        .setSmallIcon(R.drawable.ic_push_notification)
                        .setContentTitle(context.getString(R.string.push_chat_private))
                        .setAllowSystemGeneratedContextualActions(false).build()).build();
    }

    static Notification withAvatar(Context context, Notification previous, Bitmap photo) {
        NotificationCompat.MessagingStyle prior = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(previous);
        if (prior == null) return previous;
        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(prior.getUser())
                .setGroupConversation(false);
        for (NotificationCompat.MessagingStyle.Message item : prior.getMessages()) {
            Person sender = item.getPerson();
            if (sender != null) sender = sender.toBuilder()
                    .setKey(sender.getKey().split("#avatar:", 2)[0]).setIcon(IconCompat.createWithBitmap(photo)).build();
            style.addMessage(new NotificationCompat.MessagingStyle.Message(item.getText(), item.getTimestamp(), sender));
        }
        // Copy only application metadata. Recovered notification extras may retain the
        // previous framework MessagingStyle/avatars used by the system notification view.
        Bundle extras = new Bundle();
        extras.putString(RECIPIENT, previous.extras.getString(RECIPIENT));
        extras.putString(LAST_MESSAGE, previous.extras.getString(LAST_MESSAGE));
        extras.putString(ChatPushNotifications.CONVERSATION,
                previous.extras.getString(ChatPushNotifications.CONVERSATION));
        return build(context, previous.extras.getCharSequence(Notification.EXTRA_TITLE),
                previous.extras.getCharSequence(Notification.EXTRA_TEXT), previous.when,
                previous.contentIntent, extras, style, photo, true);
    }

    private static boolean matchesSender(String key, long senderId) {
        String id = Long.toString(senderId);
        return id.equals(key) || (key != null && key.startsWith(id + "#avatar:"));
    }


    private static Bitmap initials(Context context, String name) {
        int size = Math.min(192, Math.round(64 * context.getResources().getDisplayMetrics().density));
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(context.getColor(R.color.astro_primary_purple));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        String[] words = name.trim().split("\\s+");
        String text = new String(Character.toChars(words[0].codePointAt(0)));
        if (words.length > 1) text += new String(Character.toChars(words[words.length - 1].codePointAt(0)));
        paint.setColor(context.getColor(R.color.chat_text));
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(size * 0.36f);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        canvas.drawText(text.toUpperCase(Locale.getDefault()), size / 2f, size / 2f - (metrics.ascent + metrics.descent) / 2, paint);
        return bitmap;
    }
}
