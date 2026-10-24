package com.example.astro_mobile.data.firebase;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import com.example.astro_mobile.data.local.PushPreferences;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class PushTokenRepository {
    private static PushTokenRepository instance;
    private final Context context;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final FirebaseMessaging messaging;
    private final PushPreferences preferences;
    private Task<Void> registration = Tasks.forResult(null);
    private Task<Void> cleanup = Tasks.forResult(null);
    private boolean resyncRequested;
    private final TaskCompletionSource<Void> authReady = new TaskCompletionSource<>();

    private PushTokenRepository(Context context) {
        this.context = context.getApplicationContext();
        auth = FirebaseAuth.getInstance(); firestore = FirebaseFirestore.getInstance();
        messaging = FirebaseMessaging.getInstance(); preferences = new PushPreferences(context);
        auth.addAuthStateListener(current -> {
            authReady.trySetResult(null);
            if (preferences.logoutPending()) completePendingLogout();
            else if (current.getCurrentUser() != null) synchronize();
        });
    }
    public static synchronized PushTokenRepository initialize(Context context) {
        if (instance == null) instance = new PushTokenRepository(context);
        return instance;
    }
    public static PushTokenRepository getInstance() {
        if (instance == null) throw new IllegalStateException("Push repository not initialized");
        return instance;
    }
    public boolean logoutPending() { return preferences.logoutPending(); }
    public Task<Void> sessionInitialized() { return authReady.getTask(); }
    public boolean isCurrentUser(String uid) {
        return uid != null && !preferences.logoutPending() && auth.getCurrentUser() != null
                && uid.equals(auth.getCurrentUser().getUid());
    }
    public boolean permissionAsked() { return preferences.permissionAsked(); }
    public void markPermissionAsked() { preferences.permissionAsked(true); }
    public PushPreferences preferences() { return preferences; }

    public synchronized Task<Void> synchronize() {
        if (!registration.isComplete()) { resyncRequested = true; return registration; }
        if (preferences.logoutPending() || auth.getCurrentUser() == null) return Tasks.forResult(null);
        String uid = auth.getCurrentUser().getUid();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Tasks.forResult(null);
        }
        registration = messaging.getToken().onSuccessTask(token -> {
            if (!isCurrentUser(uid) || !preferences.needsSync(uid, token, System.currentTimeMillis())) return Tasks.forResult(null);
            Map<String, Object> fields = new HashMap<>();
            fields.put("token", token); fields.put("updated_at", FieldValue.serverTimestamp());
            return Tasks.withTimeout(firestore.collection("fcm_tokens").document(uid).collection("devices")
                    .document(preferences.deviceId()).set(fields), 10, TimeUnit.SECONDS).onSuccessTask(unused -> {
                if (isCurrentUser(uid)) preferences.synchronizedToken(uid, token, System.currentTimeMillis());
                return Tasks.forResult(null);
            });
        });
        registration.addOnCompleteListener(result -> {
            synchronized (PushTokenRepository.this) {
                if (resyncRequested) { resyncRequested = false; synchronize(); }
            }
        });
        // Falhas ficam pendentes: nenhum cache é marcado como sincronizado antes do ACK.
        return registration;
    }
    public void onNewToken() { synchronize(); }
    public synchronized Task<Void> beginLogout() {
        String uid = auth.getCurrentUser() == null ? preferences.uid() : auth.getCurrentUser().getUid();
        preferences.beginLogout(uid);
        androidx.core.app.NotificationManagerCompat.from(context).cancel("astro_pending_events", 1001);
        return completePendingLogout();
    }
    public synchronized Task<Void> completePendingLogout() {
        if (!preferences.logoutPending()) return Tasks.forResult(null);
        if (!cleanup.isComplete()) return cleanup;
        String previousUid = preferences.logoutUid();
        boolean canRemove = previousUid != null && auth.getCurrentUser() != null
                && previousUid.equals(auth.getCurrentUser().getUid());
        Task<Void> removal = canRemove ? Tasks.withTimeout(firestore.collection("fcm_tokens").document(previousUid)
                .collection("devices").document(preferences.deviceId()).delete(), 10, TimeUnit.SECONDS) : Tasks.forResult(null);
        cleanup = removal.continueWithTask(result -> {
            // Se a identidade foi revogada, invalidar o token também impede entrega à próxima conta.
            if (!result.isSuccessful() && auth.getCurrentUser() != null) return Tasks.forException(result.getException());
            return Tasks.withTimeout(messaging.deleteToken(), 10, TimeUnit.SECONDS);
        }).onSuccessTask(unused -> {
            auth.signOut(); preferences.clearAssociation(); return Tasks.forResult(null);
        });
        return cleanup;
    }
}
