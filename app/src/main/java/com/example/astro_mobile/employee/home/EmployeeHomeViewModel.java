package com.example.astro_mobile.employee.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.CollaboratorHomeRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.CollaboratorHomeData;

public final class EmployeeHomeViewModel extends ViewModel {
    public interface ResultCallback {
        void onSuccess(CollaboratorHomeData home);
        void onFailure(FailureKind failure);
    }

    private final CollaboratorHomeRepository repository;
    private CollaboratorHomeData home;
    private boolean loading;
    private int requestId;
    private ResultCallback pendingCallback;

    public EmployeeHomeViewModel(CollaboratorHomeRepository repository) {
        this.repository = repository;
    }

    public CollaboratorHomeData getHome() { return home; }
    public boolean isLoading() { return loading; }

    public void load(ResultCallback callback) {
        if (loading) return;
        loading = true;
        home = null;
        pendingCallback = callback;
        int id = ++requestId;
        repository.load(new CollaboratorHomeRepository.ResultCallback() {
            @Override
            public void onSuccess(CollaboratorHomeData result) {
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
        private final CollaboratorHomeRepository repository;

        public Factory(CollaboratorHomeRepository repository) { this.repository = repository; }

        public static Factory createDefault() {
            return new Factory(CollaboratorHomeRepository.createDefault());
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(EmployeeHomeViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new EmployeeHomeViewModel(repository));
        }
    }
}
