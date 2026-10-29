package com.example.astro_mobile.manager.staff.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.example.astro_mobile.data.api.dto.CollaboratorStaffEventData;
import com.example.astro_mobile.manager.events.ManagerEvent;

import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;

public final class StaffEventMapperTest {
    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("UTC");

    @Test
    public void deadlinePassedWithoutSubmissionIsExpired() {
        assertEquals(StaffEvent.Status.EXPIRED, map("PENDENTE", "ATIVO", "2026-10-28T10:00:00").status);
    }

    @Test
    public void analysisRemainsAnalysisAfterDeadline() {
        assertEquals(StaffEvent.Status.ANALYSIS, map("EM_ANALISE", "ATIVO", "2026-10-28T10:00:00").status);
    }

    @Test
    public void approvedCompletionIsDone() {
        assertEquals(StaffEvent.Status.DONE, map("CONCLUIDO", "ATIVO", "2026-10-28T10:00:00").status);
    }

    @Test
    public void rejectedCompletionRemainsRejected() {
        assertEquals(StaffEvent.Status.REJECTED, map("REJEITADO", "ATIVO", "2026-10-28T10:00:00").status);
    }

    @Test
    public void cancellationTakesPriorityOverCompletion() {
        assertEquals(StaffEvent.Status.CANCELLED, map("CONCLUIDO", "CANCELADO", "2026-10-28T10:00:00").status);
    }

    @Test
    public void activeEventWithinDeadlineIsPending() {
        assertEquals(StaffEvent.Status.PENDING, map("PENDENTE", "ATIVO", "2026-11-10T10:00:00").status);
    }

    @Test
    public void missingCompletionModeCannotOpenAnArbitraryDetail() {
        CollaboratorStaffEventData data = contractEvent("");
        assertNull(StaffEventMapper.toStaffEvent(data, ZONE, NOW));
    }

    @Test
    public void unknownCompletionModeCannotOpenAnArbitraryDetail() {
        CollaboratorStaffEventData data = contractEvent(",\"completionMode\":\"OUTRO\"");
        assertNull(StaffEventMapper.toStaffEvent(data, ZONE, NOW));
    }

    @Test
    public void collaboratorModeOpensSelfDeclarationDetail() {
        StaffEvent event = StaffEventMapper.toStaffEvent(
                contractEvent(",\"completionMode\":\"COLABORADOR\""), ZONE, NOW);
        assertEquals(42L, event.id);
        assertEquals(ManagerEvent.CompletionMode.SELF_DECLARATION, event.completionMode);
    }

    @Test
    public void managerModeOpensAssignmentDetail() {
        StaffEvent event = StaffEventMapper.toStaffEvent(
                contractEvent(",\"completionMode\":\"GESTOR\""), ZONE, NOW);
        assertEquals(ManagerEvent.CompletionMode.MANAGER_ASSIGNMENT, event.completionMode);
    }

    @Test
    public void localDeadlineUsesTheDeviceTimeZone() {
        CollaboratorStaffEventData data = contractEvent(",\"completionMode\":\"GESTOR\"");
        data.classLimitDate = "2026-10-29T10:00:00";
        assertEquals(StaffEvent.Status.EXPIRED, StaffEventMapper.toStaffEvent(data, ZONE, NOW).status);
        assertEquals(StaffEvent.Status.PENDING,
                StaffEventMapper.toStaffEvent(data, ZoneId.of("America/Sao_Paulo"), NOW).status);
    }

    private CollaboratorStaffEventData contractEvent(String extra) {
        return new Gson().fromJson("{\"eventId\":42,\"eventTitle\":\"Treinamento\","
                + "\"classLimitDate\":\"2026-11-10T10:00:00\",\"completionStatus\":\"PENDENTE\","
                + "\"eventStatus\":\"ATIVO\"" + extra + "}", CollaboratorStaffEventData.class);
    }

    private StaffEvent map(String completionStatus, String eventStatus, String deadline) {
        CollaboratorStaffEventData data = new CollaboratorStaffEventData();
        data.eventId = 42L;
        data.eventTitle = "Treinamento de EPI";
        data.classLimitDate = deadline;
        data.completionStatus = completionStatus;
        data.eventStatus = eventStatus;
        data.completionMode = "COLABORADOR";
        return StaffEventMapper.toStaffEvent(data, ZONE, NOW);
    }
}
