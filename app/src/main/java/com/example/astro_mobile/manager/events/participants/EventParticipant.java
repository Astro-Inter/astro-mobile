package com.example.astro_mobile.manager.events.participants;

public final class EventParticipant {
    public enum Status { VALIDATION_PENDING, AWAITING_COMPLETION, VALIDATED, ASSIGNED }

    public final String name;
    public final Status status;
    public final int avatarIndex;

    public EventParticipant(String name, Status status, int avatarIndex) {
        this.name = name;
        this.status = status;
        this.avatarIndex = avatarIndex;
    }
}
