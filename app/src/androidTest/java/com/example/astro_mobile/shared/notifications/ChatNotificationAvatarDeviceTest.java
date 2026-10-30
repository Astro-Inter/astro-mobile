package com.example.astro_mobile.shared.notifications;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.example.astro_mobile.data.api.HumanChatRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.ChatConversationData;
import com.example.astro_mobile.data.firebase.PushTokenRepository;
import com.google.android.gms.tasks.Tasks;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Diagnóstico opt-in: não envia mensagens; só atualiza um aviso com argumento explícito. */
@RunWith(AndroidJUnit4.class)
public final class ChatNotificationAvatarDeviceTest {
    @Test public void authorizedSenderPhotoCanBeLoaded() throws Exception {
        String sender = InstrumentationRegistry.getArguments().getString("avatar_sender_id");
        org.junit.Assume.assumeTrue("Requires an authorized sender ID", sender != null);
        long senderId = Long.parseLong(sender);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Tasks.await(PushTokenRepository.initialize(context).sessionInitialized(), 5, TimeUnit.SECONDS);
        CompletableFuture<List<ChatConversationData>> response = new CompletableFuture<>();
        HumanChatRepository.createDefault().load(new HumanChatRepository.ResultCallback() {
            @Override public void onSuccess(List<ChatConversationData> conversations) { response.complete(conversations); }
            @Override public void onFailure(FailureKind failure) {
                response.completeExceptionally(new IllegalStateException("Conversation lookup: " + failure));
            }
        });
        ChatConversationData match = null;
        for (ChatConversationData conversation : response.get(25, TimeUnit.SECONDS)) {
            if (Long.valueOf(senderId).equals(conversation.participant.id)) match = conversation;
        }
        assertNotNull("Authorized sender conversation not found", match);
        String url = match.participant.profilePhotoUrl;
        assertNotNull("API returned no sender profile photo URL", url);
        assertEquals("Photo must use HTTPS", "https", Uri.parse(url.trim()).getScheme());
        FutureTarget<Bitmap> target = Glide.with(context).asBitmap().load(url.trim()).timeout(4000)
                .circleCrop().submit(192, 192);
        try {
            Bitmap photo;
            try { photo = target.get(5, TimeUnit.SECONDS); }
            catch (Exception failure) {
                throw new AssertionError("Sender photo download failed: " + failure.getClass().getSimpleName());
            }
            assertNotNull(photo);
            assertEquals(192, photo.getWidth());
            if ("true".equals(InstrumentationRegistry.getArguments().getString("inspect_active_avatar"))) {
                android.app.Notification active = null;
                for (android.service.notification.StatusBarNotification item : context.getSystemService(android.app.NotificationManager.class).getActiveNotifications()) {
                    if ((ChatPushNotifications.CHANNEL + ":" + match.id).equals(item.getTag())) active = item.getNotification();
                }
                assertNotNull("Keep the sender notification active", active);
                java.util.List<android.app.Notification.MessagingStyle.Message> messages = android.app.Notification.MessagingStyle.Message
                        .getMessagesFromBundleArray(active.extras.getParcelableArray(android.app.Notification.EXTRA_MESSAGES));
                assertFalse(messages.isEmpty());
                android.graphics.drawable.Drawable icon = messages.get(messages.size() - 1).getSenderPerson().getIcon().loadDrawable(context);
                assertTrue(icon instanceof android.graphics.drawable.BitmapDrawable);
                Bitmap avatar = ((android.graphics.drawable.BitmapDrawable) icon).getBitmap();
                assertTrue("Native sender avatar differs from the profile photo", photo.sameAs(avatar));
                if ("true".equals(InstrumentationRegistry.getArguments().getString("refresh_active_avatar"))) {
                    String uid = active.extras.getString("astro_chat_notification_uid");
                    String messageId = active.extras.getString("astro_chat_notification_message");
                    ChatPushNotifications.updateAvatar(context, uid, match.id, messageId,
                            photo.copy(Bitmap.Config.ARGB_8888, false));
                }
            }
        } finally { Glide.with(context).clear(target); }
    }
}
