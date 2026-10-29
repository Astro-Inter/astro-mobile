package com.example.astro_mobile.data.api.dto;

/** Detalhe do evento de autodeclaração na visão do gestor. */
public final class ManagerEventDetailsData {
    public Long eventId, participantCount, pendingValidationCount, validatedCount, notSubmittedCount;
    public String eventTitle, eventDescription, externalLink, lastClassLimitDate, completionMode, managerName;
}
