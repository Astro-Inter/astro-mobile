package com.example.astro_mobile.auth;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.CreationExtras;

import com.example.astro_mobile.data.api.AccessKeyRepository;
import com.example.astro_mobile.data.api.AstroApiClient;
import com.example.astro_mobile.data.api.FailureKind;

import java.util.concurrent.TimeUnit;

import retrofit2.Call;

// Guarda o contexto do primeiro acesso sem colocar a chave na próxima tela.
public class AccessKeyViewModel extends ViewModel {
    public interface ResultCallback {
        void onResult(FailureKind failure, String message);
    }

    private static final long REQUEST_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15);
    private static final String EMAIL = "access_key_email";
    private static final String USER_TYPE = "access_key_user_type";
    private static final String VERIFIED = "access_key_verified";

    private final AccessKeyRepository repository;
    private final SavedStateHandle state;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Call<Void> currentCall;
    private Runnable timeoutRunnable;
    private int requestId;
    private boolean loading;
    private String lastKey;
    private String pendingInlineError;

    public AccessKeyViewModel(AccessKeyRepository repository, SavedStateHandle state) {
        this.repository = repository;
        this.state = state;
    }

    public void setContext(String email, String userType) {
        // O perfil permanece no estado compartilhado; só o e-mail vai no Bundle seguinte.
        String previousEmail = getEmail();
        if (email == null || !email.equals(previousEmail)) {
            cancelCurrentRequest();
            lastKey = null;
            pendingInlineError = null;
            state.set(VERIFIED, false);
        }
        state.set(EMAIL, email);
        state.set(USER_TYPE, userType);
    }

    public String getEmail() {
        return state.get(EMAIL);
    }

    public String getVerifiedUserType(String email) {
        return email != null && email.equals(getEmail())
                && Boolean.TRUE.equals(state.get(VERIFIED)) ? state.get(USER_TYPE) : null;
    }

    public String getLastKey() {
        return lastKey;
    }

    public boolean isLoading() {
        return loading;
    }

    public void verifyKey(String accessKey, ResultCallback callback) {
        if (loading) {
            return;
        }
        lastKey = accessKey;
        loading = true;
        state.set(VERIFIED, false);
        int id = ++requestId;
        currentCall = repository.verifyKey(getEmail(), accessKey,
                new AccessKeyRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        if (finishRequest(id)) {
                            state.set(VERIFIED, true);
                            lastKey = null;
                            callback.onResult(null, null);
                        }
                    }

                    @Override
                    public void onFailure(FailureKind kind, String message) {
                        if (finishRequest(id)) {
                            callback.onResult(kind, message);
                        }
                    }
                });
        // Cancela o POST após 15 segundos, sem deixar o botão travado.
        timeoutRunnable = () -> {
            if (id == requestId && loading) {
                cancelCurrentRequest();
                callback.onResult(FailureKind.INTERNAL, null);
            }
        };
        mainHandler.postDelayed(timeoutRunnable, REQUEST_TIMEOUT_MS);
    }

    private boolean finishRequest(int id) {
        if (id != requestId) {
            return false;
        }
        loading = false;
        currentCall = null;
        clearTimeout();
        return true;
    }

    public void setPendingInlineError(String message) {
        pendingInlineError = message;
    }

    public String consumeInlineError() {
        String message = pendingInlineError;
        pendingInlineError = null;
        return message;
    }

    public void cancelCurrentRequest() {
        // Respostas de uma tela fechada não podem disparar navegação.
        ++requestId;
        clearTimeout();
        if (currentCall != null) {
            currentCall.cancel();
            currentCall = null;
        }
        loading = false;
    }

    private void clearTimeout() {
        if (timeoutRunnable != null) {
            mainHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    @Override
    protected void onCleared() {
        cancelCurrentRequest();
        lastKey = null;
    }

    public static class Factory implements ViewModelProvider.Factory {
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass,
                                            @NonNull CreationExtras extras) {
            if (!modelClass.isAssignableFrom(AccessKeyViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new AccessKeyViewModel(
                    new AccessKeyRepository(AstroApiClient.getApi()),
                    SavedStateHandleSupport.createSavedStateHandle(extras)));
        }
    }
}
