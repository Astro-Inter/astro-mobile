package com.example.astro_mobile.chat.human;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.astro_mobile.data.api.HumanChatContactsRepository;
import com.example.astro_mobile.data.api.dto.ChatContactData;
import com.example.astro_mobile.data.api.dto.ChatSendResponse;
import com.example.astro_mobile.data.api.dto.ChatSendError;
import com.example.astro_mobile.data.chat.AndroidChatConnection;
import com.example.astro_mobile.data.chat.ChatConnectionState;
import com.example.astro_mobile.data.chat.StompChatClient;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.HumanChatRepository;
import com.example.astro_mobile.data.api.HumanChatHistoryRepository;
import com.example.astro_mobile.data.api.dto.ChatConversationData;
import com.example.astro_mobile.data.api.dto.ChatMessageData;
import com.example.astro_mobile.data.api.dto.ChatMessagePageData;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HumanConversationsViewModel extends ViewModel {
    private final HumanChatRepository repository = HumanChatRepository.createDefault();
    private final HumanChatHistoryRepository historyRepository = HumanChatHistoryRepository.createDefault();
    private final HumanChatHistoryRepository readRepository = HumanChatHistoryRepository.createDefault();
    private final HumanChatContactsRepository contactsRepository = HumanChatContactsRepository.createDefault();
    private final List<HumanConversation> conversations = new ArrayList<>();
    private final List<HumanConversation> contacts = new ArrayList<>();
    private final MutableLiveData<Long> changes = new MutableLiveData<>(0L);
    private final MutableLiveData<ChatConnectionState> connectionState = new MutableLiveData<>(ChatConnectionState.STOPPED);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final java.util.Map<HumanConversation, Runnable> confirmations = new java.util.HashMap<>();
    private final AndroidChatConnection connection;
    private HumanConversation activeConversation;
    private boolean contactsLoading, contactsLoaded;
    private boolean loading;
    private boolean loaded;
    private FailureKind failure;

    public HumanConversationsViewModel(Context application) {
        connection = new AndroidChatConnection(application, new StompChatClient.Listener() {
            @Override public void onState(ChatConnectionState value) {
                connectionState.setValue(value);
                if (value != ChatConnectionState.CONNECTED) {
                    for (HumanConversation conversation : allConversations()) {
                        if (conversation.pending != null) conversation.pending.awaiting = false;
                    }
                } else {
                    // Redis Pub/Sub não guarda eventos offline: recupera lista e histórico após reconectar.
                    loadConversations(HumanConversationsViewModel.this::notifyChanged);
                    if (activeConversation != null && activeConversation.conversationId != null) {
                        loadHistory(activeConversation, backgroundHistoryCallback());
                    }
                }
                notifyChanged();
            }
            @Override public void onMessage(ChatSendResponse event) { receive(event); }
            @Override public void onError(ChatSendError error) {
                for (HumanConversation conversation : allConversations()) {
                    if (conversation.pending == null || !conversation.pending.request.clientMessageId.equals(error.clientMessageId)) continue;
                    cancelConfirmation(conversation);
                    conversation.sendErrorCode = error.code;
                    if (java.util.Set.of("UNREAD_COUNTER_UNAVAILABLE", "DELIVERY_UNAVAILABLE", "PERSISTENCE_UNAVAILABLE", "SEND_FAILED")
                            .contains(error.code)) conversation.pending.awaiting = false;
                    else conversation.pending = null;
                }
                if ("UNAUTHENTICATED".equals(error.code)) {
                    connection.stop();
                    connectionState.setValue(ChatConnectionState.SESSION_EXPIRED);
                }
                notifyChanged();
            }
        }, this::clearSession);
    }

    public LiveData<Long> changes() { return changes; }
    public LiveData<ChatConnectionState> connectionState() { return connectionState; }
    public void setForeground(boolean value) { connection.setForeground(value); }
    public void retryConnection() { connection.request(); connection.client().retry(); }
    public void setActiveConversation(HumanConversation conversation) {
        activeConversation = conversation;
        if (conversation != null) connection.request();
        else readRepository.cancel();
    }
    private void notifyChanged() { changes.setValue(changes.getValue() + 1); }
    private List<HumanConversation> allConversations() {
        List<HumanConversation> all = new ArrayList<>(conversations);
        for (HumanConversation contact : contacts) if (!all.contains(contact)) all.add(contact);
        return all;
    }
    private HistoryCallback backgroundHistoryCallback() {
        return new HistoryCallback() {
            @Override public void onChanged() { notifyChanged(); }
            @Override public void onFailure(FailureKind result) {
                if (result == FailureKind.SESSION) connectionState.setValue(ChatConnectionState.SESSION_EXPIRED);
                else failure = result;
                notifyChanged();
            }
        };
    }

    public boolean isLoading() { return loading; }
    public boolean isLoaded() { return loaded; }
    public FailureKind getFailure() { return failure; }
    public void clearFailure() { failure = null; }

    public void loadConversations(Runnable onChanged) {
        connection.request();
        loading = true;
        failure = null;
        onChanged.run();
        java.util.Map<Long, Long> revisions = new java.util.HashMap<>();
        for (HumanConversation item : allConversations()) revisions.put(item.participantId, item.revision);
        repository.load(new HumanChatRepository.ResultCallback() {
            @Override public void onSuccess(List<ChatConversationData> result) {
                List<HumanConversation> previous = allConversations();
                conversations.clear();
                // Mantém a ordem da API e usa a última mensagem apenas como prévia do card.
                for (ChatConversationData data : result) {
                    boolean manager = data.participant.role.startsWith("GESTOR");
                    HumanConversation conversation = previous.stream().filter(item -> data.participant.id.equals(item.participantId))
                            .findFirst().orElseGet(() -> new HumanConversation(data.participant.name,
                                    manager ? "Gestor" : "Colaborador", manager, 0));
                    conversation.conversationId = data.id;
                    conversation.participantId = data.participant.id;
                    conversation.profilePhotoUrl = data.participant.profilePhotoUrl;
                    conversation.avatarInitials = data.participant.avatarInitials;
                    // Um GET iniciado antes de um evento/leitura não pode desfazer o estado mais recente.
                    if (conversation.revision == 0 || java.util.Objects.equals(revisions.get(data.participant.id), conversation.revision)) {
                        conversation.lastMessageText = data.lastMessage.content;
                        conversation.lastMessageSent = !data.lastMessage.senderId.equals(data.participant.id);
                        conversation.unreadCount = data.unreadCount;
                    }
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

    public boolean isContactsLoading() { return contactsLoading; }
    public boolean isContactsLoaded() { return contactsLoaded; }
    public void loadContacts(Runnable onChanged) {
        connection.request();
        contactsLoading = true;
        failure = null;
        onChanged.run();
        contactsRepository.load(new HumanChatContactsRepository.ResultCallback() {
            @Override public void onSuccess(List<ChatContactData> result) {
                List<HumanConversation> previous = allConversations();
                contacts.clear();
                for (ChatContactData data : result) {
                    HumanConversation conversation = previous.stream().filter(item -> data.id.equals(item.participantId))
                            .findFirst().orElseGet(() -> new HumanConversation(data.name,
                                    data.role.startsWith("GESTOR") ? "Gestor" : "Colaborador", data.role.startsWith("GESTOR"), 0));
                    conversation.participantId = data.id;
                    contacts.add(conversation);
                }
                contactsLoading = false;
                contactsLoaded = true;
                onChanged.run();
            }
            @Override public void onFailure(FailureKind result) { contactsLoading = false; failure = result; onChanged.run(); }
        });
    }
    public void cancelContacts() { contactsRepository.cancel(); contactsLoading = false; }

    public interface HistoryCallback {
        void onChanged();
        void onFailure(FailureKind failure);
    }

    public void loadHistory(HumanConversation conversation, HistoryCallback callback) {
        if (conversation == null || conversation.conversationId == null || conversation.historyLoading) return;
        conversation.historyLoading = true;
        java.util.Set<String> knownIds = conversation.messages.stream().map(item -> item.id)
                .collect(java.util.stream.Collectors.toSet());
        historyRepository.loadConversation(conversation.conversationId, new HumanChatHistoryRepository.ResultCallback() {
            @Override public void onMarkedRead() {
                conversation.unreadCount = 0;
                conversation.revision++;
            }

            @Override public void onSuccess(ChatMessagePageData page) {
                // Recomeça pela página recente sem apagar eventos recebidos durante o GET.
                List<HumanMessage> live = conversation.messages.stream().filter(item -> !knownIds.contains(item.id))
                        .collect(java.util.stream.Collectors.toList());
                conversation.messages.clear();
                conversation.messages.addAll(live);
                for (ChatMessageData message : page.messages) mergeMessage(conversation, message);
                conversation.nextCursor = page.nextCursor;
                conversation.hasMore = page.hasMore;
                conversation.loaded = true;
                conversation.historyLoading = false;
                conversation.loadingOlder = false;
                callback.onChanged();
            }

            @Override public void onFailure(FailureKind failure) {
                conversation.historyLoading = false;
                callback.onFailure(failure);
            }
        });
    }

    public void loadOlder(HumanConversation conversation, HistoryCallback callback) {
        if (conversation == null || conversation.conversationId == null || conversation.loadingOlder
                || !conversation.hasMore || conversation.nextCursor == null) return;
        conversation.loadingOlder = true;
        historyRepository.loadOlder(conversation.conversationId, conversation.nextCursor,
                new HumanChatHistoryRepository.ResultCallback() {
                    @Override public void onMarkedRead() { }

                    @Override public void onSuccess(ChatMessagePageData page) {
                        for (ChatMessageData message : page.messages) mergeMessage(conversation, message);
                        conversation.nextCursor = page.nextCursor;
                        conversation.hasMore = page.hasMore;
                        conversation.loadingOlder = false;
                        callback.onChanged();
                    }

                    @Override public void onFailure(FailureKind failure) {
                        conversation.loadingOlder = false;
                        callback.onFailure(failure);
                    }
                });
    }

    public void cancelHistory(HumanConversation conversation) {
        historyRepository.cancel();
        if (conversation != null) {
            conversation.historyLoading = false;
            conversation.loadingOlder = false;
        }
    }

    private static HumanMessage toMessage(HumanConversation conversation, ChatMessageData data) {
        String time;
        try {
            time = OffsetDateTime.parse(data.createdAt).atZoneSameInstant(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));
        } catch (DateTimeParseException error) {
            time = data.createdAt;
        }
        boolean sent = conversation.participantId == null || !conversation.participantId.equals(data.senderId);
        HumanMessage result = new HumanMessage(data.content, time, sent);
        result.id = data.id;
        result.createdAt = data.createdAt;
        return result;
    }

    private static void mergeMessage(HumanConversation conversation, ChatMessageData message) {
        conversation.addMessage(toMessage(conversation, message));
    }

    public HumanConversation find(String id) {
        for (HumanConversation conversation : allConversations()) {
            if (id.equals(conversation.conversationId)) return conversation;
        }
        return null;
    }

    public List<HumanConversation> visible(boolean managerFlow, boolean contacts, String query) {
        // A busca local preserva a lista autorizada retornada pelo backend.
        List<HumanConversation> result = new ArrayList<>();
        for (HumanConversation conversation : contacts ? this.contacts : conversations) {
            if (contacts && !managerFlow && !conversation.manager) continue;
            if (matches(conversation.name, query)) result.add(conversation);
        }
        return result;
    }

    public HumanConversation open(Long participantId, String name, String role, boolean manager, int avatarIndex) {
        if (participantId == null || participantId <= 0) return null;
        for (HumanConversation conversation : allConversations()) {
            if (participantId.equals(conversation.participantId)) return conversation;
        }
        HumanConversation conversation = new HumanConversation(name, role, manager, avatarIndex);
        conversation.participantId = participantId;
        contacts.add(conversation);
        return conversation;
    }

    public boolean send(HumanConversation conversation) {
        if (connection.client().getState() != ChatConnectionState.CONNECTED) return false;
        if (conversation.pending != null && conversation.pending.awaiting) return false;
        if (conversation.pending == null) {
            if (conversation.draft.codePoints().allMatch(Character::isWhitespace)
                    || conversation.draft.length() > 4000 || conversation.participantId == null) return false;
            conversation.pending = new HumanPendingSend(conversation);
        }
        HumanPendingSend pending = conversation.pending;
        pending.awaiting = true;
        conversation.sendErrorCode = null;
        boolean queued = connection.client().send(pending.request);
        pending.awaiting = queued;
        cancelConfirmation(conversation);
        if (queued) {
            Runnable timeout = () -> {
                confirmations.remove(conversation);
                if (conversation.pending == pending) { pending.awaiting = false; notifyChanged(); }
            };
            confirmations.put(conversation, timeout);
            handler.postDelayed(timeout, 20000);
        }
        notifyChanged();
        return queued;
    }

    private void cancelConfirmation(HumanConversation conversation) {
        Runnable timeout = confirmations.remove(conversation);
        if (timeout != null) handler.removeCallbacks(timeout);
    }

    private void receive(ChatSendResponse event) {
        HumanConversation conversation = find(event.conversationId);
        if (conversation == null) {
            conversation = allConversations().stream().filter(item -> item.pending != null
                    && event.clientMessageId.equals(item.pending.request.clientMessageId)).findFirst().orElse(null);
        }
        if (conversation == null) {
            conversation = contacts.stream().filter(item -> event.senderId.equals(item.participantId)).findFirst().orElse(null);
        }
        if (conversation == null) { loadConversations(this::notifyChanged); return; }
        conversation.conversationId = event.conversationId;
        if (!conversations.contains(conversation)) conversations.add(0, conversation);
        boolean duplicate = conversation.messages.stream().anyMatch(item -> event.id.equals(item.id));
        mergeMessage(conversation, event.toMessage());
        if (conversation.pending != null && event.clientMessageId.equals(conversation.pending.request.clientMessageId)
                && !event.senderId.equals(conversation.participantId)) {
            cancelConfirmation(conversation);
            if (conversation.draft.equals(conversation.pending.request.content)) conversation.draft = "";
            conversation.pending = null;
            conversation.sendErrorCode = null;
        }
        HumanMessage last = conversation.messages.get(conversation.messages.size() - 1);
        conversation.lastMessageText = last.text;
        conversation.lastMessageSent = last.sent;
        if (!duplicate) conversation.revision++;
        if (!duplicate && last.id.equals(event.id)) {
            conversations.remove(conversation);
            conversations.add(0, conversation);
        }
        if (!duplicate && event.senderId.equals(conversation.participantId)) {
            conversation.unreadCount++;
            if (conversation == activeConversation) {
                HumanConversation current = conversation;
                readRepository.markAsRead(conversation.conversationId, new HumanChatHistoryRepository.ResultCallback() {
                    @Override public void onSuccess(ChatMessagePageData page) { }
                    @Override public void onMarkedRead() { current.unreadCount = 0; current.revision++; notifyChanged(); }
                    @Override public void onFailure(FailureKind result) {
                        if (result == FailureKind.SESSION) connectionState.setValue(ChatConnectionState.SESSION_EXPIRED);
                        notifyChanged();
                    }
                });
            }
        }
        notifyChanged();
    }

    public void clearSession() {
        // Descarta mensagens e rascunhos ao voltar à identificação, inclusive após logout.
        cancelLoad();
        historyRepository.cancel();
        readRepository.cancel();
        contactsRepository.cancel();
        for (Runnable timeout : confirmations.values()) handler.removeCallbacks(timeout);
        confirmations.clear();
        if (connection != null) connection.stop();
        activeConversation = null;
        for (HumanConversation conversation : allConversations()) {
            conversation.messages.clear();
            conversation.draft = "";
            conversation.pending = null;
            conversation.unreadCount = 0;
        }
        conversations.clear();
        contacts.clear();
        contactsLoaded = contactsLoading = false;
        loaded = false;
        failure = null;
        notifyChanged();
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
        historyRepository.cancel();
        readRepository.cancel();
        contactsRepository.cancel();
        for (Runnable timeout : confirmations.values()) handler.removeCallbacks(timeout);
        connection.close();
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final Context context;
        private Factory(Context context) { this.context = context.getApplicationContext(); }
        public static Factory createDefault(Context context) { return new Factory(context); }
        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type) {
            if (!type.isAssignableFrom(HumanConversationsViewModel.class)) throw new IllegalArgumentException("Unknown ViewModel");
            return type.cast(new HumanConversationsViewModel(context));
        }
    }
}
