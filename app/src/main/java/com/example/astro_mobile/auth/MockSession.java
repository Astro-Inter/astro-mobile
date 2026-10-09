package com.example.astro_mobile.auth;

import android.content.Context;
import android.content.SharedPreferences;

/** Limpa a antiga sessão mock e identifica os perfis conhecidos do Astro. */
public final class MockSession {
    private static final String PREFS = "astro_mock_session";

    private MockSession() {
    }

    public static boolean clear(Context context) {
        // Remove dados deixados pelas versões que simulavam o cadastro.
        return preferences(context).edit().clear().commit();
    }

    public static boolean isKnownUserType(String userType) {
        return "COLABORADOR".equals(userType) || "GESTOR".equals(userType)
                || "GESTOR_WORKSPACE".equals(userType);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
