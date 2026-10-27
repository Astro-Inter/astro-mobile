package com.example.astro_mobile.data.api.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

public final class CollaboratorEventDetailsData {
    public enum CompletionMode { COLABORADOR, GESTOR }
    private Long eventId;
    private String eventTitle;
    private String classLimitDate;
    private CollaboratorEventData.ParticipationStatus participationStatus;
    private CollaboratorEventData.EventStatus eventStatus;
    private String eventDescription;
    private String complianceValidity;
    private String externalLink;
    private String managerName;
    private boolean evidenceMandatory;
    private CompletionMode completionMode;
    private List<Evidence> evidences;

    public Long getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public LocalDateTime getDeadline() { return LocalDateTime.parse(classLimitDate); }
    public CollaboratorEventData.ParticipationStatus getParticipationStatus() { return participationStatus; }
    public CollaboratorEventData.EventStatus getEventStatus() { return eventStatus; }
    public String getEventDescription() { return eventDescription; }
    public String getComplianceValidity() { return complianceValidity; }
    public String getExternalLink() { return externalLink; }
    public String getManagerName() { return managerName; }
    public boolean isEvidenceMandatory() { return evidenceMandatory; }
    public CompletionMode getCompletionMode() { return completionMode; }
    public List<Evidence> getEvidences() { return evidences == null ? Collections.emptyList() : evidences; }

    public boolean isValid() {
        if (eventId == null || eventId <= 0 || eventTitle == null || eventTitle.trim().isEmpty()
                || classLimitDate == null || participationStatus == null || eventStatus == null
                || completionMode == null) return false;
        try {
            getDeadline();
            return true;
        } catch (DateTimeParseException error) { return false; }
    }

    public static final class Evidence {
        private String originalName;
        private String url;
        public String getOriginalName() { return originalName; }
        public String getUrl() { return url; }
    }
}
