package com.example.astro_mobile.chat.human;

import java.util.ArrayList;
import java.util.List;

public final class HumanConversation {
    public String conversationId;
    public String profilePhotoUrl;
    public String avatarInitials;
    public String lastMessageText;
    public boolean lastMessageSent;
    public int unreadCount;
    public final String name;
    public final String role;
    public final boolean manager;
    public final int avatarIndex;
    public final List<HumanMessage> messages = new ArrayList<>();
    public String draft = "";
    public boolean loaded;

    public HumanConversation(String name, String role, boolean manager, int avatarIndex) {
        this.name = name;
        this.role = role;
        this.manager = manager;
        this.avatarIndex = avatarIndex;
    }
}
