package com.example.astro_mobile.chat;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.astro_mobile.data.ai.*;
import com.google.firebase.auth.FirebaseAuth;
import java.time.Instant;
import java.util.*;

/** Estado em memória isolado por conta; histórico persistido vem sempre da API. */
public final class ChatViewModel extends ViewModel {
    public enum Action { NONE, HISTORY, RESUME }

    public static final class State {
        public final List<ChatMessage> messages;
        public final boolean loading, waitingLong, unavailable, pending;
        public final ChatFailure failure;
        public final String sessionId, status;
        public final Action action;
        State(List<ChatMessage> messages, boolean loading, ChatFailure failure, boolean waitingLong,
              String sessionId, String status, Action action, boolean unavailable, boolean pending) {
            this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
            this.loading = loading; this.failure = failure; this.waitingLong = waitingLong;
            this.sessionId = sessionId; this.status = status; this.action = action; this.unavailable = unavailable;
            this.pending = pending;
        }
        public boolean canRetry() {
            return failure != null && failure != ChatFailure.SESSION && failure != ChatFailure.FORBIDDEN
                    && failure != ChatFailure.INVALID_MESSAGE && failure != ChatFailure.NOT_FOUND
                    && failure != ChatFailure.CONFLICT;
        }
        public boolean canSend() { return !loading && !unavailable && !pending && failure != ChatFailure.CONFLICT
                && failure != ChatFailure.SESSION && failure != ChatFailure.FORBIDDEN
                && ("ativa".equals(status) || "encerrada".equals(status)); }
    }

    public static final class Conversation {
        public final String id, title, preview, status;
        public final long updatedAt;
        public final boolean active, responding;
        Conversation(String id, String title, String preview, long updatedAt, String status, boolean active, boolean responding) {
            this.id = id; this.title = title; this.preview = preview; this.updatedAt = updatedAt;
            this.status = status; this.active = active; this.responding = responding;
        }
    }

    public static final class ListState {
        public final boolean loading, loadingMore, hasMore;
        public final ChatFailure failure;
        ListState(boolean loading, boolean loadingMore, boolean hasMore, ChatFailure failure) {
            this.loading = loading; this.loadingMore = loadingMore; this.hasMore = hasMore; this.failure = failure;
        }
    }

    public static final class OpenState {
        public final String id;
        public final boolean loading, opened;
        public final ChatFailure failure;
        OpenState(String id, boolean loading, boolean opened, ChatFailure failure) {
            this.id = id; this.loading = loading; this.opened = opened; this.failure = failure;
        }
    }

    private static final class SavedConversation {
        List<ChatMessage> messages;
        String sessionId, draft, status;
        ChatFailure failure;
        int pendingIndex;
    }

    private final AiChatRepository repository;
    private final AiSessionsRepository sessions;
    private final FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authListener;
    private String owner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<List<Conversation>> conversationList = new MutableLiveData<>();
    private final MutableLiveData<ListState> listState = new MutableLiveData<>();
    private final MutableLiveData<OpenState> openState = new MutableLiveData<>();
    private final List<ChatMessage> messages = new ArrayList<>();
    private final Map<String, SavedConversation> drafts = new LinkedHashMap<>();
    private final Map<String, SessionSummary> remote = new LinkedHashMap<>();
    private final Set<String> consumedCursors = new HashSet<>();
    private String conversationId = UUID.randomUUID().toString(), sessionId, draft = "", status = "ativa", nextCursor;
    private boolean sending, waitingLong, unavailable, listing, paging, retryChecked;
    private Action action = Action.NONE;
    private int requestId, listRequestId, sessionRequestId, pendingMessageIndex = -1;
    private ChatFailure failure, listFailure;
    private AiChatRepository.Operation currentOperation;
    private AiSessionsRepository.Operation listOperation, sessionOperation;
    private Runnable slowResponse, deadline, listDeadline, sessionDeadline;

    public ChatViewModel(AiChatRepository repository) { this(repository, null, null); }
    public ChatViewModel(AiChatRepository repository, AiSessionsRepository sessions, FirebaseAuth auth) {
        this.repository = repository; this.sessions = sessions; this.auth = auth;
        owner = uid();
        openState.setValue(new OpenState(null, false, false, null));
        publish(null);
        if (auth != null) {
            authListener = ignored -> checkAccount();
            auth.addAuthStateListener(authListener);
        }
    }

