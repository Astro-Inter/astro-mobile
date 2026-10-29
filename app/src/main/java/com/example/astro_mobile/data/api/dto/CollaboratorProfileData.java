package com.example.astro_mobile.data.api.dto;

import java.util.List;

/** Dados do perfil de um colaborador consultado pelo gestor. */
public final class CollaboratorProfileData {
    public Long id;
    public String name, roleName, profilePhotoUrl, unitName, workModel, email;
    public Long pendingCount;
    public List<UserProfileData.Nr> nrs;
}
