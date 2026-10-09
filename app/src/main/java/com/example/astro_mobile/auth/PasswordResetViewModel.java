package com.example.astro_mobile.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.example.astro_mobile.data.firebase.FirebaseAuthRepository;

// Guarda o e-mail para a confirmação e uma possível nova tentativa.
public class PasswordResetViewModel extends ViewModel {
    public interface ResultCallback {
        void onSuccess();

        void onFailure(AuthFailureKind kind);
    }

    private final FirebaseAuthRepository repository;
    private boolean loading;
    private int requestId;
    private String lastEmail;
    private ResultCallback callback;

    public PasswordResetViewModel(FirebaseAuthRepository repository) {
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
        return repository.getCurrentEmail();
    }

    public void sendPasswordResetEmail(String email, ResultCallback callback) {
        if (loading) {
            return;
        }
        lastEmail = email;
        loading = true;
        this.callback = callback;
        int id = ++requestId;
        repository.sendPasswordResetEmail(email, new FirebaseAuthRepository.ResultCallback() {
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
        // O SDK pode concluir a operação, mas uma tela fechada não recebe seu retorno.
        ++requestId;
        callback = null;
        loading = false;
    }

    @Override
    protected void onCleared() {
        clearRequest();
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final FirebaseAuthRepository repository;

        public Factory(FirebaseAuthRepository repository) {
            this.repository = repository;
        }

        public static Factory createDefault() {
            return new Factory(FirebaseAuthRepository.createDefault());
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(PasswordResetViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new PasswordResetViewModel(repository));
        }
    }
}
