package com.example.astro_mobile.chat.human.mock;

import com.example.astro_mobile.chat.human.HumanConversation;

import java.util.Arrays;
import java.util.List;

public final class HumanChatSamples {
    private HumanChatSamples() { }

    public static List<HumanConversation> contacts() {
        // Contatos de demonstração, sem mensagens recebidas ou conversas pré-criadas na lista.
        return Arrays.asList(
                new HumanConversation("Tyler, the Creator", "Engenheiro de segurança", true, 0),
                new HumanConversation("Kevin Parker", "Eletricista", false, 0),
                new HumanConversation("Kevin Parker", "Analista de dados", false, 1),
                new HumanConversation("Kevin Parker", "Tech Lead", false, 2));
    }
}
