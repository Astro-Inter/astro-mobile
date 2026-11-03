package com.example.astro_mobile.data.local;

import android.content.Context;

/** Descarta preferências das versões de protótipo sem tocar na sessão Firebase. */
public final class LegacySessionMigration {
    private LegacySessionMigration() { }

    public static void clear(Context context) {
        context.getApplicationContext().getSharedPreferences("astro_mock_session", Context.MODE_PRIVATE)
                .edit().clear().apply();
    }
}
