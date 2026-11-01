package com.example.astro_mobile.data.api.dto;

import java.util.List;
import com.google.gson.annotations.SerializedName;

/** Dados do perfil de um colaborador consultado pelo gestor. */
public final class CollaboratorProfileData {
    public Long id;
    @SerializedName(value = "name", alternate = {"nome"}) public String name;
    public String profilePhotoUrl, email;
    @SerializedName(value = "workModel", alternate = {"modalidade"}) public String workModel;
    @SerializedName(value = "role", alternate = {"roleName", "cargo"}) public String roleName;
    @SerializedName(value = "unit", alternate = {"unitName", "unidade"}) public String unitName;
    public Long pendingCount;
    public List<Nr> nrs;

    public static final class Nr {
        public int code;
        @SerializedName(value = "expiresAt", alternate = {"validade"}) public String expiresAt;
        @SerializedName(value = "description", alternate = {"descricao"}) public String description;
        @SerializedName(value = "objective", alternate = {"objetivo"}) public String objective;
        @SerializedName(value = "applicable", alternate = {"aplicabilidade"}) public String applicable;
    }
}
