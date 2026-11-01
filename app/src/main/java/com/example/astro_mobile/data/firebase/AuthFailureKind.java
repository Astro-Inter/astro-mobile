package com.example.astro_mobile.data.firebase;

public enum AuthFailureKind {
    INVALID_CREDENTIALS,
    WEAK_PASSWORD,
    DISABLED,
    RATE_LIMIT,
    CONNECTION,
    RESET_SUPPORT,
    RESET_UNAVAILABLE,
    RESET_UNCERTAIN,
    INTERNAL
}
