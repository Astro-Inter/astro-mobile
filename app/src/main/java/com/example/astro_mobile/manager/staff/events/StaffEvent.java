package com.example.astro_mobile.manager.staff.events;

public final class StaffEvent {
    public enum Status { PENDING, DONE, EXPIRED }

    public final String title;
    public final String deadline;
    public final Status status;

    public StaffEvent(String title, String deadline, Status status) {
        this.title = title;
        this.deadline = deadline;
        this.status = status;
    }
}
