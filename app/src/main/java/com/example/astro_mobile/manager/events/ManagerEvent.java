package com.example.astro_mobile.manager.events;

public final class ManagerEvent {
    public enum Status { VALIDATION_PENDING, ASSIGNMENT_PENDING, NO_ACTION, FINISHED }
    public enum CompletionMode { SELF_DECLARATION, MANAGER_ASSIGNMENT }

    public final String title;
    public final Status status;
    public final int pendingValidations;
    public final CompletionMode completionMode;

    public ManagerEvent(String title, Status status, int pendingValidations, CompletionMode completionMode) {
        this.title = title;
        this.status = status;
        this.pendingValidations = pendingValidations;
        this.completionMode = completionMode;
    }
}
