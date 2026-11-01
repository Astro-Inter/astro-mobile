package com.example.astro_mobile.auth;

import android.content.Context;

import com.example.astro_mobile.R;
import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Orientações para uma tentativa manual, sem expor os dados da solicitação. */
public final class PasswordResetMessages {
    private PasswordResetMessages() { }

    public static void show(Context context, AuthFailureKind kind, String requestId) {
        int resource = kind == AuthFailureKind.RESET_SUPPORT ? R.string.password_reset_support
                : kind == AuthFailureKind.RESET_UNCERTAIN ? R.string.password_reset_uncertain
                : kind == AuthFailureKind.CONNECTION ? R.string.password_reset_connection
                : R.string.password_reset_unavailable;
        String message = context.getString(resource);
        if (requestId != null) message += "\n" + context.getString(R.string.password_reset_request_id, requestId);
        new MaterialAlertDialogBuilder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
