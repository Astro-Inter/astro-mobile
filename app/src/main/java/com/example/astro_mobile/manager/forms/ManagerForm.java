package com.example.astro_mobile.manager.forms;

public final class ManagerForm {
    public enum Status { URGENT, NEW, PENDING, FINISHED }

    public final String title;
    public final String deadline;
    public final String nr;
    public final Status status;

    public ManagerForm(String title, String deadline, String nr, Status status) {
        this.title = title;
        this.deadline = deadline;
        this.nr = nr;
        this.status = status;
    }
}
