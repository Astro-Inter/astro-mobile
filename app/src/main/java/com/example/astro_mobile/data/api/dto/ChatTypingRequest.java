package com.example.astro_mobile.data.api.dto;

public final class ChatTypingRequest {
    public final Long recipientId;
    public final String conversationId;
    public final boolean typing;

    public ChatTypingRequest(Long recipientId, String conversationId, boolean typing) {
        this.recipientId = conversationId == null ? recipientId : null;
        this.conversationId = conversationId;
        this.typing = typing;
    }
}
