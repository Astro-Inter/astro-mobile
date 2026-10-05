package com.example.astro_mobile.auth;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.AccessKeyRepository;
import com.example.astro_mobile.data.api.AstroApiClient;
import com.example.astro_mobile.data.api.FailureKind;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;

// Guarda o contexto do primeiro acesso sem colocar a chave na próxima tela.
public class AccessKeyViewModel extends ViewModel {
    public interface ResultCallback {
        void onResult(FailureKind failure, String message);
    }

    private final AccessKeyRepository repository;
    private Call<Void> currentCall;
    private int requestId;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private boolean loading;
    private String email;
    private String userType;
    private String lastKey;
    private String pendingInlineError;

    public AccessKeyViewModel(AccessKeyRepository repository) {
        this.repository = repository;
    }

    public void setContext(String email, String userType) {
        // O perfil permanece no estado compartilhado; só o e-mail vai no Bundle seguinte.
        if (!Objects.equals(email, this.email)) {
            cancelCurrentRequest();
            lastKey = null;
            pendingInlineError = null;
        }
        this.email = email;
        this.userType = userType;
    }

    public String getEmail() {
        return email;
    }

    public String getUserType() {
        return userType;
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
        int id = ++requestId;
        currentCall = repository.verifyKey(getEmail(), accessKey,
                new AccessKeyRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        if (finishRequest(id)) {
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
        scheduleTimeout(callback);
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

    private void scheduleTimeout(ResultCallback callback) {
        if (!loading) {
            return;
        }
        if (timeoutHandler == null) {
            timeoutHandler = new Handler(Looper.getMainLooper());
        }
        // O cancelamento HTTP pode esperar o DNS; a interface não espera além de 15s.
        timeoutRunnable = () -> {
            cancelCurrentRequest();
            callback.onResult(FailureKind.INTERNAL, null);
        };
        timeoutHandler.postDelayed(timeoutRunnable,
                TimeUnit.SECONDS.toMillis(AstroApiClient.REQUEST_TIMEOUT_SECONDS));
    }

    private void clearTimeout() {
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
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

    public void clearForLogout() {
        // Não deixa dados do primeiro acesso disponíveis para a próxima conta.
        cancelCurrentRequest();
        email = null;
        userType = null;
        lastKey = null;
        pendingInlineError = null;
    }

    @Override
    protected void onCleared() {
        cancelCurrentRequest();
        lastKey = null;
    }

    public static class Factory implements ViewModelProvider.Factory {
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(AccessKeyViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new AccessKeyViewModel(
                    new AccessKeyRepository(AstroApiClient.getApi())));
        }
    }
}
