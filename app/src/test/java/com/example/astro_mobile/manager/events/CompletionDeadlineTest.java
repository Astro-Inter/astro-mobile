package com.example.astro_mobile.manager.events;

import org.junit.Test;
import java.time.LocalDateTime;
import static org.junit.Assert.assertEquals;

public class CompletionDeadlineTest {
    @Test public void assignmentIsBlockedOnlyAfterTheExactDeadline() {
        String end = "2026-10-27T18:30:00";
        assertEquals(CompletionDeadline.Result.ALLOWED,
                CompletionDeadline.assignment(end, LocalDateTime.parse(end)));
        assertEquals(CompletionDeadline.Result.EXPIRED,
                CompletionDeadline.assignment(end, LocalDateTime.parse(end).plusNanos(1)));
    }

    @Test public void validationUsesSubmissionTimeRatherThanCurrentTime() {
        assertEquals(CompletionDeadline.Result.ALLOWED,
                CompletionDeadline.validation("2026-10-27T18:30:00", "2026-10-27T18:30:00"));
        assertEquals(CompletionDeadline.Result.EXPIRED,
                CompletionDeadline.validation("2026-10-27T18:30:00", "2026-10-27T18:30:01"));
    }

    @Test public void invalidOrMissingDatesCannotAuthorizeActions() {
        assertEquals(CompletionDeadline.Result.UNAVAILABLE,
                CompletionDeadline.assignment(null, LocalDateTime.now()));
        assertEquals(CompletionDeadline.Result.UNAVAILABLE,
                CompletionDeadline.validation("invalid", "2026-10-27T18:30:00"));
        assertEquals(CompletionDeadline.Result.UNAVAILABLE,
                CompletionDeadline.validation("2026-10-27T18:30:00", null));
    }
}
