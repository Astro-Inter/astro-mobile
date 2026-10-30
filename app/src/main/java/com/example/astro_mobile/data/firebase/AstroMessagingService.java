package com.example.astro_mobile.data.firebase;

import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.TimeUnit;
import com.example.astro_mobile.shared.notifications.PendingEventNotifications;

public final class AstroMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Sem sessão, o SDK preserva o token; o listener sincroniza depois do login.
        PushTokenRepository.initialize(this).onNewToken();
    }
    @Override public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        // O FCM pode iniciar um processo encerrado antes de a sessão local ser restaurada.
        try { Tasks.await(PushTokenRepository.initialize(this).sessionInitialized(), 5, TimeUnit.SECONDS); }
        catch (Exception unavailable) { return; }
        if ("chat_message".equals(message.getData().get("type"))) {
            com.example.astro_mobile.shared.notifications.ChatPushNotifications.show(this, message.getData());
        } else PendingEventNotifications.show(this, message.getData());
    }
}
