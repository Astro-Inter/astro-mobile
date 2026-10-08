package com.example.astro_mobile.data.ai;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public final class SessionHistory {
    @SerializedName("session_id") public String sessionId;
    public String status;
    public Integer total;
    @SerializedName("mensagens") public List<Message> messages;

    public static final class Message {
        public String role;
        public String content;
    }

    public boolean isValid() {
        if (!SessionSummary.validId(sessionId) || !SessionSummary.validStatus(status)
                || messages == null || total == null || total != messages.size()) return false;
        for (Message message : messages) {
            if (message == null || message.content == null
                    || !("user".equals(message.role) || "assistant".equals(message.role))) return false;
        }
        return true;
    }
}
