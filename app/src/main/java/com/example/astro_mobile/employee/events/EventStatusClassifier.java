package com.example.astro_mobile.employee.events;

import java.time.Instant;
import java.time.Duration;

public final class EventStatusClassifier {
    private static final Duration URGENCY_WINDOW = Duration.ofHours(72);

    public enum Participation { NOT_SUBMITTED, AWAITING_APPROVAL, APPROVED, REJECTED }
    public enum DisplayStatus { CANCELLED, DONE, PENDING, EXPIRED, URGENT, NEW, ANALYSIS, REJECTED }

    private EventStatusClassifier() { }

    public static DisplayStatus classify(boolean cancelled, Participation participation,
                                         boolean viewed, Instant deadline, Instant now) {
        // Cancelamento e situação individual têm prioridade sobre prazo e visualização.
        if (cancelled) return DisplayStatus.CANCELLED;
        if (participation == Participation.APPROVED) return DisplayStatus.DONE;
        if (participation == Participation.AWAITING_APPROVAL) return DisplayStatus.ANALYSIS;
        if (participation == Participation.REJECTED) return DisplayStatus.REJECTED;

        // A hora vem do chamador: a regra não consulta relógio, Android nem API.
        if (!deadline.isAfter(now)) return DisplayStatus.EXPIRED;
        if (!deadline.isAfter(now.plus(URGENCY_WINDOW))) return DisplayStatus.URGENT;
        return viewed ? DisplayStatus.PENDING : DisplayStatus.NEW;
    }
}
