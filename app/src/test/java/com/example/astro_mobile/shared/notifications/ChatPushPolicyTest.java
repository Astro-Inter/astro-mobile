package com.example.astro_mobile.shared.notifications;

import com.example.astro_mobile.data.api.dto.ChatPushData;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ChatPushPolicyTest {
    private final Instant now = Instant.parse("2026-10-30T10:00:00Z");

    private Map<String, String> payload() {
        Map<String, String> data = new HashMap<>();
        data.put("type", "chat_message");
        data.put("uid", "recipient");
        data.put("sender_id", "1");
        data.put("sender_name", "Gestor");
        data.put("body", "Olá");
        data.put("created_at", now.toString());
        data.put("message_id", "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee");
        data.put("conversation_id", "11111111-2222-4333-8444-555555555555");
        return data;
    }

    @Test public void onlyNewBackgroundMessagesForCurrentAccountProduceAlerts() {
        ChatPushData message = ChatPushData.parse(payload(), now);
        assertNotNull(message);
        assertTrue(ChatPushPolicy.shouldDisplay(message, true, false, false));
        assertFalse(ChatPushPolicy.shouldDisplay(message, false, false, false));
        assertFalse(ChatPushPolicy.shouldDisplay(message, true, true, false));
        assertFalse(ChatPushPolicy.shouldDisplay(message, true, false, true));
        assertFalse(ChatPushPolicy.shouldDisplay(null, true, false, false));
    }

    @Test public void rejectsMalformedMissingAndOversizedFields() {
        for (String field : payload().keySet()) {
            Map<String, String> data = payload();
            data.remove(field);
            assertNull(field, ChatPushData.parse(data, now));
        }
        Map<String, String> data = payload();
        data.put("conversation_id", "1-1-1-1-1");
        assertNull(ChatPushData.parse(data, now));
        data = payload();
        data.put("sender_id", "9223372036854775808");
        assertNull(ChatPushData.parse(data, now));
        data = payload();
        data.put("body", "😀".repeat(161));
        assertNull(ChatPushData.parse(data, now));
        data.put("body", "😀".repeat(160));
        assertNotNull(ChatPushData.parse(data, now));
    }

    @Test public void rejectsExpiredAndFutureMessages() {
        Map<String, String> data = payload();
        data.put("created_at", now.minusSeconds(86401).toString());
        assertNull(ChatPushData.parse(data, now));
        data.put("created_at", now.plusSeconds(61).toString());
        assertNull(ChatPushData.parse(data, now));
        data.put("created_at", "invalid");
        assertNull(ChatPushData.parse(data, now));
    }
}
