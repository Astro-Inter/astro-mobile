package com.example.astro_mobile.employee.events;

import com.example.astro_mobile.data.api.dto.CollaboratorEventData;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;

public final class EmployeeEvent {
    public enum Status { PENDING, REVIEW, DONE, REJECTED, CANCELLED, EXPIRED }

    public final String title;
    public final LocalDate date;
    public final Status status;
    public final boolean urgent;
    public final Long id;
    public final EventStatusClassifier.DisplayStatus displayStatus;

    public static EmployeeEvent fromApi(CollaboratorEventData data, ZoneId zone, Instant now) {
        EventStatusClassifier.Participation participation;
        switch (data.getParticipationStatus()) {
            case EM_ANALISE: participation = EventStatusClassifier.Participation.AWAITING_APPROVAL; break;
            case CONCLUIDO: participation = EventStatusClassifier.Participation.APPROVED; break;
            case REJEITADO: participation = EventStatusClassifier.Participation.REJECTED; break;
            default: participation = EventStatusClassifier.Participation.NOT_SUBMITTED;
        }
        // Home e calendário usam a mesma precedência de situação, prazo e visualização.
        EventStatusClassifier.DisplayStatus display = EventStatusClassifier.classify(
                data.getEventStatus() == CollaboratorEventData.EventStatus.CANCELADO,
                participation, data.isViewed(), data.getDeadline().atZone(zone).toInstant(), now);
        return new EmployeeEvent(data.getId(), data.getEventTitle(), data.getDeadline().toLocalDate(), display);
    }

    public EmployeeEvent(String title, LocalDate date, Status status, boolean urgent) {
        this.title = title;
        this.date = date;
        this.status = status;
        this.urgent = urgent;
        this.id = null;
        this.displayStatus = urgent ? EventStatusClassifier.DisplayStatus.URGENT
                : status == Status.DONE ? EventStatusClassifier.DisplayStatus.DONE
                : status == Status.REVIEW ? EventStatusClassifier.DisplayStatus.ANALYSIS
                : EventStatusClassifier.DisplayStatus.PENDING;
    }

    public EmployeeEvent(Long id, String title, LocalDate date,
                         EventStatusClassifier.DisplayStatus displayStatus) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.displayStatus = displayStatus;
        this.urgent = displayStatus == EventStatusClassifier.DisplayStatus.URGENT;
        switch (displayStatus) {
            case DONE: status = Status.DONE; break;
            case ANALYSIS: status = Status.REVIEW; break;
            case REJECTED: status = Status.REJECTED; break;
            case CANCELLED: status = Status.CANCELLED; break;
            case EXPIRED: status = Status.EXPIRED; break;
            default: status = Status.PENDING;
        }
    }
}
