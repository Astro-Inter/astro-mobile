package com.example.astro_mobile.data.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class FirebaseSessionIdentity implements SessionIdentity {
    private final FirebaseAuth auth;

    public FirebaseSessionIdentity(FirebaseAuth auth) {
        this.auth = auth;
    }

    public static FirebaseSessionIdentity createDefault() {
        return new FirebaseSessionIdentity(FirebaseAuth.getInstance());
    }

    @Override public String currentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    @Override public Runnable observe(Runnable onChanged) {
        FirebaseAuth.AuthStateListener listener = ignored -> onChanged.run();
        auth.addAuthStateListener(listener);
        return () -> auth.removeAuthStateListener(listener);
    }
}
