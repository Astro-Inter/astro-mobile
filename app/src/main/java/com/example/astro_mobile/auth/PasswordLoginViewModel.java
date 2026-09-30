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
    private boolean hasPendingResult;
    private AuthFailureKind pendingFailure;

    public PasswordLoginViewModel(FirebaseAuthRepository repository) {
        this.repository = repository;
    }

    public boolean isLoading() {
        return loading;
    }

    public void attach(ResultCallback callback) {
        // Uma tela recriada recebe o resultado que chegou durante a rotação.
        this.callback = callback;
        deliverPendingResult();
    }

    public void detach(ResultCallback callback) {
        if (this.callback == callback) {
            this.callback = null;
        }
    }

    public void signIn(String email, String password) {
        if (loading) {
            return;
        }
        loading = true;
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
        hasPendingResult = true;
        pendingFailure = failure;
        deliverPendingResult();
    }

    private void deliverPendingResult() {
        if (callback == null || !hasPendingResult) {
            return;
        }
        AuthFailureKind failure = pendingFailure;
        hasPendingResult = false;
        pendingFailure = null;
        if (failure == null) {
            callback.onSuccess();
        } else {
            callback.onFailure(failure);
        }
    }

    @Override
    protected void onCleared() {
        ++requestId;
        callback = null;
        hasPendingResult = false;
        pendingFailure = null;
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
