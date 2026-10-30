package com.example.astro_mobile.data.api.dto;

import java.time.Instant;
import java.util.UUID;

public final class ChatTypingData {
    public String conversationId, expiresAt;
    public Long senderId;
    public Boolean typing;

    public boolean isValid() {
        try {
            return UUID.fromString(conversationId).toString().equals(conversationId)
                    && senderId != null && senderId > 0 && typing != null && Instant.parse(expiresAt) != null;
        } catch (RuntimeException invalid) { return false; }
    }
}
