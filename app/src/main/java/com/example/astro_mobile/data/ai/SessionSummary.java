package com.example.astro_mobile.data.ai;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.UUID;

public final class SessionSummary {
    @SerializedName("session_id") public String sessionId;
    public String title;
    @SerializedName("last_message_preview") public String preview;
    @SerializedName("created_at") public String createdAt;
    @SerializedName("updated_at") public String updatedAt;
    public String status;

    public static boolean validId(String id) {
        try { return id != null && UUID.fromString(id).toString().equalsIgnoreCase(id); }
        catch (IllegalArgumentException error) { return false; }
    }

    public static boolean validStatus(String status) {
        return "ativa".equals(status) || "encerrando".equals(status) || "encerrada".equals(status);
    }

    public boolean isValid() {
        try {
            Instant.parse(createdAt);
            Instant.parse(updatedAt);
            return validId(sessionId) && validStatus(status) && title != null && preview != null;
        } catch (RuntimeException error) { return false; }
    }
}
