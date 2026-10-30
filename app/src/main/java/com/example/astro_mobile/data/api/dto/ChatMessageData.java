package com.example.astro_mobile.data.api.dto;

public final class ChatMessageData {
    public String id;
    public String conversationId;
    public Long senderId;
    public String content;
    public String replyToMessageId;
    public String createdAt;

    public boolean isValid(String expectedConversationId) {
        try {
            java.time.Instant.parse(createdAt);
            return id != null && !id.isEmpty() && expectedConversationId.equals(conversationId)
                    && senderId != null && content != null;
        } catch (RuntimeException invalid) { return false; }
    }
}
