package com.example.astro_mobile.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public final class PushPreferences {
    public static final String NAME = "astro_push";
    private final SharedPreferences preferences;
    public PushPreferences(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }
    public synchronized String deviceId() {
        String id = preferences.getString("device_id", null);
        if (id == null) { id = UUID.randomUUID().toString(); preferences.edit().putString("device_id", id).commit(); }
        return id;
    }
    public String uid() { return preferences.getString("firebase_uid", null); }
    public boolean logoutPending() { return preferences.getBoolean("logout_pending", false); }
    public String logoutUid() { return preferences.getString("logout_uid", uid()); }
    public void beginLogout(String uid) {
        preferences.edit().putBoolean("logout_pending", true).putString("logout_uid", uid).commit();
    }
    public boolean needsSync(String uid, String token, long now) {
        return PushSyncPolicy.needsSync(uid, token, uid(), preferences.getString("fcm_token", null),
                preferences.getLong("last_sync", 0), now);
    }
    public void synchronizedToken(String uid, String token, long now) {
        preferences.edit().putString("firebase_uid", uid).putString("fcm_token", token).putLong("last_sync", now).apply();
    }
    public void clearAssociation() {
        preferences.edit().remove("firebase_uid").remove("fcm_token").remove("last_sync")
                .remove("logout_uid").remove("logout_pending").apply();
    }
    public long lastCycle(String uid) { return preferences.getLong("cycle:" + uid, 0); }
    public void displayed(String uid, long cycle) { preferences.edit().putLong("cycle:" + uid, cycle).commit(); }
    public boolean permissionAsked() { return preferences.getBoolean("permission_asked", false); }
    public void permissionAsked(boolean value) { preferences.edit().putBoolean("permission_asked", value).apply(); }
}
