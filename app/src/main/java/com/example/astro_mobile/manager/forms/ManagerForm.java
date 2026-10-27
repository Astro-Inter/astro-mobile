package com.example.astro_mobile.manager.forms;
public final class ManagerForm {
    public enum Status { NEW, PENDING, ANALYSIS, APPROVED, REJECTED }
    public final String id, title, description, deadline, nr;
    public final Status status;
    public final boolean finished, requiresUnitLocation;
    public ManagerForm(String id, String title, String description, String deadline, String nr,
                       Status status, boolean finished, boolean requiresUnitLocation) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.nr = nr;
        this.status = status;
        this.finished = finished;
        this.requiresUnitLocation = requiresUnitLocation;
    }
}
