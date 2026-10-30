package com.example.astro_mobile.chat.human;

import static org.junit.Assert.*;
import com.google.gson.Gson;
import org.junit.Test;

public final class HumanPendingSendTest {
    @Test public void firstMessageUsesInternalRecipientAndKeepsOperationAndOriginalContentOnRetry() {
        HumanConversation conversation = new HumanConversation("Contato autorizado", "Gestor", true, 0);
        conversation.participantId = 25L;
        conversation.draft = "  Olá!\nTudo bem?  ";
        HumanPendingSend pending = new HumanPendingSend(conversation);
        String original = new Gson().toJson(pending.request);
        pending.awaiting = false;
        conversation.conversationId = "11111111-2222-4333-8444-555555555555";
        conversation.draft = "outro rascunho";
        assertEquals(original, new Gson().toJson(pending.request));
        assertEquals(Long.valueOf(25), pending.request.recipientId);
        assertNull(pending.request.conversationId);
        assertEquals("  Olá!\nTudo bem?  ", pending.request.content);
        assertNotNull(java.util.UUID.fromString(pending.request.clientMessageId));
    }
    @Test public void existingConversationUsesOnlyConversationId() {
        HumanConversation conversation = new HumanConversation("Contato autorizado", "Colaborador", false, 0);
        conversation.participantId = 25L;
        conversation.conversationId = "11111111-2222-4333-8444-555555555555";
        conversation.draft = "Mensagem";
        HumanPendingSend pending = new HumanPendingSend(conversation);
        assertNull(pending.request.recipientId);
        assertEquals(conversation.conversationId, pending.request.conversationId);
        assertFalse(new Gson().toJson(pending.request).contains("senderId"));
    }
}
