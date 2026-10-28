package com.example.astro_mobile.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.example.astro_mobile.data.firebase.FirebaseAuthRepository;
import com.example.astro_mobile.data.api.PasswordResetRepository;

// Guarda o e-mail para a confirmação e uma possível nova tentativa.
public class PasswordResetViewModel extends ViewModel {
    public interface ResultCallback {
        void onSuccess();

        void onFailure(AuthFailureKind kind);
    }

    private final FirebaseAuthRepository auth;
    private final PasswordResetRepository repository;
    private boolean loading;
    private int requestId;
    private String lastEmail;
    private ResultCallback callback;

    public PasswordResetViewModel(FirebaseAuthRepository auth, PasswordResetRepository repository) {
        this.auth = auth;
        this.repository = repository;
    }

    public boolean isLoading() {
        return loading;
    }

    public String getLastEmail() {
        return lastEmail;
    }

    public String getCurrentEmail() {
        // No perfil, o destinatário é a conta autenticada e não um campo editável.
        return auth.getCurrentEmail();
    }

    public long getRetrySeconds() { return repository.getRetrySeconds(); }

    public void sendPasswordResetEmail(String email, ResultCallback callback) {
        if (loading) {
            return;
        }
        lastEmail = email;
        loading = true;
        this.callback = callback;
        int id = ++requestId;
        repository.request(email, new PasswordResetRepository.ResultCallback() {
            @Override
            public void onSuccess() {
                finish(id, null);
            }

            @Override
            public void onFailure(AuthFailureKind kind) {
                finish(id, kind);
            }
        });
    }

    private void finish(int id, AuthFailureKind failure) {
        if (id != requestId) {
            return;
        }
        loading = false;
        ResultCallback result = callback;
        callback = null;
        if (result == null) {
            return;
        }
        if (failure == null) {
            result.onSuccess();
        } else {
            result.onFailure(failure);
        }
    }

    public void clearRequest() {
        // Uma tela fechada não recebe o retorno da solicitação em andamento.
        ++requestId;
        callback = null;
        loading = false;
    }

    @Override
    protected void onCleared() {
        clearRequest();
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final FirebaseAuthRepository auth;
        private final PasswordResetRepository repository;

        public Factory(FirebaseAuthRepository auth, PasswordResetRepository repository) {
            this.auth = auth;
            this.repository = repository;
        }

        public static Factory createDefault() {
            return new Factory(FirebaseAuthRepository.createDefault(), PasswordResetRepository.createDefault());
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(PasswordResetViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new PasswordResetViewModel(auth, repository));
        }
    }
}
