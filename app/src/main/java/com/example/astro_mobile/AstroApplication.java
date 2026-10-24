package com.example.astro_mobile;

import android.app.Application;
import com.example.astro_mobile.data.firebase.PushTokenRepository;
import com.example.astro_mobile.shared.notifications.PendingEventNotifications;

public final class AstroApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        PendingEventNotifications.createChannel(this);
        PushTokenRepository.initialize(this);
    }
}
