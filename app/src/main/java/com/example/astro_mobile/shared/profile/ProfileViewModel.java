package com.example.astro_mobile.shared.profile;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.UserProfileRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.UserProfileData;
import com.example.astro_mobile.data.firebase.FirebaseAuthRepository;

public final class ProfileViewModel extends ViewModel {
    private final UserProfileRepository repository;
    private final FirebaseAuthRepository auth;
    private UserProfileData profile;
    private boolean loading;

    public ProfileViewModel(UserProfileRepository repository, FirebaseAuthRepository auth) {
        this.repository = repository;
        this.auth = auth;
    }

    public UserProfileData getProfile() { return profile; }
    public String getUserId() { return auth.getCurrentUserId(); }
    public boolean isLoading() { return loading; }

    public void load(UserProfileRepository.ResultCallback callback) {
        if (loading) return;
        loading = true;
        // Conserva os dados ao visitar a alteração de senha e voltar ao perfil.
        repository.load(new UserProfileRepository.ResultCallback() {
            @Override
            public void onSuccess(UserProfileData result) {
                profile = result;
                loading = false;
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(FailureKind failure) {
                loading = false;
                callback.onFailure(failure);
            }
        });
    }

    public void cancel() {
        repository.cancel();
        loading = false;
    }

    @Override
    protected void onCleared() { cancel(); }

    public static final class Factory implements ViewModelProvider.Factory {
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return modelClass.cast(new ProfileViewModel(UserProfileRepository.createDefault(),
                    FirebaseAuthRepository.createDefault()));
        }
    }
}
