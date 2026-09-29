package com.example.astro_mobile.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/** Sessão local temporária para os fluxos mock, sem senha ou token. */
public final class MockSession {
    private static final String PREFS = "astro_mock_session";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_DESTINATION = "destination";

    public enum Destination { FLOW_CHOICE, EMPLOYEE_HOME }

    private MockSession() {
    }

    public static void save(Context context, String email, String userType,
                            Destination destination) {
        if (email == null || email.isEmpty() || !isKnownUserType(userType)
                || destination == null) {
            return;
        }
        preferences(context).edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_USER_TYPE, userType)
                .putString(KEY_DESTINATION, destination.name())
                .apply();
    }

    @Nullable
    public static State read(Context context) {
        SharedPreferences preferences = preferences(context);
        String email = preferences.getString(KEY_EMAIL, null);
        String userType = preferences.getString(KEY_USER_TYPE, null);
        String savedDestination = preferences.getString(KEY_DESTINATION, null);
        if (email == null || email.isEmpty() || !isKnownUserType(userType)
                || savedDestination == null) {
            return null;
        }
        try {
            Destination destination = Destination.valueOf(savedDestination);
            if (destination == Destination.FLOW_CHOICE && "COLABORADOR".equals(userType)) {
                return null;
            }
            return new State(email, userType, destination);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static boolean clear(Context context) {
        return preferences(context).edit().clear().commit();
    }

    public static boolean isKnownUserType(String userType) {
        return "COLABORADOR".equals(userType) || "GESTOR".equals(userType)
                || "GESTOR_WORKSPACE".equals(userType);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static final class State {
        public final String email;
        public final String userType;
        public final Destination destination;

        private State(String email, String userType, Destination destination) {
            this.email = email;
            this.userType = userType;
            this.destination = destination;
        }
    }
}
