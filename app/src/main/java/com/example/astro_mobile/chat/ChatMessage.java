package com.example.astro_mobile.chat;

public final class ChatMessage {
    private final String text;
    private final com.google.gson.JsonElement blocks;
    private final boolean fromUser;
    private final boolean failed;
    private final long createdAt;

    public ChatMessage(String text, boolean fromUser) {
        this(text, fromUser, false);
    }

    public ChatMessage(String text, boolean fromUser, boolean failed) {
        this(text, fromUser, failed, System.currentTimeMillis(), null);
    }

    private ChatMessage(String text, boolean fromUser, boolean failed, long createdAt, com.google.gson.JsonElement blocks) {
        this.text = text;
        this.blocks = blocks == null ? null : blocks.deepCopy();
        this.fromUser = fromUser;
        this.failed = failed;
        this.createdAt = createdAt;
    }

    public String getText() { return text; }
    public com.google.gson.JsonElement getBlocks() { return blocks; }
    public static ChatMessage fromResponse(String text, com.google.gson.JsonElement blocks) {
        return new ChatMessage(text, false, false, System.currentTimeMillis(), blocks);
    }
    public boolean isFromUser() { return fromUser; }
    public boolean hasFailed() { return failed; }
    public long getCreatedAt() { return createdAt; }

    public static ChatMessage fromHistory(String text, boolean fromUser) {
        return new ChatMessage(text, fromUser, false, 0, null);
    }

    public static ChatMessage fromHistory(String text, boolean fromUser, com.google.gson.JsonElement blocks) {
        return new ChatMessage(text, fromUser, false, 0, fromUser ? null : blocks);
    }

    public ChatMessage withFailure(boolean failed) {
        return new ChatMessage(text, fromUser, failed, createdAt, blocks);
    }
}
