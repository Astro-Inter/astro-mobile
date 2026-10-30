package com.example.astro_mobile.chat.human;

import static org.junit.Assert.*;
import org.junit.Test;

public final class HumanConversationTest {
    @Test public void liveAndHistoryOverlapDeduplicateByServerIdInChronologicalOrder() {
        HumanConversation conversation = new HumanConversation("Contato", "Gestor", true, 0);
        HumanMessage recent = message("b", "2026-10-26T18:30:01Z");
        HumanMessage older = message("a", "2026-10-26T18:30:00Z");
        assertTrue(conversation.addMessage(recent));
        assertTrue(conversation.addMessage(older));
        assertFalse(conversation.addMessage(message("b", "2026-10-26T18:30:01Z")));
        assertEquals(2, conversation.messages.size());
        assertSame(older, conversation.messages.get(0));
        assertSame(recent, conversation.messages.get(1));
    }
    private static HumanMessage message(String id, String instant) {
        HumanMessage message = new HumanMessage("Conteúdo", "15:30", false);
        message.id = id; message.createdAt = instant; return message;
    }
}