    public LiveData<State> getState() { return state; }
    public LiveData<List<Conversation>> getConversations() { return conversationList; }
    public LiveData<ListState> getListState() { return listState; }
    public LiveData<OpenState> getOpenState() { return openState; }
    public String getDraft() { return draft; }
    public void setDraft(String draft) { this.draft = draft; }
    public boolean isRetryChecked() { return retryChecked; }
    public boolean hasPendingMessage() { return pendingMessageIndex >= 0; }
    public boolean isBusy() { return sending || action != Action.NONE || openState.getValue().loading; }

    private String uid() { return auth == null || auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid(); }
    private boolean checkAccount() {
        if (auth == null || Objects.equals(owner, uid())) return true;
        owner = uid();
        reset();
        publish(owner == null ? ChatFailure.SESSION : null);
        return false;
    }

    public void refreshConversations() {
        checkAccount();
        saveDraft();
        if (sessions == null) { publishLists(); return; }
        if (listOperation != null) listOperation.cancel();
        nextCursor = null;
        loadPage(null);
    }

    public void loadMore() {
        if (!listing && !paging && listFailure == null && nextCursor != null && sessions != null) loadPage(nextCursor);
    }

    public void retryConversations() {
        if (listing || paging || sessions == null) return;
        if (nextCursor == null || listFailure == ChatFailure.INVALID_RESPONSE || listFailure == ChatFailure.INVALID_CURSOR) {
            refreshConversations();
        } else loadPage(nextCursor);
    }

    private void loadPage(String cursor) {
        if (!checkAccount()) return;
        int id = ++listRequestId;
        listing = cursor == null; paging = cursor != null; listFailure = null;
        if (listDeadline != null) handler.removeCallbacks(listDeadline);
        listDeadline = () -> {
            if (id != listRequestId || (!listing && !paging)) return;
            ++listRequestId;
            if (listOperation != null) listOperation.cancel();
            finishList(ChatFailure.TIMEOUT);
        };
        handler.postDelayed(listDeadline, 150_000);
        publishLists();
        AiSessionsRepository.Operation operation = sessions.list(cursor, new AiSessionsRepository.Callback<SessionsPage>() {
            @Override public void onSuccess(SessionsPage page) {
                if (id != listRequestId || !checkAccount()) return;
                if (cursor != null && (cursor.equals(page.nextCursor) || consumedCursors.contains(page.nextCursor))) {
                    finishList(ChatFailure.INVALID_RESPONSE); return;
                }
                if (cursor == null) { remote.clear(); consumedCursors.clear(); }
                else consumedCursors.add(cursor);
                for (SessionSummary summary : page.sessions) remote.put(summary.sessionId, summary);
                nextCursor = page.nextCursor;
                finishList(null);
            }
            @Override public void onFailure(ChatFailure error) {
                if (id != listRequestId || !checkAccount()) return;
                finishList(error);
                if (error == ChatFailure.INVALID_CURSOR && cursor != null) refreshConversations();
            }
        });
        if (listing || paging) listOperation = operation;
    }

    private void finishList(ChatFailure error) {
        if (listDeadline != null) handler.removeCallbacks(listDeadline);
        listOperation = null; listing = false; paging = false; listFailure = error;
        publishLists();
    }

    public boolean startNewConversation() {
        if (!checkAccount() || isBusy()) return false;
        saveDraft();
        if (sessionId == null && messages.isEmpty() && draft.trim().isEmpty()) return true;
        conversationId = UUID.randomUUID().toString(); sessionId = null; draft = ""; status = "ativa";
        messages.clear(); pendingMessageIndex = -1; unavailable = false; retryChecked = false;
        publish(null);
        return true;
    }

    public boolean openConversation(String id) {
        if (!checkAccount() || isBusy()) return false;
        SavedConversation saved = drafts.get(id);
        if (sessions != null && (remote.containsKey(id) || (saved != null && saved.sessionId != null))) {
            saveDraft();
            loadHistory(id, true);
            return true;
        }
        if (saved == null && !conversationId.equals(id)) return false;
        if (saved != null) { saveDraft(); restoreDraft(id, saved); publish(saved.failure); }
        openState.setValue(new OpenState(id, false, true, null));
        return true;
    }

