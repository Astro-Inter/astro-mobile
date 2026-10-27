package com.example.astro_mobile.chat.human;

import androidx.lifecycle.ViewModel;

import com.example.astro_mobile.chat.human.mock.HumanChatSamples;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.HumanChatRepository;
import com.example.astro_mobile.data.api.dto.ChatConversationData;

import java.text.Normalizer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HumanConversationsViewModel extends ViewModel {
    private final HumanChatRepository repository = HumanChatRepository.createDefault();
    private final List<HumanConversation> conversations = new ArrayList<>();
    private final List<HumanConversation> contacts = new ArrayList<>(HumanChatSamples.contacts());
    private boolean loading;
    private boolean loaded;
    private FailureKind failure;

    public boolean isLoading() { return loading; }
    public boolean isLoaded() { return loaded; }
    public FailureKind getFailure() { return failure; }
    public void clearFailure() { failure = null; }

    public void loadConversations(Runnable onChanged) {
        loading = true;
        failure = null;
        onChanged.run();
        repository.load(new HumanChatRepository.ResultCallback() {
            @Override public void onSuccess(List<ChatConversationData> result) {
                conversations.clear();
                // Mantém a ordem da API e usa a última mensagem apenas como prévia do card.
                for (ChatConversationData data : result) {
                    boolean manager = data.participant.role.startsWith("GESTOR");
                    HumanConversation conversation = new HumanConversation(data.participant.name,
                            manager ? "Gestor" : "Colaborador", manager, 0);
                    conversation.conversationId = data.id;
                    conversation.profilePhotoUrl = data.participant.profilePhotoUrl;
                    conversation.avatarInitials = data.participant.avatarInitials;
                    conversation.lastMessageText = data.lastMessage.content;
                    conversation.lastMessageSent = !data.lastMessage.senderId.equals(data.participant.id);
                    conversation.unreadCount = data.unreadCount;
                    conversations.add(conversation);
                }
                loading = false;
                loaded = true;
                onChanged.run();
            }

            @Override public void onFailure(FailureKind result) {
                loading = false;
                failure = result;
                onChanged.run();
            }
        });
    }

    public void cancelLoad() {
        repository.cancel();
        loading = false;
    }

    public HumanConversation find(String id) {
        for (HumanConversation conversation : conversations) {
            if (conversation.conversationId.equals(id)) return conversation;
        }
        return null;
    }

    public List<HumanConversation> visible(boolean managerFlow, boolean contacts, String query) {
        // A busca é local; contatos ainda usam a demonstração da tarefa visual.
        List<HumanConversation> result = new ArrayList<>();
        for (HumanConversation conversation : contacts ? this.contacts : conversations) {
            if (contacts && !managerFlow && !conversation.manager) continue;
            if (matches(conversation.name, query)) result.add(conversation);
        }
        return result;
    }

    public HumanConversation open(String name, String role, boolean manager, int avatarIndex) {
        // O detalhe do funcionário e a pesquisa reutilizam a mesma conversa mock.
        for (HumanConversation conversation : contacts) {
            if (conversation.name.equals(name) && conversation.role.equals(role)) return conversation;
        }
        HumanConversation conversation = new HumanConversation(name, role, manager, avatarIndex);
        contacts.add(conversation);
        return conversation;
    }

    public boolean send(HumanConversation conversation) {
        if (conversation.conversationId != null) return false;
        String text = conversation.draft.trim();
        if (text.isEmpty()) return false;
        // O envio da demonstração permanece local até existir a operação no backend.
        conversation.messages.add(new HumanMessage(text,
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), true));
        conversation.draft = "";
        return true;
    }

    public void clearSession() {
        // Descarta mensagens e rascunhos ao voltar à identificação, inclusive após logout.
        cancelLoad();
        conversations.clear();
        contacts.clear();
        contacts.addAll(HumanChatSamples.contacts());
        loaded = false;
        failure = null;
    }

    public static boolean matches(String value, String query) {
        return normalize(value).contains(normalize(query.trim()));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    @Override protected void onCleared() {
        repository.cancel();
    }
}
