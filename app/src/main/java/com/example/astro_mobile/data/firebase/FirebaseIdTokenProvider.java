package com.example.astro_mobile.data.firebase;

import com.example.astro_mobile.data.ai.IdTokenProvider;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;

/** O SDK renova tokens expirados; nenhum token é salvo ou registrado pelo app. */
public final class FirebaseIdTokenProvider implements IdTokenProvider {
    private final FirebaseAuth auth;

    public FirebaseIdTokenProvider(FirebaseAuth auth) {
        this.auth = auth;
    }

    public static FirebaseIdTokenProvider createDefault() {
        return new FirebaseIdTokenProvider(FirebaseAuth.getInstance());
    }

    @Override
    public void getToken(boolean forceRefresh, Callback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SESSION);
            return;
        }
        user.getIdToken(forceRefresh).addOnCompleteListener(task -> {
            // Uma resposta de uma sessão encerrada não pode autenticar outra pessoa.
            FirebaseUser current = auth.getCurrentUser();
            if (current == null || !current.getUid().equals(user.getUid())) {
                callback.onFailure(Failure.SESSION);
            } else if (task.isSuccessful() && task.getResult() != null
                    && task.getResult().getToken() != null
                    && !task.getResult().getToken().isEmpty()) {
                callback.onToken(task.getResult().getToken());
            } else if (task.getException() instanceof FirebaseNetworkException) {
                callback.onFailure(Failure.CONNECTION);
            } else if (task.getException() instanceof FirebaseAuthInvalidUserException
                    || task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                callback.onFailure(Failure.SESSION);
            } else {
                callback.onFailure(Failure.INTERNAL);
            }
        });
    }
}
