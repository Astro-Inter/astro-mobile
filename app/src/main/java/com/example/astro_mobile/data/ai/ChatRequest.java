package com.example.astro_mobile.data.ai;

import com.google.gson.annotations.SerializedName;

public final class ChatRequest {
    public static final int MAX_MESSAGE_LENGTH = 4000;
    private final String message;
    @SerializedName("session_id") private final String sessionId;

    public ChatRequest(String message, String sessionId) {
        this.message = message;
        this.sessionId = sessionId;
    }
}
