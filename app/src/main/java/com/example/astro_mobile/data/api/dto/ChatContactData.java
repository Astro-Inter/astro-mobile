package com.example.astro_mobile.data.api.dto;

public final class ChatContactData {
    public Long id;
    public String name;
    public String role;

    public boolean isValid() {
        return id != null && id > 0 && name != null && !name.trim().isEmpty()
                && ("COLABORADOR".equals(role) || "GESTOR".equals(role) || "GESTOR_WORKSPACE".equals(role));
    }
}
