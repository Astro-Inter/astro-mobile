package com.example.astro_mobile.shared.profile;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.UserProfileRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.ProfilePhotoFile;
import com.example.astro_mobile.data.api.dto.UserProfileData;
import com.example.astro_mobile.data.firebase.FirebaseAuthRepository;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ProfileViewModel extends ViewModel {
    private final UserProfileRepository repository;
    private final FirebaseAuthRepository auth;
    private UserProfileData profile;
    private boolean loading;
    private final ExecutorService photoReader = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Future<?> preparation;
    private int photoRequest;

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
        ++photoRequest;
        if (preparation != null) { preparation.cancel(true); preparation = null; }
        repository.cancel();
        loading = false;
    }

    public void uploadPhoto(ContentResolver resolver, Uri uri, UserProfileRepository.PhotoCallback callback) {
        cancel();
        loading = true;
        int id = photoRequest;
        preparation = photoReader.submit(() -> {
            try (InputStream input = resolver.openInputStream(uri)) {
                ProfilePhotoFile file = ProfilePhotoFile.read(input);
                mainHandler.post(() -> {
                    if (id != photoRequest) return;
                    repository.uploadPhoto(file, new UserProfileRepository.PhotoCallback() {
                        @Override
                        public void onSuccess() {
                            profile = null;
                            loading = false;
                            callback.onSuccess();
                        }

                        @Override
                        public void onFailure(FailureKind failure) {
                            loading = false;
                            callback.onFailure(failure);
                        }
                    });
                });
            } catch (IOException | SecurityException error) {
                mainHandler.post(() -> {
                    if (id != photoRequest) return;
                    loading = false;
                    callback.onFailure(error instanceof ProfilePhotoFile.InvalidPhotoException
                            ? FailureKind.BUSINESS : FailureKind.INTERNAL);
                });
            }
        });
    }

    @Override
    protected void onCleared() { cancel(); photoReader.shutdownNow(); }

    public static final class Factory implements ViewModelProvider.Factory {
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return modelClass.cast(new ProfileViewModel(UserProfileRepository.createDefault(),
                    FirebaseAuthRepository.createDefault()));
        }
    }
}
