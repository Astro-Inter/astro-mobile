package com.example.astro_mobile.shared.notifications;

import static org.junit.Assert.*;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import androidx.core.app.NotificationCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.astro_mobile.R;
import com.example.astro_mobile.data.api.dto.ChatPushData;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class ChatNotificationFactoryTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final String conversation = "11111111-2222-4333-8444-555555555555";

    private ChatPushData message(String uid, String text, int seconds) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "chat_message"); data.put("uid", uid);
        data.put("message_id", UUID.randomUUID().toString()); data.put("conversation_id", conversation);
        data.put("sender_id", "2"); data.put("sender_name", "Rosa Maduda");
        data.put("body", text); data.put("created_at", Instant.now().minusSeconds(seconds).toString());
        return ChatPushData.parse(data, Instant.now());
    }

    @Test public void buildsMessagingStyleWithAvatarAndNoActionsOrPublicPreview() {
        ChatPushData data = message("recipient", "Olá", 1);
        Notification notification = ChatNotificationFactory.create(context, data, null, null, null, false);
        NotificationCompat.MessagingStyle style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification);
        assertNotNull(style);
        assertFalse(style.isGroupConversation());
        assertEquals(1, style.getMessages().size());
        assertEquals("Rosa Maduda", style.getMessages().get(0).getPerson().getName().toString());
        assertNotNull(style.getMessages().get(0).getPerson().getIcon());
        assertEquals(Notification.CATEGORY_MESSAGE, notification.category);
        assertTrue(notification.actions == null || notification.actions.length == 0);
        assertFalse(notification.getAllowSystemGeneratedContextualActions());
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility);
        assertEquals(context.getString(R.string.push_chat_private), notification.publicVersion.extras.getString(Notification.EXTRA_TITLE));
        assertNull(notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT));
        assertTrue(ChatNotificationFactory.isLatest(notification, data));
    }

    @Test public void retainsOnlyFiveRecentMessagesInChronologicalOrder() {
        Notification previous = null;
        for (int index = 0; index < 7; index++) {
            previous = ChatNotificationFactory.create(context, message("recipient", "Mensagem " + index, 10 - index),
                    previous, null, null, false);
        }
        NotificationCompat.MessagingStyle style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(previous);
        assertEquals(5, style.getMessages().size());
        assertEquals("Mensagem 2", style.getMessages().get(0).getText().toString());
        assertEquals("Mensagem 6", style.getMessages().get(4).getText().toString());
    }

    @Test public void avatarRefreshKeepsHistoryWithoutDuplicatingOrAlertingAgain() {
        ChatPushData data = message("recipient", "Olá", 1);
        Notification original = ChatNotificationFactory.create(context, data, null, null, null, false);
        Bitmap photo = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        photo.eraseColor(android.graphics.Color.GREEN);
        Notification updated = ChatNotificationFactory.withAvatar(context, original, photo);
        NotificationCompat.MessagingStyle style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(updated);
        assertEquals(1, style.getMessages().size());
        assertTrue(photo.sameAs(senderPhoto(style, 0)));
        assertNotEquals(NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(original)
                .getMessages().get(0).getPerson().getKey(), style.getMessages().get(0).getPerson().getKey());
        java.util.List<Notification.MessagingStyle.Message> nativeMessages = Notification.MessagingStyle.Message.getMessagesFromBundleArray(
                updated.extras.getParcelableArray(Notification.EXTRA_MESSAGES));
        assertEquals(1, nativeMessages.size());
        android.graphics.drawable.Drawable nativeIcon = nativeMessages.get(0).getSenderPerson().getIcon().loadDrawable(context);
        assertTrue(nativeIcon instanceof android.graphics.drawable.BitmapDrawable);
        assertTrue(photo.sameAs(((android.graphics.drawable.BitmapDrawable) nativeIcon).getBitmap()));
        assertEquals("Olá", style.getMessages().get(0).getText().toString());
        assertTrue(ChatNotificationFactory.isLatest(updated, data));
        assertTrue(updated.actions == null || updated.actions.length == 0);
        assertNull(updated.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT));
        assertTrue((updated.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0);
        assertFalse((original.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0);
    }

    @Test public void nextMessageKeepsPhotoAndAnotherAccountDoesNotInheritIt() {
        Bitmap photo = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        photo.eraseColor(android.graphics.Color.GREEN);
        Notification previous = ChatNotificationFactory.create(context, message("recipient", "Primeira", 2), null, null, photo, false);
        Notification next = ChatNotificationFactory.create(context, message("recipient", "Segunda", 1), previous, null, null, false);
        NotificationCompat.MessagingStyle style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(next);
        assertEquals(2, style.getMessages().size());
        assertTrue(photo.sameAs(senderPhoto(style, 1)));
        Notification otherAccount = ChatNotificationFactory.create(context, message("another", "Nova conta", 0), next, null, null, false);
        NotificationCompat.MessagingStyle other = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(otherAccount);
        assertFalse(photo.sameAs(senderPhoto(other, 0)));
    }

    private Bitmap senderPhoto(NotificationCompat.MessagingStyle style, int index) {
        android.graphics.drawable.Drawable drawable = style.getMessages().get(index).getPerson().getIcon().loadDrawable(context);
        assertTrue(drawable instanceof android.graphics.drawable.BitmapDrawable);
        return ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
    }

    @Test public void anotherAccountOrDismissedNotificationStartsFreshHistory() {
        ChatPushData first = message("old-account", "Privado", 1);
        Notification old = ChatNotificationFactory.create(context, first, null, null, null, false);
        ChatPushData current = message("new-account", "Atual", 0);
        Notification notification = ChatNotificationFactory.create(context, current, old, null, null, false);
        NotificationCompat.MessagingStyle style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification);
        assertEquals(1, style.getMessages().size());
        assertEquals("Atual", style.getMessages().get(0).getText().toString());
        assertFalse(ChatNotificationFactory.isLatest(notification, first));
        assertEquals(1, NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(
                ChatNotificationFactory.create(context, current, null, null, null, false)).getMessages().size());
    }
}
