package com.example.astro_mobile.manager.forms.details;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import com.example.astro_mobile.data.api.ReceivedFormDetailsRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.ReceivedFormDetails;

public final class FormDetailsViewModel extends ViewModel {
    private final ReceivedFormDetailsRepository repository;
    private ReceivedFormDetails data;
    private boolean loading;
    private boolean loaded;
    private boolean consentAccepted;
    private boolean permissionError;

    public FormDetailsViewModel(ReceivedFormDetailsRepository repository) { this.repository = repository; }
    public ReceivedFormDetails getData() { return data; }
    public boolean isLoading() { return loading; }
    // Mantém o resultado da consulta separado do consentimento de localização.
    public void load(String id, ReceivedFormDetailsRepository.ResultCallback callback) {
        loading = true;
        repository.load(id, new ReceivedFormDetailsRepository.ResultCallback() {
            @Override public void onSuccess(ReceivedFormDetails details) {
                data = details;
                loaded = true;
                loading = false;
                callback.onSuccess(details);
            }
            @Override public void onFailure(FailureKind failure) {
                loading = false;
                callback.onFailure(failure);
            }
        });
    }
    public void cancel() { repository.cancel(); loading = false; }
    @Override protected void onCleared() { cancel(); }
    public static final class Factory implements ViewModelProvider.Factory {
        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return modelClass.cast(new FormDetailsViewModel(ReceivedFormDetailsRepository.createDefault()));
        }
    }

    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }
    public boolean isConsentAccepted() { return consentAccepted; }
    public boolean hasPermissionError() { return permissionError; }

    public void clearConsent() {
        // Desmarcar o checkbox retira o consentimento desta tela, sem alterar a permissão do Android.
        consentAccepted = false;
        permissionError = false;
    }

    public void onPermissionResult(boolean preciseGranted) {
        // Localização aproximada e recusa mantêm o consentimento desmarcado.
        consentAccepted = preciseGranted;
        permissionError = !preciseGranted;
    }
}
