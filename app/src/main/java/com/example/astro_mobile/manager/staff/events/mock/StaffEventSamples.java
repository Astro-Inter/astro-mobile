package com.example.astro_mobile.manager.staff.events.mock;

import com.example.astro_mobile.manager.staff.events.StaffEvent;

import java.util.Arrays;
import java.util.List;

public final class StaffEventSamples {
    private StaffEventSamples() { }

    public static List<StaffEvent> events() {
        // São os três exemplos visuais do Figma; o estado não é calculado a partir da data.
        return Arrays.asList(
                new StaffEvent("Treinamento de EPI", "6/07/2067", StaffEvent.Status.PENDING),
                new StaffEvent("Treinamento de EPI", "6/07/2000", StaffEvent.Status.DONE),
                new StaffEvent("Treinamento de EPI", "6/07/2000", StaffEvent.Status.EXPIRED));
    }
}
