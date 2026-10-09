package com.example.astro_mobile.data.api.dto;

import java.util.Collections;
import java.util.List;

public final class UserProfileData {
    private String nome;
    private String cargo;
    private String unidade;
    private String modalidade;
    private String email;
    private List<Nr> nrs;

    public String getNome() { return nome; }
    public String getCargo() { return cargo; }
    public String getUnidade() { return unidade; }
    public String getModalidade() { return modalidade; }
    public String getEmail() { return email; }
    public List<Nr> getNrs() { return nrs == null ? Collections.emptyList() : nrs; }

    public static final class Nr {
        private int code;
        private String validade;
        private String descricao;
        private String objetivo;
        private String aplicabilidade;

        public int getCode() { return code; }
        public String getValidade() { return validade; }
        public String getDescricao() { return descricao; }
        public String getObjetivo() { return objetivo; }
        public String getAplicabilidade() { return aplicabilidade; }
    }
}
