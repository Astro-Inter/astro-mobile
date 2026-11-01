package com.example.astro_mobile.chat;

/** Rótulos de apresentação: os códigos recebidos continuam intactos no bloco original. */
final class ChatDisplayLabels {
    private ChatDisplayLabels() { }
    static String format(String key, String value) {
        if (value == null) return null;
        if ("perfil".equals(key) || "tipo".equals(key) || "categoria".equals(key)) {
            switch (value) {
                case "GESTOR_WORKSPACE": return "Gestor do workspace";
                case "GESTOR": return "Gestor";
                case "COLABORADOR": return "Colaborador";
                case "ADMIN": return "Administrador";
            }
        }
        if ("situacao".equals(key) || "status".equals(key)) {
            switch (value) {
                case "VIGENTE": return "Vigente";
                case "VENCIDA": return "Vencida";
                case "SEM_VALIDADE": return "Sem validade";
                case "SEM_REGISTRO": return "Sem registro";
                case "ATIVO": return "Ativo";
                case "INATIVO": return "Inativo";
            }
        }
        if ("modalidade".equals(key)) {
            switch (value) {
                case "PRESENCIAL": return "Presencial";
                case "HIBRIDO": return "Híbrido";
                case "REMOTO": return "Remoto";
            }
        }
        return value;
    }
    static String unit(String value) { return "pares_pessoa_nr".equals(value) ? "pares pessoa/NR" : value; }
    static String field(String key) {
        switch (key) {
            case "nome": return "Nome";
            case "cargo": return "Cargo";
            case "unidade": return "Unidade";
            case "nr": return "NR";
            case "evento_id": return "Referência do evento";
            case "conformidade_id": return "Referência de conformidade";
            case "incluir_agendados": return "Incluir agendados";
            case "status": return "Situação";
            default: return key;
        }
    }
}
