package com.example.astro_mobile.data.api.dto;

public final class ManagerEventData {
    public Long id;
    public String title;
    public String status;
    public String completionMode;
    /** Deadline of the last event group; used to classify active events locally. */
    public String lastClassLimitDate;
    public Long pendingCount;
}
