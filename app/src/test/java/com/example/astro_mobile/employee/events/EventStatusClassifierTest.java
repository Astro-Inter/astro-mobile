package com.example.astro_mobile.employee.events;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Instant;
import java.time.Duration;

public final class EventStatusClassifierTest {
    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

    @Test
    public void cancellationTakesPriorityOverApprovalAndDeadline() {
        assertEquals(EventStatusClassifier.DisplayStatus.CANCELLED,
                EventStatusClassifier.classify(true,
                        EventStatusClassifier.Participation.APPROVED, true,
                        NOW.minusSeconds(1), NOW));
    }

    @Test
    public void approvedEventRemainsDoneAfterDeadlineEvenWhenUnread() {
        assertEquals(EventStatusClassifier.DisplayStatus.DONE,
                EventStatusClassifier.classify(false,
                        EventStatusClassifier.Participation.APPROVED, false,
                        NOW.minusSeconds(1), NOW));
    }

    @Test
    public void awaitingApprovalRemainsPendingAfterDeadline() {
        assertEquals(EventStatusClassifier.DisplayStatus.PENDING,
                EventStatusClassifier.classify(false,
                        EventStatusClassifier.Participation.AWAITING_APPROVAL, true,
                        NOW.minusSeconds(1), NOW));
    }

    @Test
    public void awaitingApprovalIsNotUrgentOrNew() {
        assertEquals(EventStatusClassifier.DisplayStatus.PENDING,
                EventStatusClassifier.classify(false,
                        EventStatusClassifier.Participation.AWAITING_APPROVAL, false,
                        NOW.plusSeconds(3600), NOW));
    }

    @Test
    public void expiredNotSubmittedEventIsNeitherNewNorUrgent() {
        for (boolean viewed : new boolean[] {false, true}) {
            assertEquals(EventStatusClassifier.DisplayStatus.EXPIRED,
                    EventStatusClassifier.classify(false,
                            EventStatusClassifier.Participation.NOT_SUBMITTED, viewed,
                            NOW.minusNanos(1), NOW));
        }
    }

    @Test
    public void deadlineReachedIsExpiredForNotSubmittedEvent() {
        assertEquals(EventStatusClassifier.DisplayStatus.EXPIRED,
                EventStatusClassifier.classify(false,
                        EventStatusClassifier.Participation.NOT_SUBMITTED, true, NOW, NOW));
    }

    @Test
    public void exactly72HoursIsUrgentEvenWhenUnread() {
        assertEquals(EventStatusClassifier.DisplayStatus.URGENT,
                EventStatusClassifier.classify(false,
                        EventStatusClassifier.Participation.NOT_SUBMITTED, false,
                        NOW.plus(Duration.ofHours(72)), NOW));
    }

    @Test
    public void lessThan72HoursIsUrgentEvenWhenViewed() {
        assertEquals(EventStatusClassifier.DisplayStatus.URGENT,
                EventStatusClassifier.classify(false,
                        EventStatusClassifier.Participation.NOT_SUBMITTED, true,
                        NOW.plus(Duration.ofHours(72)).minusNanos(1), NOW));
    }

    @Test
    public void moreThan72HoursIsNewWhenUnread() {
        assertEquals(EventStatusClassifier.DisplayStatus.NEW,
                EventStatusClassifier.classify(false,
                        EventStatusClassifier.Participation.NOT_SUBMITTED, false,
                        NOW.plus(Duration.ofHours(72)).plusNanos(1), NOW));
    }

    @Test
    public void moreThan72HoursIsPendingWhenViewed() {
        assertEquals(EventStatusClassifier.DisplayStatus.PENDING,
                EventStatusClassifier.classify(false,
                        EventStatusClassifier.Participation.NOT_SUBMITTED, true,
                        NOW.plus(Duration.ofHours(72)).plusNanos(1), NOW));
    }
}
