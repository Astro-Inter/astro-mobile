package com.example.astro_mobile.data.api.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public final class ManagerHomeData {
    private String username;
    @SerializedName("workspace_name") private String workspaceName;
    @SerializedName("pending_validations") private List<PendingValidation> pendingValidations;
    @SerializedName("pending_forms") private List<PendingForm> pendingForms;
    private Indicators indicators;

    public String getUsername() { return username; }
    public String getWorkspaceName() { return workspaceName; }
    public Indicators getIndicators() { return indicators; }
    public List<PendingValidation> getPendingValidations() {
        return pendingValidations == null ? Collections.emptyList() : pendingValidations;
    }
    public List<PendingForm> getPendingForms() {
        return pendingForms == null ? Collections.emptyList() : pendingForms;
    }

    public boolean isValid() {
        return username != null && !username.trim().isEmpty()
                && workspaceName != null && !workspaceName.trim().isEmpty()
                && indicators != null && indicators.currentEvents != null
                && indicators.pendingValidations != null && indicators.collaboratorsWithPendingEvents != null
                && indicators.currentEvents >= 0 && indicators.pendingValidations >= 0
                && indicators.collaboratorsWithPendingEvents >= 0;
    }

    public static final class Indicators {
        @SerializedName("current_events") private Long currentEvents;
        @SerializedName("pending_validations") private Long pendingValidations;
        @SerializedName("collaborators_with_pending_events") private Long collaboratorsWithPendingEvents;

        public long getCurrentEvents() { return currentEvents; }
        public long getPendingValidations() { return pendingValidations; }
        public long getCollaboratorsWithPendingEvents() { return collaboratorsWithPendingEvents; }
    }

    public static final class PendingValidation {
        private Long id;
        @SerializedName("collaborator_name") private String collaboratorName;
        @SerializedName("event_title") private String eventTitle;
        private boolean viewed;

        public Long getId() { return id; }
        public String getCollaboratorName() { return collaboratorName; }
        public String getEventTitle() { return eventTitle; }
        public boolean isViewed() { return viewed; }
    }

    public static final class PendingForm {
        private String id;
        private String title;
        @SerializedName("days_left") private Long daysLeft;
        private boolean viewed;

        public String getId() { return id; }
        public String getTitle() { return title; }
        public Long getDaysLeft() { return daysLeft; }
        public boolean isViewed() { return viewed; }
    }
}
