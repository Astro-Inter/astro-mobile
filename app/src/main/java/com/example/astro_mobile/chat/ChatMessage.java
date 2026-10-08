package com.example.astro_mobile.chat;

public final class ChatMessage {
    private final String text;
    private final boolean fromUser;
    private final boolean failed;
    private final long createdAt;

    public ChatMessage(String text, boolean fromUser) {
        this(text, fromUser, false);
    }

    public ChatMessage(String text, boolean fromUser, boolean failed) {
        this(text, fromUser, failed, System.currentTimeMillis());
    }

    private ChatMessage(String text, boolean fromUser, boolean failed, long createdAt) {
        this.text = text;
        this.fromUser = fromUser;
        this.failed = failed;
        this.createdAt = createdAt;
    }

    public String getText() { return text; }
    public boolean isFromUser() { return fromUser; }
    public boolean hasFailed() { return failed; }
    public long getCreatedAt() { return createdAt; }

    public static ChatMessage fromHistory(String text, boolean fromUser) {
        return new ChatMessage(text, fromUser, false, 0);
    }

    public ChatMessage withFailure(boolean failed) {
        return new ChatMessage(text, fromUser, failed, createdAt);
    }
}
