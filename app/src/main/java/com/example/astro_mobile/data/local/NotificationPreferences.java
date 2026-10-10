package com.example.astro_mobile.data.local;

import android.content.Context;
import android.content.SharedPreferences;

public final class NotificationPreferences {
    private NotificationPreferences() { }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                "astro_notification_preferences", Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context, String userId) {
        // Esta referência local não solicita permissão nem entrega notificações.
        return preferences(context).getBoolean(userId, false);
    }

    public static void save(Context context, String userId, boolean enabled) {
        // Cada conta conserva sua escolha neste aparelho, inclusive após sair.
        preferences(context).edit().putBoolean(userId, enabled).apply();
    }
}
