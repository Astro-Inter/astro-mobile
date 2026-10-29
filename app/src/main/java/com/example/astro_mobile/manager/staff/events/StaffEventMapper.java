package com.example.astro_mobile.manager.staff.events;

import com.example.astro_mobile.data.api.dto.CollaboratorStaffEventData;
import com.example.astro_mobile.employee.events.EventStatusClassifier;
import com.example.astro_mobile.manager.events.ManagerEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Converte os estados do endpoint para os rótulos usados na lista do Gestor. */
final class StaffEventMapper {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private StaffEventMapper() { }

    static StaffEvent toStaffEvent(CollaboratorStaffEventData data, ZoneId zone, Instant now) {
        // Valida uma vez os campos necessários para exibir o item e abrir seu detalhe.
        if (data == null || data.eventId == null || data.eventId <= 0
                || data.eventTitle == null || data.eventTitle.trim().isEmpty()
                || data.classLimitDate == null) return null;
        ManagerEvent.CompletionMode mode;
        if ("COLABORADOR".equals(data.completionMode)) {
            mode = ManagerEvent.CompletionMode.SELF_DECLARATION;
        } else if ("GESTOR".equals(data.completionMode)) {
            mode = ManagerEvent.CompletionMode.MANAGER_ASSIGNMENT;
        } else {
            return null;
        }
        EventStatusClassifier.Participation participation;
        if ("PENDENTE".equals(data.completionStatus)) {
            participation = EventStatusClassifier.Participation.NOT_SUBMITTED;
        } else if ("EM_ANALISE".equals(data.completionStatus)) {
            participation = EventStatusClassifier.Participation.AWAITING_APPROVAL;
        } else if ("CONCLUIDO".equals(data.completionStatus)) {
            participation = EventStatusClassifier.Participation.APPROVED;
        } else if ("REJEITADO".equals(data.completionStatus)) {
            participation = EventStatusClassifier.Participation.REJECTED;
        } else {
            return null;
        }

        if (!"ATIVO".equals(data.eventStatus) && !"CONCLUIDO".equals(data.eventStatus)
                && !"CANCELADO".equals(data.eventStatus)) return null;
        try {
            LocalDateTime deadline = LocalDateTime.parse(data.classLimitDate);
            // Reutiliza a regra testada; nesta visão, Novo/Urgente permanecem Pendente.
            EventStatusClassifier.DisplayStatus displayStatus = EventStatusClassifier.classify(
                    "CANCELADO".equals(data.eventStatus), participation, true,
                    deadline.atZone(zone).toInstant(), now);
            StaffEvent.Status status = toStatus(displayStatus);
            return status == null ? null : new StaffEvent(data.eventId, data.eventTitle,
                    DATE_FORMAT.format(deadline), status, mode);
        } catch (DateTimeParseException error) {
            return null;
        }
    }

    private static StaffEvent.Status toStatus(EventStatusClassifier.DisplayStatus status) {
        switch (status) {
            case DONE: return StaffEvent.Status.DONE;
            case EXPIRED: return StaffEvent.Status.EXPIRED;
            case ANALYSIS: return StaffEvent.Status.ANALYSIS;
            case REJECTED: return StaffEvent.Status.REJECTED;
            case CANCELLED: return StaffEvent.Status.CANCELLED;
            case PENDING:
            case NEW:
            case URGENT: return StaffEvent.Status.PENDING;
            default: return null;
        }
    }
}
