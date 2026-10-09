package com.example.astro_mobile.auth;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.ActivationRepository;
import com.example.astro_mobile.data.api.AstroApiClient;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.example.astro_mobile.data.firebase.FirebaseAuthRepository;

import java.util.concurrent.TimeUnit;

import retrofit2.Call;

public class FirstLoginPasswordViewModel extends ViewModel {
    public interface ResultCallback {
        void onSuccess();

        void onAuthFailure(AuthFailureKind kind);

        void onActivationFailure(FailureKind kind);
    }

    private final FirebaseAuthRepository authRepository;
    private final ActivationRepository activationRepository;
    private Call<Void> activationCall;
    private ResultCallback callback;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private boolean loading;
    private int requestId;
    private String email;

    public FirstLoginPasswordViewModel(FirebaseAuthRepository authRepository,
                                      ActivationRepository activationRepository) {
        this.authRepository = authRepository;
        this.activationRepository = activationRepository;
    }

    public boolean isLoading() {
        return loading;
    }

    public String getEmail() {
        return email;
    }

    public boolean hasAuthenticatedAccount(String email) {
        return authRepository.getUidForEmail(email) != null;
    }

    public void register(String email, String password, boolean resumeOnly,
                         ResultCallback callback) {
        if (loading) {
            return;
        }
        this.email = email;
        this.callback = callback;
        loading = true;
        int id = ++requestId;
        String uid = authRepository.getUidForEmail(email);
        if (uid != null) {
            // Firebase já criado: uma nova tentativa envia apenas a ativação.
            activate(id, uid);
            return;
        }
        if (password == null || password.isEmpty()) {
            // Se a sessão expirou durante o retry, volta a pedir autenticação.
            ResultCallback result = finish(id);
            if (result != null) {
                result.onAuthFailure(AuthFailureKind.INVALID_CREDENTIALS);
            }
            return;
        }
        FirebaseAuthRepository.AccountCallback accountCallback =
                new FirebaseAuthRepository.AccountCallback() {
                    @Override
                    public void onSuccess(String firebaseUid) {
                        if (id == requestId) {
                            activate(id, firebaseUid);
                        }
                    }

                    @Override
                    public void onFailure(AuthFailureKind kind) {
                        ResultCallback result = finish(id);
                        if (result != null) {
                            result.onAuthFailure(kind);
                        }
                    }
                };
        // O caminho de retomada nunca cria uma conta sem validar a chave.
        if (resumeOnly) {
            authRepository.signInForActivation(email, password, accountCallback);
        } else {
            authRepository.createAccount(email, password, accountCallback);
        }
    }

    private void activate(int id, String uid) {
        activationCall = activationRepository.activate(email, uid,
                new ActivationRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        ResultCallback result = finish(id);
                        if (result != null) {
                            result.onSuccess();
                        }
                    }

                    @Override
                    public void onFailure(FailureKind kind) {
                        ResultCallback result = finish(id);
                        if (result != null) {
                            result.onActivationFailure(kind);
                        }
                    }
                });
        if (!loading) {
            return;
        }
        if (timeoutHandler == null) {
            timeoutHandler = new Handler(Looper.getMainLooper());
        }
        // Como nos outros POSTs, limita também a espera da interface durante o DNS.
        timeoutRunnable = () -> {
            ResultCallback result = this.callback;
            clearRequest();
            if (result != null) {
                result.onActivationFailure(FailureKind.INTERNAL);
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable,
                TimeUnit.SECONDS.toMillis(AstroApiClient.REQUEST_TIMEOUT_SECONDS));
    }

    private ResultCallback finish(int id) {
        if (id != requestId) {
            return null;
        }
        loading = false;
        activationCall = null;
        clearTimeout();
        ResultCallback result = callback;
        callback = null;
        return result;
    }

    private void clearTimeout() {
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    public void clearRequest() {
        // Descarta retornos da tela fechada, sem apagar a identidade Firebase criada.
        ++requestId;
        clearTimeout();
        if (activationCall != null) {
            activationCall.cancel();
            activationCall = null;
        }
        callback = null;
        loading = false;
    }

    public static boolean isPasswordValid(String password) {
        if (password.length() < 8 || password.length() > 4096) {
            return false;
        }
        boolean uppercase = false;
        boolean lowercase = false;
        boolean number = false;
        boolean special = false;
        String specialCharacters = "^$*.[]{}()?\"!@#%&/\\,><':;|_~";
        // Usa os mesmos caracteres especiais aceitos pela política do Firebase.
        for (char character : password.toCharArray()) {
            uppercase |= character >= 'A' && character <= 'Z';
            lowercase |= character >= 'a' && character <= 'z';
            number |= character >= '0' && character <= '9';
            special |= specialCharacters.indexOf(character) >= 0;
        }
        return uppercase && lowercase && number && special;
    }

    @Override
    protected void onCleared() {
        clearRequest();
    }

    public static class Factory implements ViewModelProvider.Factory {
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(FirstLoginPasswordViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new FirstLoginPasswordViewModel(
                    FirebaseAuthRepository.createDefault(),
                    new ActivationRepository(AstroApiClient.getApi())));
        }
    }
}
