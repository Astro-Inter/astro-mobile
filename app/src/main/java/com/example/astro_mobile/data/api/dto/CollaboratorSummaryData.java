package com.example.astro_mobile.data.api.dto;

/** Resumo do colaborador com a quantidade de eventos pendentes. */
public final class CollaboratorSummaryData {
    public Long id;
    public String name;
    public String roleName;
    public String profilePhotoUrl;
    public Long pendingCount;

    public boolean isValid() {
        return id != null && name != null && !name.trim().isEmpty() && pendingCount != null;
    }
}
