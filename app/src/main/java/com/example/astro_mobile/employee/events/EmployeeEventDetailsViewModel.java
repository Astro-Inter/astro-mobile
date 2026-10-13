package com.example.astro_mobile.employee.events;

import androidx.lifecycle.ViewModel;

import com.example.astro_mobile.employee.events.mock.EmployeeEventSamples;

public class EmployeeEventDetailsViewModel extends ViewModel {
    private EmployeeEvent event;
    private boolean loaded;
    private boolean evidenceAttached = true;

    public void initialize(EmployeeEvent selectedEvent) {
        // Guarda o evento uma vez para preservar o estado ao recriar as Views.
        if (event == null) event = selectedEvent;
    }

    public EmployeeEvent getEvent() { return event; }
    public String getDescription() { return EmployeeEventSamples.descriptionFor(event.title); }
    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }
    public boolean isEvidenceAttached() { return evidenceAttached; }
    public void setEvidenceAttached(boolean attached) { evidenceAttached = attached; }
}