    public void consumeOpened() { openState.setValue(new OpenState(null, false, false, null)); }
    public void cancelOpening() {
        if (!openState.getValue().loading) return;
        ++sessionRequestId;
        if (sessionOperation != null) sessionOperation.cancel();
        clearSessionTimer();
        openState.setValue(new OpenState(null, false, false, null));
        publish(failure);
    }

    private void restoreDraft(String id, SavedConversation saved) {
        conversationId = id; sessionId = saved.sessionId; draft = saved.draft; status = saved.status;
        messages.clear(); messages.addAll(saved.messages); pendingMessageIndex = saved.pendingIndex;
        unavailable = false; retryChecked = false;
    }

    public void refreshCurrentHistory() {
        if (!checkAccount()) return;
        if (sessionId != null && sessions != null && !isBusy() && !unavailable) loadHistory(sessionId, false);
    }

    private void loadHistory(String id, boolean opening) {
        int request = ++sessionRequestId;
        if (opening) openState.setValue(new OpenState(id, true, false, null));
        else action = Action.HISTORY;
        publish(failure);
        scheduleSessionDeadline(request, () -> historyFailed(id, opening, ChatFailure.TIMEOUT));
        AiSessionsRepository.Operation operation = sessions.history(id, new AiSessionsRepository.Callback<SessionHistory>() {
            @Override public void onSuccess(SessionHistory history) {
                if (request != sessionRequestId || !checkAccount()) return;
                clearSessionTimer();
                SavedConversation saved = drafts.get(id);
                int oldPendingIndex = opening ? (saved == null ? -1 : saved.pendingIndex) : pendingMessageIndex;
                List<ChatMessage> previous = opening ? (saved == null ? Collections.emptyList() : saved.messages) : messages;
                ChatMessage unsent = oldPendingIndex >= 0 && oldPendingIndex < previous.size() ? previous.get(oldPendingIndex) : null;
                ChatFailure oldFailure = opening ? (saved == null ? null : saved.failure) : failure;
                if (opening) {
                    conversationId = id; sessionId = id; draft = saved == null ? "" : saved.draft;
                }
                messages.clear();
                for (SessionHistory.Message message : history.messages) {
                    messages.add(ChatMessage.fromHistory(message.content, "user".equals(message.role)));
                }
                pendingMessageIndex = -1;
                // Não reenvia: só recupera a pergunta que ainda não apareceu no histórico.
                boolean persisted = unsent != null && oldPendingIndex < messages.size()
                        && messages.get(oldPendingIndex).isFromUser()
                        && messages.get(oldPendingIndex).getText().equals(unsent.getText());
                if (unsent != null && !persisted) {
                    if (oldFailure == ChatFailure.CONFLICT) {
                        if (draft.trim().isEmpty()) draft = unsent.getText();
                        oldFailure = null;
                    } else {
                        pendingMessageIndex = messages.size(); messages.add(unsent.withFailure(true));
                    }
                }
                status = history.status; unavailable = false; action = Action.NONE; retryChecked = unsent != null;
                if (opening) openState.setValue(new OpenState(id, false, false, null));
                publish(unsent != null && !persisted ? oldFailure : null);
                if (opening) openState.setValue(new OpenState(id, false, true, null));
            }
            @Override public void onFailure(ChatFailure error) {
                if (request == sessionRequestId && checkAccount()) historyFailed(id, opening, error);
            }
        });
        if (action != Action.NONE || openState.getValue().loading) sessionOperation = operation;
    }

    private void historyFailed(String id, boolean opening, ChatFailure error) {
        clearSessionTimer(); action = Action.NONE;
        if (opening) openState.setValue(new OpenState(id, false, false, error));
        if (error == ChatFailure.NOT_FOUND) {
            remote.remove(id); drafts.remove(id);
            if (id.equals(sessionId)) unavailable = true;
            refreshConversations();
        }
        publish(opening ? failure : error);
    }

