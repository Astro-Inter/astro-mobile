package com.example.astro_mobile.manager.events;

public final class ManagerEvent {
    public enum Status { VALIDATION_PENDING, ASSIGNMENT_PENDING, NO_ACTION, FINISHED, CANCELLED }
    public enum CompletionMode { SELF_DECLARATION, MANAGER_ASSIGNMENT }

    public final long id;
    public final String title;
    public final Status status;
    public final long pendingValidations;
    public final CompletionMode completionMode;

    public ManagerEvent(long id, String title, Status status, long pendingValidations, CompletionMode completionMode) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.pendingValidations = pendingValidations;
        this.completionMode = completionMode;
    }
}
