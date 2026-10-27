package com.example.astro_mobile.employee.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.CollaboratorHomeRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.NotificationsRepository;
import com.example.astro_mobile.data.api.dto.CollaboratorHomeData;
import com.example.astro_mobile.data.api.dto.NotificationData;

import java.util.List;

public final class EmployeeHomeViewModel extends ViewModel {
    public interface ResultCallback {
        void onSuccess(CollaboratorHomeData home);
        void onFailure(FailureKind failure);
    }

    private final CollaboratorHomeRepository repository;
    private final NotificationsRepository notificationsRepository;
    private CollaboratorHomeData home;
    private boolean loading;
    private int requestId;
    private ResultCallback pendingCallback;

    public EmployeeHomeViewModel(CollaboratorHomeRepository repository) {
        this(repository, null);
    }

    public EmployeeHomeViewModel(CollaboratorHomeRepository repository,
                                 NotificationsRepository notificationsRepository) {
        this.repository = repository;
        this.notificationsRepository = notificationsRepository;
    }

    public CollaboratorHomeData getHome() { return home; }
    public boolean isLoading() { return loading; }

    public void load(ResultCallback callback) {
        if (loading) return;
        loading = true;
        pendingCallback = callback;
        int id = ++requestId;
        repository.load(new CollaboratorHomeRepository.ResultCallback() {
            @Override
            public void onSuccess(CollaboratorHomeData result) {
                if (notificationsRepository == null) {
                    finishHome(id, result);
                    return;
                }
                notificationsRepository.load(new NotificationsRepository.ResultCallback() {
                    @Override public void onSuccess(List<NotificationData> notifications) {
                        if (id != requestId) return;
                        result.setLastNotificationsFrom(notifications);
                        finishHome(id, result);
                    }

                    @Override public void onFailure(FailureKind failure) {
                        finishFailure(id, failure);
                    }
                });
            }

            @Override
            public void onFailure(FailureKind failure) {
                finishFailure(id, failure);
            }
        });
    }

    private void finishHome(int id, CollaboratorHomeData result) {
        ResultCallback current = finishRequest(id);
        if (current == null) return;
        home = result;
        current.onSuccess(result);
    }

    private void finishFailure(int id, FailureKind failure) {
        ResultCallback current = finishRequest(id);
        if (current == null) return;
        if (failure == FailureKind.SESSION) home = null;
        current.onFailure(failure);
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
        // Libera a View e cancela a consulta, preservando a última Home carregada nesta sessão.
        pendingCallback = null;
        loading = false;
        repository.cancel();
        if (notificationsRepository != null) notificationsRepository.cancel();
    }

    @Override
    protected void onCleared() { cancel(); home = null; }

    public static final class Factory implements ViewModelProvider.Factory {
        private final CollaboratorHomeRepository repository;
        private final NotificationsRepository notificationsRepository;

        public Factory(CollaboratorHomeRepository repository, NotificationsRepository notificationsRepository) {
            this.repository = repository;
            this.notificationsRepository = notificationsRepository;
        }

        public static Factory createDefault() {
            return new Factory(CollaboratorHomeRepository.createDefault(), NotificationsRepository.createDefault());
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(EmployeeHomeViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new EmployeeHomeViewModel(repository, notificationsRepository));
        }
    }
}
