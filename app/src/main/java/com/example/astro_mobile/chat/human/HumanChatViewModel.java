package com.example.astro_mobile.chat.human;

import androidx.lifecycle.ViewModel;

import com.example.astro_mobile.chat.human.mock.HumanChatSamples;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class HumanChatViewModel extends ViewModel {
    private final List<HumanMessage> messages = new ArrayList<>();
    private boolean initialized;
    private boolean loaded;
    private String draft = "";

    public void initialize(boolean manager) {
        // Não recria a conversa nem perde as mensagens locais ao recriar a View.
        if (initialized) return;
        messages.addAll(HumanChatSamples.messages(manager));
        initialized = true;
    }

    public List<HumanMessage> getMessages() { return messages; }
    public String getDraft() { return draft; }
    public void setDraft(String draft) { this.draft = draft; }
    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }

    public boolean send() {
        // Acrescenta só uma mensagem local, sem rede, resposta automática ou confirmação de entrega.
        String text = draft.trim();
        if (text.isEmpty()) return false;
        messages.add(new HumanMessage(text, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), true));
        draft = "";
        return true;
    }
}
