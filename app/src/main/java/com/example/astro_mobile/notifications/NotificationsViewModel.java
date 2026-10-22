package com.example.astro_mobile.notifications;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.NotificationsRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.NotificationData;
import com.example.astro_mobile.data.api.dto.NotificationDetailsData;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class NotificationsViewModel extends ViewModel {
    public interface ResultCallback {
        void onSuccess(List<NotificationData> notifications);
        void onFailure(FailureKind failure);
    }

    private final NotificationsRepository repository;
    private List<NotificationData> notifications = Collections.emptyList();
    private String searchQuery = "";
    private boolean loading;
    private int requestId;
    private ResultCallback pendingCallback;

    public NotificationsViewModel(NotificationsRepository repository) {
        this.repository = repository;
    }

    public List<NotificationData> getNotifications() { return notifications; }
    public boolean isLoading() { return loading; }
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String query) { searchQuery = query; }

    public void loadDetail(String id, NotificationsRepository.OperationCallback<NotificationDetailsData> callback) {
        repository.loadDetail(id, callback);
    }

    public void markViewed(String id, NotificationsRepository.OperationCallback<Void> callback) {
        repository.markViewed(id, new NotificationsRepository.OperationCallback<Void>() {
            @Override public void onSuccess(Void result) {
                // Atualiza a indicação local apenas depois da confirmação do servidor.
                for (NotificationData notification : notifications) {
                    if (notification.getId().equals(id)) notification.markViewed();
                }
                callback.onSuccess(null);
            }
            @Override public void onFailure(FailureKind failure) { callback.onFailure(failure); }
        });
    }

    public void cancelDetail() { repository.cancelDetail(); }

    public List<NotificationData> visibleNotifications() {
        // Mantém a lista recebida intacta e aplica a pesquisa no estado da tela.
        List<NotificationData> visible = new ArrayList<>();
        String query = normalize(searchQuery.trim());
        for (NotificationData notification : notifications) {
            if (normalize(notification.getTitle()).contains(query)) visible.add(notification);
        }
        return visible;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public void load(ResultCallback callback) {
        if (loading) return;
        loading = true;
        notifications = Collections.emptyList();
        pendingCallback = callback;
        int id = ++requestId;
        repository.load(new NotificationsRepository.ResultCallback() {
            @Override
            public void onSuccess(List<NotificationData> result) {
                ResultCallback current = finishRequest(id);
                if (current == null) return;
                notifications = result;
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
        notifications = Collections.emptyList();
        repository.cancel();
    }

    @Override
    protected void onCleared() { cancel(); }

    public static final class Factory implements ViewModelProvider.Factory {
        private final NotificationsRepository repository;

        public Factory(NotificationsRepository repository) { this.repository = repository; }

        public static Factory createDefault() {
            return new Factory(NotificationsRepository.createDefault());
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(NotificationsViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new NotificationsViewModel(repository));
        }
    }
}
