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
    private static final long REQUEST_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15);

    public enum Destination { FIRST_ACCESS_KEY, PASSWORD, INLINE_ERROR, DISABLED, CONNECTION_ERROR,
        INTERNAL_ERROR }

    public interface ResultCallback {
        void onResult(Destination destination, String message, String userType);
    }

    private final EmailVerificationRepository repository;
    private Handler mainHandler;
    private Call<ApiResponse<VerifyEmailData>> currentCall;
    private Runnable timeoutRunnable;
    private int requestId;
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
        scheduleTimeout(id, callback);
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

    private void scheduleTimeout(int id, ResultCallback callback) {
        // Um callback síncrono já pode ter concluído a consulta.
        // Só inicializa o scheduler Android quando há uma requisição pendente.
        if (id != requestId || !loading) {
            return;
        }
        clearTimeout();
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        timeoutRunnable = () -> {
            if (id != requestId || !loading) {
                return;
            }
            Call<ApiResponse<VerifyEmailData>> timedOutCall = currentCall;
            currentCall = null;
            loading = false;
            ++requestId;
            timeoutRunnable = null;
            if (timedOutCall != null) {
                timedOutCall.cancel();
            }
            // Render pode demorar para iniciar, mas a tela não fica carregando sem limite.
            callback.onResult(Destination.INTERNAL_ERROR, null, null);
        };
        mainHandler.postDelayed(timeoutRunnable, REQUEST_TIMEOUT_MS);
    }

    private void clearTimeout() {
        if (timeoutRunnable != null) {
            mainHandler.removeCallbacks(timeoutRunnable);
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
