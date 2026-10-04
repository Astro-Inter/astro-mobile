package com.example.astro_mobile.chat;

public final class ChatMessage {
    private final String text;
    private final boolean fromUser;

    public ChatMessage(String text, boolean fromUser) {
        this.text = text;
        this.fromUser = fromUser;
    }

    public String getText() { return text; }
    public boolean isFromUser() { return fromUser; }
}
