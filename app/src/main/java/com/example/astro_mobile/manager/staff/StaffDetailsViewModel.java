package com.example.astro_mobile.manager.staff;

import androidx.lifecycle.ViewModel;

public final class StaffDetailsViewModel extends ViewModel {
    private final boolean[] expandedNrs = new boolean[3];
    private boolean loaded;

    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }

    public boolean isExpanded(int index) { return expandedNrs[index]; }

    public void toggleNr(int index) {
        // Cada NR guarda seu próprio estado, sem fechar ou sobrepor as demais.
        expandedNrs[index] = !expandedNrs[index];
    }
}
