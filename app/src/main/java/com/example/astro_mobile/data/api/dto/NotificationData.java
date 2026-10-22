package com.example.astro_mobile.data.api.dto;

public final class NotificationData {
    private String id;
    private String title;
    private Boolean viewed;
    private Long hours;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public boolean isViewed() { return Boolean.TRUE.equals(viewed); }
    public void markViewed() { viewed = true; }
    public long getHours() { return hours == null ? 0 : Math.max(0, hours); }

    public boolean isValid() {
        return id != null && !id.trim().isEmpty() && title != null
                && !title.trim().isEmpty() && viewed != null && hours != null && hours >= 0;
    }
}
