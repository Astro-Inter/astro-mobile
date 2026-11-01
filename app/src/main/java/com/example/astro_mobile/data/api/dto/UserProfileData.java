package com.example.astro_mobile.data.api.dto;

import java.util.Collections;
import java.util.List;
import com.google.gson.annotations.SerializedName;

public final class UserProfileData {
    @SerializedName(value = "name", alternate = {"nome"}) private String nome;
    @SerializedName(value = "role", alternate = {"cargo"}) private String cargo;
    @SerializedName(value = "unit", alternate = {"unidade"}) private String unidade;
    @SerializedName(value = "workModel", alternate = {"modalidade"}) private String modalidade;
    private String email;
    private String profilePhotoUrl;
    private List<Nr> nrs;

    public String getNome() { return nome; }
    public String getCargo() { return cargo; }
    public String getUnidade() { return unidade; }
    public String getModalidade() { return modalidade; }
    public String getEmail() { return email; }
    public String getProfilePhotoUrl() {
        return profilePhotoUrl == null || profilePhotoUrl.trim().isEmpty()
                ? null : profilePhotoUrl.trim();
    }
    public List<Nr> getNrs() { return nrs == null ? Collections.emptyList() : nrs; }

    public static final class Nr {
        private int code;
        @SerializedName(value = "expiresAt", alternate = {"validade"}) private String validade;
        @SerializedName(value = "description", alternate = {"descricao"}) private String descricao;
        @SerializedName(value = "objective", alternate = {"objetivo"}) private String objetivo;
        @SerializedName(value = "applicable", alternate = {"aplicabilidade"}) private String aplicabilidade;

        public int getCode() { return code; }
        public String getValidade() { return validade; }
        public String getDescricao() { return descricao; }
        public String getObjetivo() { return objetivo; }
        public String getAplicabilidade() { return aplicabilidade; }
    }
}
