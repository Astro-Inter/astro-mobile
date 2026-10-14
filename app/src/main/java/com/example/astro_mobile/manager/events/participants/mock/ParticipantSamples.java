package com.example.astro_mobile.manager.events.participants.mock;

import com.example.astro_mobile.manager.events.ManagerEvent;
import com.example.astro_mobile.manager.events.participants.EventParticipant;

import java.util.Arrays;
import java.util.List;

public final class ParticipantSamples {
    private ParticipantSamples() { }

    public static List<EventParticipant> forMode(ManagerEvent.CompletionMode mode) {
        // Reproduz os três exemplos de cada frame; não são usuários reais da API.
        boolean selfDeclaration = mode == ManagerEvent.CompletionMode.SELF_DECLARATION;
        return Arrays.asList(
                new EventParticipant("Kevin Parker", selfDeclaration ? EventParticipant.Status.VALIDATION_PENDING
                        : EventParticipant.Status.AWAITING_COMPLETION, 0),
                new EventParticipant("Kevin Parker", EventParticipant.Status.AWAITING_COMPLETION, 1),
                new EventParticipant("Kevin Parker", selfDeclaration ? EventParticipant.Status.VALIDATED
                        : EventParticipant.Status.ASSIGNED, 2));
    }
}
