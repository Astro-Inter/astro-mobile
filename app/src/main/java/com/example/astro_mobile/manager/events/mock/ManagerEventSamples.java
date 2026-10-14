package com.example.astro_mobile.manager.events.mock;

import com.example.astro_mobile.manager.events.ManagerEvent;

import java.util.Arrays;
import java.util.List;

public final class ManagerEventSamples {
    private ManagerEventSamples() { }

    public static List<ManagerEvent> events() {
        // Um exemplo por estado do Figma; nenhum dado vem da API.
        return Arrays.asList(
                new ManagerEvent("Treinamento de EPI", ManagerEvent.Status.VALIDATION_PENDING, 4),
                new ManagerEvent("Treinamento de EPI", ManagerEvent.Status.ASSIGNMENT_PENDING, 0),
                new ManagerEvent("Treinamento de EPI", ManagerEvent.Status.NO_ACTION, 0),
                new ManagerEvent("Treinamento de EPI", ManagerEvent.Status.FINISHED, 0));
    }
}
