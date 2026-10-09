package com.example.astro_mobile.auth;

import android.os.Bundle;

import androidx.annotation.Nullable;

public final class AuthArgs {
    public static final String EMAIL = "auth_email";
    public static final String USER_TYPE = "auth_user_type";
    public static final String RESUME_ACTIVATION = "auth_resume_activation";

    private AuthArgs() {
    }

    public static Bundle of(String email, String userType) {
        Bundle args = new Bundle();
        args.putString(EMAIL, email);
        args.putString(USER_TYPE, userType);
        return args;
    }

    public static Bundle copy(@Nullable Bundle source) {
        return source == null ? new Bundle() : new Bundle(source);
    }
}
