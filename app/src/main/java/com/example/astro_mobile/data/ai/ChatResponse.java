package com.example.astro_mobile.data.ai;

import com.google.gson.annotations.SerializedName;

public final class ChatResponse {
    @SerializedName("session_id") private String sessionId;
    @SerializedName("resposta") private String answer;

    public String getSessionId() { return sessionId; }
    public String getAnswer() { return answer; }

    public boolean isValid() {
        return sessionId != null
                && sessionId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                && answer != null && !answer.trim().isEmpty();
    }
}
