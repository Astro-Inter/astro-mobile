package com.example.astro_mobile.data.firebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthRepository {
    public interface AccountCallback {
        void onSuccess(String firebaseUid);

        void onFailure(AuthFailureKind kind);
    }

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

    public void createAccount(String email, String password, AccountCallback callback) {
        // Cria a identidade Firebase; ativar a conta Astro é uma etapa separada.
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                reportAuthenticatedAccount(email, callback);
            } else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                // Conta existente só pode ser retomada depois de confirmar sua senha.
                signInForActivation(email, password, callback);
            } else {
                callback.onFailure(classify(task.getException()));
            }
        });
    }

    public void signInForActivation(String email, String password, AccountCallback callback) {
        // Não recria a conta nem troca a senha em uma recuperação de cadastro.
        signIn(email, password, new ResultCallback() {
            @Override
            public void onSuccess() {
                reportAuthenticatedAccount(email, callback);
            }

            @Override
            public void onFailure(AuthFailureKind kind) {
                callback.onFailure(kind);
            }
        });
    }

    @Nullable
    public String getUidForEmail(String email) {
        // Nunca usa o UID de uma sessão pertencente a outro e-mail.
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || !email.equalsIgnoreCase(user.getEmail())) {
            return null;
        }
        return user.getUid();
    }

    private void reportAuthenticatedAccount(String email, AccountCallback callback) {
        String uid = getUidForEmail(email);
        if (uid == null) {
            callback.onFailure(AuthFailureKind.INTERNAL);
        } else {
            callback.onSuccess(uid);
        }
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
        if (error instanceof FirebaseAuthWeakPasswordException) {
            return AuthFailureKind.WEAK_PASSWORD;
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