    /** Compatibilidade com sessões antigas: retoma somente antes do envio solicitado. */
    private void resumeBeforeSending(String message) {
        if (sessions == null || sessionId == null) { finishFailure(ChatFailure.INVALID_RESPONSE); return; }
        String id = sessionId;
        int request = ++sessionRequestId;
        action = Action.RESUME;
        publish(null);
        scheduleSessionDeadline(request, () -> finishSessionFailure(ChatFailure.TIMEOUT));
        AiSessionsRepository.Callback<SessionResult> callback = new AiSessionsRepository.Callback<SessionResult>() {
            @Override public void onSuccess(SessionResult result) {
                if (request != sessionRequestId || !checkAccount()) return;
                clearSessionTimer(); action = Action.NONE; status = result.status;
                // Mantém UUID, histórico, mensagem pendente e qualquer próximo rascunho.
                request(message);
            }
            @Override public void onFailure(ChatFailure error) {
                if (request == sessionRequestId && checkAccount()) finishSessionFailure(error);
            }
        };
        AiSessionsRepository.Operation operation = sessions.resume(id, callback);
        if (action == Action.RESUME) sessionOperation = operation;
    }

    private void finishSessionFailure(ChatFailure error) {
        clearSessionTimer(); action = Action.NONE;
        if (pendingMessageIndex >= 0) messages.set(pendingMessageIndex, messages.get(pendingMessageIndex).withFailure(true));
        if (error == ChatFailure.NOT_FOUND) { unavailable = true; refreshConversations(); }
        publish(error);
        // Não envia após falha de retomada; 409 exige consultar o histórico novamente.
    }

    private void scheduleSessionDeadline(int id, Runnable fail) {
        if (sessionDeadline != null) handler.removeCallbacks(sessionDeadline);
        sessionDeadline = () -> {
            if (id != sessionRequestId) return;
            ++sessionRequestId;
            if (sessionOperation != null) sessionOperation.cancel();
            fail.run();
        };
        handler.postDelayed(sessionDeadline, 150_000);
    }
    private void clearSessionTimer() {
        if (sessionDeadline != null) handler.removeCallbacks(sessionDeadline);
        sessionDeadline = null; sessionOperation = null;
    }

    public void send(String text) {
        if (!checkAccount() || !state.getValue().canSend()) return;
        String message = text == null ? "" : text.trim();
        if (message.isEmpty()) return;
        if (message.codePointCount(0, message.length()) > ChatRequest.MAX_MESSAGE_LENGTH) {
            publish(ChatFailure.INVALID_MESSAGE); return;
        }
        messages.add(new ChatMessage(message, true)); pendingMessageIndex = messages.size() - 1;
        draft = ""; retryChecked = false;
        sendOrResume(message);
    }

    public void retry() {
        if (!checkAccount() || isBusy() || !state.getValue().canRetry() || pendingMessageIndex < 0
                || unavailable || !("ativa".equals(status) || "encerrada".equals(status))) return;
        if (sessions != null && sessionId != null && !retryChecked) { refreshCurrentHistory(); return; }
        ChatMessage pending = messages.get(pendingMessageIndex);
        messages.set(pendingMessageIndex, pending.withFailure(false));
        sendOrResume(pending.getText());
    }

    private void sendOrResume(String message) {
        if ("encerrada".equals(status)) resumeBeforeSending(message);
        else request(message);
    }

    private void request(String message) {
        int id = ++requestId;
        sending = true; waitingLong = false; publish(null);
        slowResponse = () -> { if (id == requestId && sending) { waitingLong = true; publish(null); } };
        deadline = () -> {
            if (id != requestId || !sending) return;
            ++requestId; if (currentOperation != null) currentOperation.cancel(); finishFailure(ChatFailure.TIMEOUT);
        };
        handler.postDelayed(slowResponse, 15_000); handler.postDelayed(deadline, 150_000);
        AiChatRepository.Operation operation = repository.sendMessage(message, sessionId, new AiChatRepository.ResultCallback() {
            @Override public void onSuccess(ChatResponse response) {
                if (id != requestId || !checkAccount()) return;
                if (sessionId != null && !sessionId.equals(response.getSessionId())) {
                    finishFailure(ChatFailure.INVALID_RESPONSE); return;
                }
                clearTimers();
                String previousId = conversationId;
                sessionId = response.getSessionId(); conversationId = sessionId; drafts.remove(previousId);
                messages.add(new ChatMessage(response.getAnswer(), false));
                sending = false; currentOperation = null; pendingMessageIndex = -1; waitingLong = false;
                status = "ativa"; retryChecked = false; publish(null); refreshConversations();
            }
            @Override public void onFailure(ChatFailure error) {
                if (id == requestId && checkAccount()) finishFailure(error);
            }
        });
        if (sending) currentOperation = operation;
    }

