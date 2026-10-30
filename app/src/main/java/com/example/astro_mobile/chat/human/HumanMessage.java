package com.example.astro_mobile.chat.human;

public final class HumanMessage {
    public final String text;
    public final String time;
    public final boolean sent;
    public String id;
    public String createdAt;

    public HumanMessage(String text, String time, boolean sent) {
        this.text = text;
        this.time = time;
        this.sent = sent;
    }
}
