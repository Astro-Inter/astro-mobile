package com.example.astro_mobile.data.api.dto;

/** Contrato de /app/chat.send. Nunca inclui identidade do remetente ou token. */
public final class ChatSendRequest {
    public final Long recipientId;
    public final String conversationId;
    public final String content;
    public final String replyToMessageId;
    public final String clientMessageId;

    public ChatSendRequest(Long recipientId, String conversationId, String content, String clientMessageId) {
        this(recipientId, conversationId, content, clientMessageId, null);
    }

    public ChatSendRequest(Long recipientId, String conversationId, String content, String clientMessageId, String replyToMessageId) {
        this.recipientId = conversationId == null ? recipientId : null;
        this.conversationId = conversationId;
        this.content = content;
        this.replyToMessageId = replyToMessageId;
        this.clientMessageId = clientMessageId;
    }
}
