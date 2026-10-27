package com.example.astro_mobile.notifications;

import android.content.Context;

import com.example.astro_mobile.R;

public final class NotificationTimeFormatter {
    private NotificationTimeFormatter() { }

    // Exibe horas até 23h e, depois, dias completos de 24 horas.
    public static String format(Context context, long hours) {
        if (hours < 24) {
            return context.getString(hours == 0 ? R.string.notifications_recent
                    : R.string.notifications_hours, hours);
        }
        int days = (int) (hours / 24);
        return context.getResources().getQuantityString(R.plurals.notifications_days, days, days);
    }
}
