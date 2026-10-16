package com.example.astro_mobile.manager.forms.details;

import androidx.lifecycle.ViewModel;

public final class FormDetailsViewModel extends ViewModel {
    private boolean loaded;
    private boolean consentAccepted;
    private boolean permissionError;

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
