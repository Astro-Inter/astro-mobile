package com.example.astro_mobile.data.local;

public final class PushSyncPolicy {
    public static final long REFRESH_MILLIS = 7L * 24 * 60 * 60 * 1000;
    private PushSyncPolicy() { }
    public static boolean needsSync(String uid, String token, String savedUid, String savedToken, long last, long now) {
        return uid != null && token != null && !token.isEmpty()
                && (!uid.equals(savedUid) || !token.equals(savedToken) || last <= 0 || now < last || now - last >= REFRESH_MILLIS);
    }
}
