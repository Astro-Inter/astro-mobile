package com.example.astro_mobile.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.example.astro_mobile.data.firebase.FirebaseAuthRepository;

public class PasswordLoginViewModel extends ViewModel {
    public interface ResultCallback {
        void onSuccess();

        void onFailure(AuthFailureKind kind);
    }

    private final FirebaseAuthRepository repository;
    private boolean loading;
    private int requestId;
    private ResultCallback callback;

    public PasswordLoginViewModel(FirebaseAuthRepository repository) {
        this.repository = repository;
    }

    public boolean isLoading() {
        return loading;
    }

    public void signIn(String email, String password, ResultCallback callback) {
        if (loading) {
            return;
        }
        loading = true;
        this.callback = callback;
        int id = ++requestId;
        repository.signIn(email, password, new FirebaseAuthRepository.ResultCallback() {
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
            // Ignora o retorno de uma operação pertencente a uma tela encerrada.
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
            if (!modelClass.isAssignableFrom(PasswordLoginViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new PasswordLoginViewModel(repository));
        }
    }
}
