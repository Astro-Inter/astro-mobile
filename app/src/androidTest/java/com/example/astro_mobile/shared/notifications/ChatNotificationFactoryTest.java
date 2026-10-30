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
        Notification updated = ChatNotificationFactory.create(context, data, original, null, photo, true);
        assertEquals(1, NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(updated).getMessages().size());
        assertTrue((updated.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0);
        assertFalse((original.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0);
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
