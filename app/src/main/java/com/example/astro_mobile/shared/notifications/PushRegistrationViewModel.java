package com.example.astro_mobile.shared.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.astro_mobile.data.firebase.PushTokenRepository;

public final class PushRegistrationViewModel extends ViewModel {
    private final PushTokenRepository repository;
    public PushRegistrationViewModel(PushTokenRepository repository) { this.repository = repository; }
    public void synchronize() { repository.synchronize(); }
    public boolean mayAskPermission() { return !repository.permissionAsked(); }
    public void permissionRequested() { repository.markPermissionAsked(); }
    public boolean isCurrentUser(String uid) { return repository.isCurrentUser(uid); }
    public static final class Factory implements ViewModelProvider.Factory {
        public static Factory createDefault(Context context) { PushTokenRepository.initialize(context); return new Factory(); }
        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type) {
            if (!type.isAssignableFrom(PushRegistrationViewModel.class)) throw new IllegalArgumentException("Unknown ViewModel");
            return type.cast(new PushRegistrationViewModel(PushTokenRepository.getInstance()));
        }
    }
}
