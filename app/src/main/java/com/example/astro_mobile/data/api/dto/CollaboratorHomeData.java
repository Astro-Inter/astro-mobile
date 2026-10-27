package com.example.astro_mobile.data.api.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CollaboratorHomeData {
    private String username;
    @SerializedName("workspace_name") private String workspaceName;
    @SerializedName("events_metrics") private EventsMetrics eventsMetrics;
    @SerializedName("status_message") private String statusMessage;
    @SerializedName("pending_events") private List<PendingEvent> pendingEvents;
    @SerializedName("last_notifications") private List<HomeNotification> lastNotifications;

    public String getUsername() { return username; }
    public String getWorkspaceName() { return workspaceName; }
    public EventsMetrics getEventsMetrics() { return eventsMetrics; }
    public String getStatusMessage() { return statusMessage; }
    public List<PendingEvent> getPendingEvents() {
        return pendingEvents == null ? Collections.emptyList() : pendingEvents;
    }
    public List<HomeNotification> getLastNotifications() {
        return lastNotifications == null ? Collections.emptyList() : lastNotifications;
    }

    public void setLastNotificationsFrom(List<NotificationData> notifications) {
        if (notifications == null) return;
        lastNotifications = new ArrayList<>();
        for (NotificationData notification : notifications) {
            if (notification == null) continue;
            lastNotifications.add(new HomeNotification(notification.getId(), notification.getTitle(),
                    notification.getHours(), notification.isViewed()));
            if (lastNotifications.size() == 3) break;
        }
    }

    public boolean isValid() {
        return username != null && !username.trim().isEmpty()
                && workspaceName != null && !workspaceName.trim().isEmpty()
                && eventsMetrics != null && eventsMetrics.pending != null
                && eventsMetrics.underAnalysis != null && eventsMetrics.done != null
                && eventsMetrics.pending >= 0 && eventsMetrics.underAnalysis >= 0 && eventsMetrics.done >= 0
                && ("URGENT".equals(statusMessage) || "PENDENT".equals(statusMessage)
                || "ANALYSIS".equals(statusMessage) || "OK".equals(statusMessage));
    }

    public static final class EventsMetrics {
        private Integer pending;
        @SerializedName("under_analysis") private Integer underAnalysis;
        private Integer done;

        public int getPending() { return pending; }
        public int getUnderAnalysis() { return underAnalysis; }
        public int getDone() { return done; }
    }

    public static final class PendingEvent {
        private Long id;
        private String title;
        @SerializedName("limit_date") private String limitDate;
        private boolean viewed;
        private transient CollaboratorEventData eventData;

        public CollaboratorEventData getEventData() { return eventData; }
        public void setEventData(CollaboratorEventData eventData) { this.eventData = eventData; }
        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getLimitDate() { return limitDate; }
        public boolean isViewed() { return viewed; }
    }

    public static final class HomeNotification {
        private String id;
        private String title;
        @SerializedName("sent_at_in_hours") private long sentAtInHours;
        private boolean viewed;

        public HomeNotification() { }

        public HomeNotification(String id, String title, long sentAtInHours, boolean viewed) {
            this.id = id;
            this.title = title;
            this.sentAtInHours = sentAtInHours;
            this.viewed = viewed;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public long getSentAtInHours() { return sentAtInHours; }
        public boolean isViewed() { return viewed; }
    }
}
