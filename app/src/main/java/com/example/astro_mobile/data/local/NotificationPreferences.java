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
        // Contas sem escolha explícita mantêm o comportamento de push já existente.
        return userId != null && preferences(context).getBoolean(userId, true);
    }

    public static void save(Context context, String userId, boolean enabled) {
        if (userId == null) return;
        // Cada conta conserva sua escolha neste aparelho, inclusive após sair.
        preferences(context).edit().putBoolean(userId, enabled).apply();
    }
}
