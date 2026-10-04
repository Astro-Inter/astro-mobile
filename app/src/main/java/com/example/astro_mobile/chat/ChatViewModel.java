package com.example.astro_mobile.chat;

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

        State(List<ChatMessage> messages, boolean loading, ChatFailure failure) {
            this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
            this.loading = loading;
            this.failure = failure;
        }
    }

    private final AiChatRepository repository;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private AiChatRepository.Operation currentOperation;
    private String sessionId;
    private String draft = "";
    private boolean loading;

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
        draft = "";
        loading = true;
        publish(null);
        currentOperation = repository.sendMessage(message, sessionId,
                new AiChatRepository.ResultCallback() {
                    @Override public void onSuccess(ChatResponse response) {
                        sessionId = response.getSessionId();
                        messages.add(new ChatMessage(response.getAnswer(), false));
                        loading = false;
                        currentOperation = null;
                        publish(null);
                    }

                    @Override public void onFailure(ChatFailure failure) {
                        loading = false;
                        currentOperation = null;
                        publish(failure);
                    }
                });
    }

    private void publish(ChatFailure failure) {
        state.setValue(new State(messages, loading, failure));
    }

    @Override protected void onCleared() {
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
