package com.example.astro_mobile.data.api.dto;

public final class ReceivedFormDetails {
    public Form form;
    public String senderName, recipientName, unitName;
    public Review review;

    public static final class Form {
        public String id, title, description, dueDate, status;
        public Long unitId;
        public UnitLocation unitLocation;
    }
    public static final class UnitLocation {
        public Double latitude, longitude;
        public boolean isValid() {
            return latitude != null && longitude != null && Double.isFinite(latitude) && Double.isFinite(longitude)
                    && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
        }
    }
    public static final class Review {
        public String status, description;
    }
}
