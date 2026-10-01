package com.example.astro_mobile.data.api.dto;

public class VerifyKeyRequest {
    private final String email;
    private final String accessKey;

    public VerifyKeyRequest(String email, String accessKey) {
        this.email = email;
        this.accessKey = accessKey;
    }
}
