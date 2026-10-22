package com.example.astro_mobile.data.api.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public final class CollaboratorEventData {
    public enum ParticipationStatus { PENDENTE, EM_ANALISE, CONCLUIDO, REJEITADO }
    public enum EventStatus { ATIVO, CONCLUIDO, CANCELADO }

    private Long id;
    private String eventTitle;
    private String classLimitDate;
    private Boolean viewed;
    private ParticipationStatus participationStatus;
    private String className;
    private EventStatus eventStatus;

    public Long getId() { return id; }
    public String getEventTitle() { return eventTitle; }
    public LocalDateTime getDeadline() { return LocalDateTime.parse(classLimitDate); }
    public boolean isViewed() { return Boolean.TRUE.equals(viewed); }
    public ParticipationStatus getParticipationStatus() { return participationStatus; }
    public String getClassName() { return className; }
    public EventStatus getEventStatus() { return eventStatus; }

    public boolean isValid() {
        if (id == null || id <= 0 || eventTitle == null || eventTitle.trim().isEmpty()
                || classLimitDate == null || viewed == null || participationStatus == null
                || eventStatus == null) return false;
        try {
            getDeadline();
            return true;
        } catch (DateTimeParseException error) {
            return false;
        }
    }
}
