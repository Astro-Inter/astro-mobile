package com.example.astro_mobile.manager.events;

import androidx.lifecycle.ViewModel;

public final class ManagerEventDetailsViewModel extends ViewModel {
    private boolean loaded;

    // Mantém a espera mock concluída ao voltar de outra tela ou recriar as Views.
    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }
}
