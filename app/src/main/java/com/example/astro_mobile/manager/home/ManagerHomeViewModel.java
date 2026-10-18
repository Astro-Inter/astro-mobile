package com.example.astro_mobile.manager.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.ManagerHomeRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.ManagerHomeData;

public final class ManagerHomeViewModel extends ViewModel {
    public interface ResultCallback {
        void onSuccess(ManagerHomeData home);
        void onFailure(FailureKind failure);
    }

    private final ManagerHomeRepository repository;
    private ManagerHomeData home;
    private boolean loading;
    private int requestId;
    private ResultCallback pendingCallback;

    public ManagerHomeViewModel(ManagerHomeRepository repository) {
        this.repository = repository;
    }

    public ManagerHomeData getHome() { return home; }
    public boolean isLoading() { return loading; }

    public void load(ResultCallback callback) {
        if (loading) return;
        loading = true;
        home = null;
        pendingCallback = callback;
        int id = ++requestId;
        repository.load(new ManagerHomeRepository.ResultCallback() {
            @Override
            public void onSuccess(ManagerHomeData result) {
                ResultCallback current = finishRequest(id);
                if (current == null) return;
                home = result;
                current.onSuccess(result);
            }

            @Override
            public void onFailure(FailureKind failure) {
                ResultCallback current = finishRequest(id);
                if (current != null) current.onFailure(failure);
            }
        });
    }

    private ResultCallback finishRequest(int id) {
        if (id != requestId || !loading) return null;
        loading = false;
        ResultCallback callback = pendingCallback;
        pendingCallback = null;
        return callback;
    }

    public void cancel() {
        ++requestId;
        // Libera o callback da View antes de cancelar token/HTTP e descarta dados da entrada anterior.
        pendingCallback = null;
        loading = false;
        home = null;
        repository.cancel();
    }

    @Override
    protected void onCleared() { cancel(); }

    public static final class Factory implements ViewModelProvider.Factory {
        private final ManagerHomeRepository repository;

        public Factory(ManagerHomeRepository repository) { this.repository = repository; }

        public static Factory createDefault() {
            return new Factory(ManagerHomeRepository.createDefault());
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(ManagerHomeViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new ManagerHomeViewModel(repository));
        }
    }
}
