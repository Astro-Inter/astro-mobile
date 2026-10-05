package com.example.astro_mobile.auth;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.EmailVerificationRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.AstroApiClient;
import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.VerifyEmailData;

import java.util.concurrent.TimeUnit;

import retrofit2.Call;

public class EmailVerificationViewModel extends ViewModel {
    public enum Destination { FIRST_ACCESS_KEY, PASSWORD, INLINE_ERROR, DISABLED, CONNECTION_ERROR,
        INTERNAL_ERROR }

    public interface ResultCallback {
        void onResult(Destination destination, String message, String userType);
    }

    private final EmailVerificationRepository repository;
    private Call<ApiResponse<VerifyEmailData>> currentCall;
    private int requestId;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private boolean loading;
    private String lastEmail;
    private String pendingInlineError;

    public EmailVerificationViewModel(EmailVerificationRepository repository) {
        this.repository = repository;
    }

    public boolean isLoading() {
        return loading;
    }

    public String getLastEmail() {
        return lastEmail;
    }

    public String consumeInlineError() {
        // Entrega o erro uma vez ao voltar para o campo de e-mail.
        String error = pendingInlineError;
        pendingInlineError = null;
        return error;
    }

    public void verifyEmail(String email, ResultCallback callback) {
        // Evita consultas simultâneas e guarda o e-mail para uma possível tentativa posterior.
        if (loading) {
            return;
        }
        lastEmail = email;
        loading = true;
        int id = ++requestId;
        currentCall = repository.verifyEmail(email, new EmailVerificationRepository.ResultCallback() {
            @Override
            public void onSuccess(VerifyEmailData data) {
                // Ignora respostas antigas e escolhe a próxima tela pelo estado da conta.
                if (!finishRequest(id)) {
                    return;
                }
                String userType = data.getUserType();
                String status = data.getUserStatus();
                if ("DESATIVADO".equals(status)) {
                    callback.onResult(Destination.DISABLED, null, null);
                } else if (!MockSession.isKnownUserType(userType)) {
                    callback.onResult(Destination.INTERNAL_ERROR, null, null);
                } else if ("PRE_CADASTRADO".equals(status)) {
                    callback.onResult(Destination.FIRST_ACCESS_KEY, null, userType);
                } else if ("ATIVO".equals(status)) {
                    callback.onResult(Destination.PASSWORD, null, userType);
                } else {
                    callback.onResult(Destination.INTERNAL_ERROR, null, null);
                }
            }

            @Override
            public void onFailure(FailureKind kind, String message) {
                // Decide se a falha aparece no campo ou em uma tela de erro.
                if (!finishRequest(id)) {
                    return;
                }
                if (kind == FailureKind.CONNECTION) {
                    callback.onResult(Destination.CONNECTION_ERROR, null, null);
                } else if (kind == FailureKind.INTERNAL) {
                    callback.onResult(Destination.INTERNAL_ERROR, null, null);
                } else {
                    callback.onResult(Destination.INLINE_ERROR, message, null);
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
        // Uma resposta síncrona já pode ter concluído a consulta.
        if (!loading) {
            return;
        }
        if (timeoutHandler == null) {
            timeoutHandler = new Handler(Looper.getMainLooper());
        }
        // O cancelamento HTTP pode esperar o DNS; a interface não espera além de 15s.
        timeoutRunnable = () -> {
            cancelCurrentRequest();
            callback.onResult(Destination.INTERNAL_ERROR, null, null);
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

    public void clearForLogout() {
        // Apaga os dados temporários da identificação ao sair da conta.
        cancelCurrentRequest();
        lastEmail = null;
        pendingInlineError = null;
    }

    public void cancelCurrentRequest() {
        // Invalida a resposta pendente para ela não alterar uma tela já fechada.
        ++requestId;
        clearTimeout();
        if (currentCall != null) {
            currentCall.cancel();
            currentCall = null;
        }
        loading = false;
    }

    @Override
    protected void onCleared() {
        cancelCurrentRequest();
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final EmailVerificationRepository repository;

        public Factory(EmailVerificationRepository repository) {
            this.repository = repository;
        }

        public static Factory createDefault() {
            return new Factory(new EmailVerificationRepository(AstroApiClient.getApi()));
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(EmailVerificationViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new EmailVerificationViewModel(repository));
        }
    }
}
