package com.example.astro_mobile.shared.notifications;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.HumanChatRepository;
import com.example.astro_mobile.data.api.dto.ChatConversationData;
import java.util.List;

public final class ChatPushNavigationViewModel extends ViewModel {
    public interface Callback {
        void onResult(ChatConversationData conversation);
        void onFailure();
    }
    private final HumanChatRepository repository;

    public ChatPushNavigationViewModel(HumanChatRepository repository) { this.repository = repository; }

    public void load(String conversationId, Callback callback) {
        repository.load(new HumanChatRepository.ResultCallback() {
            @Override public void onSuccess(List<ChatConversationData> result) {
                callback.onResult(result.stream().filter(item -> conversationId.equals(item.id)).findFirst().orElse(null));
            }
            @Override public void onFailure(FailureKind failure) { callback.onFailure(); }
        });
    }
    public void cancel() { repository.cancel(); }
    @Override protected void onCleared() { cancel(); }

    public static final class Factory implements ViewModelProvider.Factory {
        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(ChatPushNavigationViewModel.class)) throw new IllegalArgumentException();
            return modelClass.cast(new ChatPushNavigationViewModel(HumanChatRepository.createDefault()));
        }
    }
}
