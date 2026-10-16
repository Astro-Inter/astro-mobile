package com.example.astro_mobile.chat.human.mock;

import com.example.astro_mobile.chat.human.HumanMessage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class HumanChatSamples {
    private HumanChatSamples() { }

    public static List<HumanMessage> messages(boolean manager) {
        // Textos e horários são exemplos literais do Figma, não uma conversa real.
        if (manager) return Arrays.asList(
                new HumanMessage("Tu não fez o treinamento?", "20:99", true),
                new HumanMessage("Esqueci, desculpa", "20:99", false),
                new HumanMessage("De novo?", "20:99", true),
                new HumanMessage("Desculpa, pfv", "20:99", false),
                new HumanMessage("Próximo evento que você não fizer terei que te advertir", "20:99", true));
        return Collections.singletonList(new HumanMessage(
                "Já é o terceiro evento q vc não conclui, na próxima vez, terei que te advertir", "20:99", false));
    }
}
