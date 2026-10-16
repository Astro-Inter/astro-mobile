package com.example.astro_mobile.manager.forms;

public final class ManagerForm {
    public enum Status { URGENT, NEW, PENDING, FINISHED }

    public final String title;
    public final String deadline;
    public final String nr;
    public final Status status;
    public final boolean locationMatchesUnit;
    public final boolean requiresUnitLocation;

    public ManagerForm(String title, String deadline, String nr, Status status,
                       boolean requiresUnitLocation, boolean locationMatchesUnit) {
        this.title = title;
        this.deadline = deadline;
        this.nr = nr;
        this.status = status;
        this.locationMatchesUnit = locationMatchesUnit;
        this.requiresUnitLocation = requiresUnitLocation;
    }
}
