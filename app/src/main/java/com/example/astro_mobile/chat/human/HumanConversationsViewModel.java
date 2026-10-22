package com.example.astro_mobile.chat.human;

import androidx.lifecycle.ViewModel;

import com.example.astro_mobile.chat.human.mock.HumanChatSamples;

import java.text.Normalizer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HumanConversationsViewModel extends ViewModel {
    private final List<HumanConversation> conversations = new ArrayList<>(HumanChatSamples.contacts());

    public List<HumanConversation> visible(boolean managerFlow, boolean contacts, String query) {
        // Filtra todos os mocks em memória, sem chamadas por caractere.
        List<HumanConversation> result = new ArrayList<>();
        for (HumanConversation conversation : conversations) {
            if (!managerFlow && !conversation.manager) continue;
            if (!contacts && conversation.messages.isEmpty()) continue;
            if (matches(conversation.name, query)) result.add(conversation);
        }
        return result;
    }

    public HumanConversation open(String name, String role, boolean manager, int avatarIndex) {
        // O detalhe do funcionário e a pesquisa reutilizam a mesma conversa mock.
        for (HumanConversation conversation : conversations) {
            if (conversation.name.equals(name) && conversation.role.equals(role)) return conversation;
        }
        HumanConversation conversation = new HumanConversation(name, role, manager, avatarIndex);
        conversations.add(conversation);
        return conversation;
    }

    public boolean send(HumanConversation conversation) {
        String text = conversation.draft.trim();
        if (text.isEmpty()) return false;
        // Só o primeiro envio faz o contato passar a aparecer na lista de conversas.
        conversation.messages.add(new HumanMessage(text,
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), true));
        conversation.draft = "";
        return true;
    }

    public void clearSession() {
        // Descarta mensagens e rascunhos ao voltar à identificação, inclusive após logout.
        conversations.clear();
        conversations.addAll(HumanChatSamples.contacts());
    }

    public static boolean matches(String value, String query) {
        return normalize(value).contains(normalize(query.trim()));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
