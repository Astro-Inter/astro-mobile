package com.example.astro_mobile.data.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class ChatPushData {
    public final String uid, messageId, conversationId, senderName, preview;
    public final long senderId;
    public final Instant createdAt;

    private ChatPushData(Map<String, String> data) {
        uid = data.get("uid");
        messageId = data.get("message_id");
        conversationId = data.get("conversation_id");
        senderId = Long.parseLong(data.get("sender_id"));
        senderName = data.get("sender_name");
        preview = data.get("body");
        createdAt = Instant.parse(data.get("created_at"));
    }

    public static ChatPushData parse(Map<String, String> data, Instant now) {
        if (!"chat_message".equals(data.get("type"))) return null;
        try {
            ChatPushData result = new ChatPushData(data);
            if (result.uid == null || result.uid.isEmpty() || result.uid.length() > 128 || result.senderId <= 0
                    || !UUID.fromString(result.messageId).toString().equals(result.messageId)
                    || !UUID.fromString(result.conversationId).toString().equals(result.conversationId)
                    || result.senderName == null || result.senderName.trim().isEmpty()
                    || result.senderName.codePointCount(0, result.senderName.length()) > 80
                    || result.preview == null || result.preview.trim().isEmpty()
                    || result.preview.codePointCount(0, result.preview.length()) > 160
                    || result.createdAt.isBefore(now.minusSeconds(86400))
                    || result.createdAt.isAfter(now.plusSeconds(60))) return null;
            return result;
        } catch (RuntimeException invalid) { return null; }
    }
}
