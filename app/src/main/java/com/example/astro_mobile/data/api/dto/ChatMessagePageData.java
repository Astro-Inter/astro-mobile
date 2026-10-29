package com.example.astro_mobile.data.api.dto;

import java.util.List;

public final class ChatMessagePageData {
    public List<ChatMessageData> messages;
    public String nextCursor;
    public boolean hasMore;

    public boolean isValid(String conversationId) {
        if (messages == null) return false;
        for (ChatMessageData message : messages) {
            if (message == null || !message.isValid(conversationId)) return false;
        }
        return !hasMore || (nextCursor != null && !nextCursor.isEmpty());
    }
}
