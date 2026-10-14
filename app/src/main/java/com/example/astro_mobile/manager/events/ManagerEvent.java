package com.example.astro_mobile.manager.events;

public final class ManagerEvent {
    public enum Status { VALIDATION_PENDING, ASSIGNMENT_PENDING, NO_ACTION, FINISHED }

    public final String title;
    public final Status status;
    public final int pendingValidations;

    public ManagerEvent(String title, Status status, int pendingValidations) {
        this.title = title;
        this.status = status;
        this.pendingValidations = pendingValidations;
    }
}
