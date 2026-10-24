package com.example.astro_mobile.shared.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.astro_mobile.MainActivity;
import com.example.astro_mobile.R;
import com.example.astro_mobile.data.firebase.PushTokenRepository;
import com.example.astro_mobile.data.local.PushPreferences;
import java.util.Map;

public final class PendingEventNotifications {
    public static final String CHANNEL = "astro_pending_events";
    public static final String RECIPIENT = "astro_push_uid";
    private static final Object LOCK = new Object();
    private PendingEventNotifications() { }
    public static void createChannel(Context context) {
        context.getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(
                CHANNEL, context.getString(R.string.push_pending_title), NotificationManager.IMPORTANCE_DEFAULT));
    }
    public static void show(Context context, Map<String, String> data) {
        PushTokenRepository repository = PushTokenRepository.getInstance();
        String uid = data.get("uid");
        long cycle;
        try { cycle = Long.parseLong(data.getOrDefault("cycle_id", "0")); } catch (NumberFormatException error) { return; }
        synchronized (LOCK) {
            PushPreferences preferences = repository.preferences();
            if (!PushMessagePolicy.shouldDisplay(data.get("type"), uid, repository.isCurrentUser(uid), cycle,
                    uid == null ? 0 : preferences.lastCycle(uid))) return;
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
            Intent intent = new Intent(context, MainActivity.class).putExtra(RECIPIENT, uid)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent open = PendingIntent.getActivity(context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_push_notification).setContentTitle(context.getString(R.string.push_pending_title))
                    .setContentText(context.getString(R.string.push_pending_body)).setContentIntent(open)
                    .setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
            try {
                NotificationManagerCompat.from(context).notify(CHANNEL, 1001, notification.build());
                preferences.displayed(uid, cycle);
            } catch (SecurityException ignored) { /* A permissão pode ter mudado durante o recebimento. */ }
        }
    }
}
