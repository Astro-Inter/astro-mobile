package com.example.astro_mobile.data.api.dto;

public class ActivateRequest {
    private final String email;
    private final String firebaseUid;

    public ActivateRequest(String email, String firebaseUid) {
        this.email = email;
        this.firebaseUid = firebaseUid;
    }
}
