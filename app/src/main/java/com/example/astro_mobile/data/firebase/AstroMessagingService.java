package com.example.astro_mobile.data.firebase;

import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.example.astro_mobile.shared.notifications.PendingEventNotifications;

public final class AstroMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Sem sessão, o SDK preserva o token; o listener sincroniza depois do login.
        PushTokenRepository.initialize(this).onNewToken();
    }
    @Override public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        PushTokenRepository.initialize(this);
        PendingEventNotifications.show(this, message.getData());
    }
}