    private void finishFailure(ChatFailure error) {
        clearTimers(); sending = false; waitingLong = false; currentOperation = null; retryChecked = false;
        if (pendingMessageIndex >= 0) messages.set(pendingMessageIndex, messages.get(pendingMessageIndex).withFailure(true));
        if (error == ChatFailure.NOT_FOUND) unavailable = true;
        publish(error);
        if (sessionId == null || error == ChatFailure.NOT_FOUND) refreshConversations();
    }

    private void clearTimers() {
        if (slowResponse != null) handler.removeCallbacks(slowResponse);
        if (deadline != null) handler.removeCallbacks(deadline);
        slowResponse = null; deadline = null;
    }

    private void publish(ChatFailure error) {
        failure = error;
        state.setValue(new State(messages, isBusy(), error, waitingLong, sessionId, status, action, unavailable, pendingMessageIndex >= 0));
        saveDraft(); publishLists();
    }

    private void saveDraft() {
        if (messages.isEmpty() && draft.trim().isEmpty()) { drafts.remove(conversationId); return; }
        SavedConversation saved = new SavedConversation();
        saved.messages = new ArrayList<>(messages); saved.sessionId = sessionId; saved.draft = draft;
        saved.status = status; saved.failure = failure; saved.pendingIndex = pendingMessageIndex;
        drafts.put(conversationId, saved);
    }

    private void publishLists() {
        List<Conversation> list = new ArrayList<>();
        // Só rascunhos/novas conversas locais: os itens remotos preservam a ordem da API.
        for (Map.Entry<String, SavedConversation> entry : drafts.entrySet()) {
            if (remote.containsKey(entry.getKey())) continue;
            SavedConversation saved = entry.getValue();
            if (saved.sessionId != null) continue;
            String title = saved.messages.isEmpty() ? "" : saved.messages.get(0).getText();
            String preview = saved.messages.isEmpty() ? saved.draft : saved.messages.get(saved.messages.size() - 1).getText();
            long updated = saved.messages.isEmpty() ? 0 : saved.messages.get(saved.messages.size() - 1).getCreatedAt();
            list.add(new Conversation(entry.getKey(), title, preview, updated, saved.status,
                    entry.getKey().equals(conversationId), entry.getKey().equals(conversationId) && sending));
        }
        for (SessionSummary summary : remote.values()) list.add(new Conversation(summary.sessionId, summary.title,
                summary.preview, Instant.parse(summary.updatedAt).toEpochMilli(), summary.status,
                summary.sessionId.equals(sessionId), summary.sessionId.equals(sessionId) && sending));
        conversationList.setValue(Collections.unmodifiableList(list));
        listState.setValue(new ListState(listing, paging, nextCursor != null, listFailure));
    }

    private void reset() {
        ++requestId; ++listRequestId; ++sessionRequestId;
        if (sessionOperation != null) sessionOperation.cancel();
        clearTimers(); clearSessionTimer();
        if (listDeadline != null) handler.removeCallbacks(listDeadline);
        if (currentOperation != null) currentOperation.cancel();
        if (listOperation != null) listOperation.cancel();
        currentOperation = null; listOperation = null;
        messages.clear(); drafts.clear(); remote.clear(); consumedCursors.clear(); sessionId = null; draft = ""; nextCursor = null;
        conversationId = UUID.randomUUID().toString(); status = "ativa"; action = Action.NONE;
        sending = false; listing = false; paging = false; waitingLong = false; unavailable = false;
        failure = null; listFailure = null; pendingMessageIndex = -1; retryChecked = false;
        openState.setValue(new OpenState(null, false, false, null));
    }

    @Override protected void onCleared() {
        if (auth != null && authListener != null) auth.removeAuthStateListener(authListener);
        if (sessionOperation != null) sessionOperation.cancel();
        reset(); publish(null);
    }

    public static final class Factory implements ViewModelProvider.Factory {
        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> cls) {
            if (!cls.isAssignableFrom(ChatViewModel.class)) throw new IllegalArgumentException("Unknown ViewModel: " + cls.getName());
            return cls.cast(new ChatViewModel(AiChatRepository.createDefault(), AiSessionsRepository.createDefault(), FirebaseAuth.getInstance()));
        }
    }
}
