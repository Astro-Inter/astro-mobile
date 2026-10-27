package com.example.astro_mobile.data.api.dto;

public final class ChatConversationData {
    public String id;
    public ChatParticipantData participant;
    public ChatLastMessageData lastMessage;
    public int unreadCount;

    public boolean isValid() {
        return id != null && !id.isEmpty() && participant != null
                && participant.id != null && participant.name != null && participant.role != null
                && lastMessage != null && lastMessage.senderId != null && lastMessage.content != null;
    }
}
