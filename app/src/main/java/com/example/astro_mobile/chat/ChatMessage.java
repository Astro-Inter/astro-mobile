package com.example.astro_mobile.chat;

public final class ChatMessage {
    private final String text;
    private final boolean fromUser;
    private final boolean failed;

    public ChatMessage(String text, boolean fromUser) {
        this(text, fromUser, false);
    }

    public ChatMessage(String text, boolean fromUser, boolean failed) {
        this.text = text;
        this.fromUser = fromUser;
        this.failed = failed;
    }

    public String getText() { return text; }
    public boolean isFromUser() { return fromUser; }
    public boolean hasFailed() { return failed; }
}
