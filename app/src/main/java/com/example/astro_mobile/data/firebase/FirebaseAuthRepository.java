package com.example.astro_mobile.data.firebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthRepository {
    public interface ResultCallback {
        void onSuccess();

        void onFailure(AuthFailureKind kind);
    }

    public interface SessionCallback {
        void onUser(@Nullable String email);

        void onFailure(AuthFailureKind kind);
    }

    private final FirebaseAuth auth;

    public FirebaseAuthRepository(FirebaseAuth auth) {
        this.auth = auth;
    }

    public static FirebaseAuthRepository createDefault() {
        return new FirebaseAuthRepository(FirebaseAuth.getInstance());
    }

    public void signIn(String email, String password, ResultCallback callback) {
        // O Firebase mantém a sessão; a senha não é guardada pelo aplicativo.
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onFailure(classify(task.getException()));
            }
        });
    }

    public void sendPasswordResetEmail(String email, ResultCallback callback) {
        // A recuperação usa o e-mail já identificado, sem armazenar a senha no app.
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onFailure(classify(task.getException()));
            }
        });
    }

    public void checkCurrentUser(SessionCallback callback) {
        // Aguarda a inicialização do Firebase e confere se a sessão ainda é válida.
        FirebaseAuth.AuthStateListener listener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth currentAuth) {
                currentAuth.removeAuthStateListener(this);
                FirebaseUser user = currentAuth.getCurrentUser();
                if (user == null) {
                    callback.onUser(null);
                    return;
                }
                user.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String email = user.getEmail();
                        if (email == null || email.isEmpty()) {
                            currentAuth.signOut();
                            callback.onUser(null);
                            return;
                        }
                        callback.onUser(email);
                    } else if (task.getException() instanceof FirebaseAuthInvalidUserException
                            || isDisabled(task.getException())) {
                        currentAuth.signOut();
                        callback.onUser(null);
                    } else {
                        callback.onFailure(classify(task.getException()));
                    }
                });
            }
        };
        auth.addAuthStateListener(listener);
    }

    public void signOut() {
        auth.signOut();
    }

    public boolean hasCurrentUser() {
        return auth.getCurrentUser() != null;
    }

    private AuthFailureKind classify(@Nullable Exception error) {
        // Separa credenciais inválidas de falhas de rede e problemas inesperados.
        if (error instanceof FirebaseNetworkException) {
            return AuthFailureKind.CONNECTION;
        }
        if (isDisabled(error)) {
            return AuthFailureKind.DISABLED;
        }
        if (error instanceof FirebaseAuthInvalidCredentialsException
                || error instanceof FirebaseAuthInvalidUserException) {
            return AuthFailureKind.INVALID_CREDENTIALS;
        }
        return AuthFailureKind.INTERNAL;
    }

    private boolean isDisabled(@Nullable Exception error) {
        return error instanceof FirebaseAuthException
                && "ERROR_USER_DISABLED".equals(((FirebaseAuthException) error).getErrorCode());
    }
}
