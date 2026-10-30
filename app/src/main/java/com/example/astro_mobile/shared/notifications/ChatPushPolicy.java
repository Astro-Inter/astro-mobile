package com.example.astro_mobile.shared.notifications;

import com.example.astro_mobile.data.api.dto.ChatPushData;

public final class ChatPushPolicy {
    private ChatPushPolicy() { }
    public static boolean shouldDisplay(ChatPushData message, boolean currentUser, boolean foreground, boolean duplicate) {
        return message != null && currentUser && !foreground && !duplicate;
    }
}
