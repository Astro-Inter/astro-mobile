package com.example.astro_mobile.data.ai;

import com.google.gson.annotations.SerializedName;

public final class SessionResult {
    @SerializedName("session_id") public String sessionId;
    public String status;

    public boolean isValid() {
        return SessionSummary.validId(sessionId) && ("ativa".equals(status) || "encerrada".equals(status));
    }
}
