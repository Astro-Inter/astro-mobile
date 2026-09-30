package com.example.astro_mobile.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.example.astro_mobile.data.firebase.FirebaseAuthRepository;

public class SessionViewModel extends ViewModel {
    public interface SessionCallback {
        void onUser(@Nullable String email);

        void onFailure(AuthFailureKind kind);
    }

    private final FirebaseAuthRepository repository;
    private int requestId;

    public SessionViewModel(FirebaseAuthRepository repository) {
        this.repository = repository;
    }

    public void checkCurrentUser(SessionCallback callback) {
        // Só a resposta da consulta mais recente pode direcionar a splash.
        int id = ++requestId;
        repository.checkCurrentUser(new FirebaseAuthRepository.SessionCallback() {
            @Override
            public void onUser(@Nullable String email) {
                if (id == requestId) {
                    callback.onUser(email);
                }
            }

            @Override
            public void onFailure(AuthFailureKind kind) {
                if (id == requestId) {
                    callback.onFailure(kind);
                }
            }
        });
    }

    public void signOut() {
        // Invalida verificações pendentes antes de encerrar a sessão Firebase.
        ++requestId;
        repository.signOut();
    }

    public boolean hasCurrentUser() {
        return repository.hasCurrentUser();
    }

    @Override
    protected void onCleared() {
        ++requestId;
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
            if (!modelClass.isAssignableFrom(SessionViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new SessionViewModel(repository));
        }
    }
}
