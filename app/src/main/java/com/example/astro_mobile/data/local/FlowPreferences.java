package com.example.astro_mobile.data.local;

import android.content.Context;
import android.content.SharedPreferences;

public final class FlowPreferences {
    private static final String PREFS = "astro_flow";
    private static final String KEY_LAST_FLOW = "last_flow";
    private static final String EMPLOYEE = "EMPLOYEE";

    private FlowPreferences() {
    }

    public static boolean wasEmployeeFlow(Context context) {
        // A preferência só escolhe a tela inicial, nunca autoriza o perfil.
        return EMPLOYEE.equals(preferences(context).getString(KEY_LAST_FLOW, null));
    }

    public static void saveEmployeeFlow(Context context) {
        preferences(context).edit().putString(KEY_LAST_FLOW, EMPLOYEE).apply();
    }

    public static void clear(Context context) {
        preferences(context).edit().clear().apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
