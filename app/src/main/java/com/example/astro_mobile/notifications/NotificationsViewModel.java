package com.example.astro_mobile.notifications;

import androidx.lifecycle.ViewModel;

import com.example.astro_mobile.notifications.mock.NotificationSamples;

import java.util.Collections;
import java.util.List;

public final class NotificationsViewModel extends ViewModel {
    private List<NotificationItem> notifications = Collections.emptyList();
    private String searchQuery = "";
    private boolean managerFlow;
    private boolean configured;
    private boolean loaded;

    // Mantém a busca e as amostras enquanto o Fragment recria sua interface.
    public void configure(boolean isManagerFlow) {
        if (configured && managerFlow == isManagerFlow) return;
        loaded = false;
        managerFlow = isManagerFlow;
        notifications = managerFlow
                ? NotificationSamples.forManager() : NotificationSamples.forEmployee();
        searchQuery = "";
        configured = true;
    }

    public List<NotificationItem> getNotifications() {
        return notifications;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String query) {
        searchQuery = query;
    }

    public boolean isManagerFlow() {
        return managerFlow;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void markLoaded() {
        loaded = true;
    }
}
