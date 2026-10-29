package com.example.astro_mobile.data.api.dto;

public final class ChatMessageData {
    public String id;
    public String conversationId;
    public Long senderId;
    public String content;
    public String replyToMessageId;
    public String createdAt;

    public boolean isValid(String expectedConversationId) {
        return id != null && !id.isEmpty() && expectedConversationId.equals(conversationId)
                && senderId != null && content != null && createdAt != null && !createdAt.isEmpty();
    }
}
