package com.example.astro_mobile.employee.events;

import java.time.LocalDate;

public final class EmployeeEvent {
    public enum Status { PENDING, REVIEW, DONE, REJECTED, CANCELLED, EXPIRED }

    public final String title;
    public final LocalDate date;
    public final Status status;
    public final boolean urgent;
    public final Long id;
    public final EventStatusClassifier.DisplayStatus displayStatus;

    public EmployeeEvent(String title, LocalDate date, Status status, boolean urgent) {
        this.title = title;
        this.date = date;
        this.status = status;
        this.urgent = urgent;
        this.id = null;
        this.displayStatus = urgent ? EventStatusClassifier.DisplayStatus.URGENT
                : status == Status.DONE ? EventStatusClassifier.DisplayStatus.DONE
                : status == Status.REVIEW ? EventStatusClassifier.DisplayStatus.ANALYSIS
                : EventStatusClassifier.DisplayStatus.PENDING;
    }

    public EmployeeEvent(Long id, String title, LocalDate date,
                         EventStatusClassifier.DisplayStatus displayStatus) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.displayStatus = displayStatus;
        this.urgent = displayStatus == EventStatusClassifier.DisplayStatus.URGENT;
        switch (displayStatus) {
            case DONE: status = Status.DONE; break;
            case ANALYSIS: status = Status.REVIEW; break;
            case REJECTED: status = Status.REJECTED; break;
            case CANCELLED: status = Status.CANCELLED; break;
            case EXPIRED: status = Status.EXPIRED; break;
            default: status = Status.PENDING;
        }
    }
}
