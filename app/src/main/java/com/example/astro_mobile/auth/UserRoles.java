package com.example.astro_mobile.auth;

/** Perfis reconhecidos pelo fluxo de entrada; autorização continua no backend. */
public final class UserRoles {
    private UserRoles() { }

    public static boolean isKnown(String role) {
        return "COLABORADOR".equals(role) || "GESTOR".equals(role)
                || "GESTOR_WORKSPACE".equals(role);
    }
}
