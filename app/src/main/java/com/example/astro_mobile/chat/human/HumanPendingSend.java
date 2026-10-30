package com.example.astro_mobile.chat.human;

import com.example.astro_mobile.data.api.dto.ChatSendRequest;
import java.util.UUID;

/** Mantém a mesma operação em tentativas manuais após confirmação incerta. */
public final class HumanPendingSend {
    public final ChatSendRequest request;
    public boolean awaiting = true;

    public HumanPendingSend(HumanConversation conversation) {
        request = new ChatSendRequest(conversation.participantId, conversation.conversationId,
                conversation.draft, UUID.randomUUID().toString(), conversation.replyToMessageId);
    }
}
