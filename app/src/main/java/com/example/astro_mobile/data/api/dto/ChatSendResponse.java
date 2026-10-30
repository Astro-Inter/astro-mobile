package com.example.astro_mobile.data.api.dto;

/** Evento privado e confirmação usam os mesmos campos do histórico, mais a correlação. */
public final class ChatSendResponse {
    public String clientMessageId;
    public String id;
    public String conversationId;
    public Long senderId;
    public String content;
    public String replyToMessageId;
    public String createdAt;

    public ChatMessageData toMessage() {
        ChatMessageData result = new ChatMessageData();
        result.id = id;
        result.conversationId = conversationId;
        result.senderId = senderId;
        result.content = content;
        result.replyToMessageId = replyToMessageId;
        result.createdAt = createdAt;
        return result;
    }

    public boolean isValid() {
        try {
            java.util.UUID.fromString(id);
            java.util.UUID.fromString(conversationId);
            java.util.UUID.fromString(clientMessageId);
            java.time.Instant.parse(createdAt);
            return senderId != null && senderId > 0 && content != null && !content.codePoints().allMatch(Character::isWhitespace);
        } catch (RuntimeException invalid) { return false; }
    }
}
