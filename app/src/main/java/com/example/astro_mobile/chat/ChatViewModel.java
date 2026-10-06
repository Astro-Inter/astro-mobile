package com.example.astro_mobile.chat;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.ai.AiChatRepository;
import com.example.astro_mobile.data.ai.ChatFailure;
import com.example.astro_mobile.data.ai.ChatRequest;
import com.example.astro_mobile.data.ai.ChatResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mantém a conversa enquanto a Home está na pilha, inclusive ao recriar a tela. */
public final class ChatViewModel extends ViewModel {
    public static final class State {
        public final List<ChatMessage> messages;
        public final boolean loading;
        public final ChatFailure failure;
        public final boolean waitingLong;

        State(List<ChatMessage> messages, boolean loading, ChatFailure failure, boolean waitingLong) {
            this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
            this.loading = loading;
            this.failure = failure;
            this.waitingLong = waitingLong;
        }

        public boolean canRetry() {
            return failure != null && failure != ChatFailure.SESSION
                    && failure != ChatFailure.FORBIDDEN && failure != ChatFailure.INVALID_MESSAGE;
        }
    }

    private final AiChatRepository repository;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private AiChatRepository.Operation currentOperation;
    private String sessionId;
    private String draft = "";
    private boolean loading;
    private boolean waitingLong;
    private int requestId;
    private int pendingMessageIndex = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable slowResponse;
    private Runnable deadline;

    public ChatViewModel(AiChatRepository repository) {
        this.repository = repository;
        publish(null);
    }

    public LiveData<State> getState() { return state; }
    public String getDraft() { return draft; }
    public void setDraft(String draft) { this.draft = draft; }

    public void send(String text) {
        if (loading) return;
        String message = text == null ? "" : text.trim();
        if (message.isEmpty()) return;
        if (message.codePointCount(0, message.length()) > ChatRequest.MAX_MESSAGE_LENGTH) {
            publish(ChatFailure.INVALID_MESSAGE);
            return;
        }
        messages.add(new ChatMessage(message, true));
        pendingMessageIndex = messages.size() - 1;
        draft = "";
        request(message);
    }

    public void retry() {
        State current = state.getValue();
        if (loading || current == null || !current.canRetry() || pendingMessageIndex < 0) return;
        ChatMessage failed = messages.get(pendingMessageIndex);
        messages.set(pendingMessageIndex, failed.withFailure(false));
        request(failed.getText());
    }

    private void request(String message) {
        int id = ++requestId;
        loading = true;
        waitingLong = false;
        publish(null);
        slowResponse = () -> {
            if (id != requestId || !loading) return;
            waitingLong = true;
            publish(null);
        };
        deadline = () -> {
            if (id != requestId || !loading) return;
            ++requestId;
            if (currentOperation != null) currentOperation.cancel();
            finishFailure(ChatFailure.TIMEOUT);
        };
        handler.postDelayed(slowResponse, 15_000L);
        // Inclui o tempo de obtenção/renovação do token, além da chamada HTTP.
        handler.postDelayed(deadline, 150_000L);
        AiChatRepository.Operation operation = repository.sendMessage(message, sessionId,
                new AiChatRepository.ResultCallback() {
                    @Override public void onSuccess(ChatResponse response) {
                        if (id != requestId) return;
                        clearTimers();
                        sessionId = response.getSessionId();
                        messages.add(new ChatMessage(response.getAnswer(), false));
                        loading = false;
                        currentOperation = null;
                        pendingMessageIndex = -1;
                        waitingLong = false;
                        publish(null);
                    }

                    @Override public void onFailure(ChatFailure failure) {
                        if (id == requestId) finishFailure(failure);
                    }
                });
        // Falhas de validação ou de sessão podem chegar de forma síncrona.
        if (loading) currentOperation = operation;
    }

    private void finishFailure(ChatFailure failure) {
        clearTimers();
        loading = false;
        waitingLong = false;
        currentOperation = null;
        if (pendingMessageIndex >= 0) {
            ChatMessage pending = messages.get(pendingMessageIndex);
            messages.set(pendingMessageIndex, pending.withFailure(true));
        }
        publish(failure);
    }

    private void clearTimers() {
        if (slowResponse != null) handler.removeCallbacks(slowResponse);
        if (deadline != null) handler.removeCallbacks(deadline);
        slowResponse = null;
        deadline = null;
    }

    private void publish(ChatFailure failure) {
        state.setValue(new State(messages, loading, failure, waitingLong));
    }

    @Override protected void onCleared() {
        ++requestId;
        clearTimers();
        if (currentOperation != null) currentOperation.cancel();
        messages.clear();
        draft = "";
        sessionId = null;
    }

    public static final class Factory implements ViewModelProvider.Factory {
        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(ChatViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new ChatViewModel(AiChatRepository.createDefault()));
        }
    }
}
