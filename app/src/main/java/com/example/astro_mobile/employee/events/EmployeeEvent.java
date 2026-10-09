package com.example.astro_mobile.employee.events;

import java.time.LocalDate;

public final class EmployeeEvent {
    public enum Status { PENDING, REVIEW, DONE }

    public final String title;
    public final LocalDate date;
    public final Status status;
    public final boolean urgent;

    public EmployeeEvent(String title, LocalDate date, Status status, boolean urgent) {
        this.title = title;
        this.date = date;
        this.status = status;
        this.urgent = urgent;
    }
}
