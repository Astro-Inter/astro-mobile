package com.example.astro_mobile.manager.staff.events;

import com.example.astro_mobile.manager.events.ManagerEvent;

public final class StaffEvent {
    public enum Status { PENDING, DONE, EXPIRED, ANALYSIS, REJECTED, CANCELLED }

    public final long id;
    public final String title;
    public final String deadline;
    public final Status status;
    public final ManagerEvent.CompletionMode completionMode;

    public StaffEvent(long id, String title, String deadline, Status status,
                      ManagerEvent.CompletionMode completionMode) {
        this.id = id;
        this.title = title;
        this.deadline = deadline;
        this.status = status;
        this.completionMode = completionMode;
    }
}
