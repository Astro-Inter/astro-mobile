package com.example.astro_mobile.data.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Contratos v1 da PR #33. Não calcula métricas nem acessa a rede. */
public final class ChatAnalyticalContract {
    private static final Gson GSON = new Gson();
    private static final Set<String> UNITS = new HashSet<>(Arrays.asList("pessoas", "pares_pessoa_nr", "participações", "conclusões", "registros", "conflitos", "envios", "meses", "%"));
    private static final Set<String> FILTERS = new HashSet<>(Arrays.asList("nome", "cargo", "unidade", "nr", "evento_id", "conformidade_id", "incluir_agendados", "status"));
    private ChatAnalyticalContract() { }
    public static ChatIndicatorsBlock indicators(JsonObject block) { return GSON.fromJson(block, ChatIndicatorsBlock.class); }
    public static ChatContextBlock context(JsonObject block) { return GSON.fromJson(block, ChatContextBlock.class); }
    public static ChatMatrixBlock matrix(JsonObject block) { return GSON.fromJson(block, ChatMatrixBlock.class); }
    public static ChatDatasetSummary dataset(JsonObject block) { return GSON.fromJson(block, ChatDatasetSummary.class); }

    public static boolean valid(JsonObject block) {
        try {
            if (!integer(block.get("versao"), BigInteger.ONE, BigInteger.ONE)) return false;
            switch (block.get("tipo").getAsString()) {
                case "indicadores": return validIndicators(block);
                case "contexto_dados": return validContext(block);
                case "matriz_conformidade": return validMatrix(block);
                default: return false;
            }
        } catch (RuntimeException invalid) { return false; }
    }

    public static boolean validDataset(JsonObject block, int received) {
        try {
            if (!integer(block.get("solicitada"), BigInteger.ONE, null)
                    || !integer(block.get("retornada"), BigInteger.ZERO, BigInteger.valueOf(200))
                    || !integer(block.get("total"), BigInteger.ZERO, null)
                    || !bool(block.get("limitado")) || !bool(block.get("tem_mais"))) return false;
            ChatDatasetSummary summary = dataset(block);
            BigInteger count = BigInteger.valueOf(summary.getReturned());
            BigInteger expected = summary.getRequested().min(summary.getTotal());
            if ((received >= 0 && received != summary.getReturned()) || count.compareTo(expected) > 0
                    || summary.hasMore() != (count.compareTo(summary.getTotal()) < 0)
                    || summary.isLimited() != (count.compareTo(expected) < 0)) return false;
            String reason = summary.getLimitReason();
            return summary.isLimited() ? "maximo_resultados".equals(reason) || "tamanho_resposta".equals(reason) : reason == null;
        } catch (RuntimeException invalid) { return false; }
    }

    private static boolean validIndicators(JsonObject block) {
        if (!text(block.get("titulo"), 200) || !text(block.get("universo"), 300)
                || !"conjunto_autorizado".equals(block.get("abrangencia").getAsString())) return false;
        JsonArray items = block.getAsJsonArray("indicadores");
        if (items == null || items.isEmpty() || items.size() > 10) return false;
        Set<String> keys = new HashSet<>();
        for (JsonElement item : items) {
            JsonObject value = item.getAsJsonObject();
            if (!key(value.get("chave")) || !keys.add(value.get("chave").getAsString())
                    || !text(value.get("rotulo"), 120) || !text(value.get("criterio"), 300)
                    || !text(value.get("unidade"), 30) || !UNITS.contains(value.get("unidade").getAsString())) return false;
            JsonElement number = value.get("valor");
            if ("%".equals(value.get("unidade").getAsString())) {
                if (!integer(value.get("numerador"), BigInteger.ZERO, null) || !integer(value.get("denominador"), BigInteger.ZERO, null)) return false;
                BigInteger numerator = value.get("numerador").getAsBigInteger(), denominator = value.get("denominador").getAsBigInteger();
                if (numerator.compareTo(denominator) > 0) return false;
                if (denominator.signum() == 0) { if (number == null || !number.isJsonNull()) return false; }
                else if (!numeric(number) || number.getAsBigDecimal().signum() < 0 || number.getAsBigDecimal().compareTo(BigDecimal.valueOf(100)) > 0) return false;
            } else if (!integer(number, BigInteger.ZERO, null) || !absent(value.get("numerador")) || !absent(value.get("denominador"))) return false;
        }
        return true;
    }

    private static boolean validMatrix(JsonObject block) {
        if (!text(block.get("titulo"), 200) || !"referencia_pessoa".equals(block.get("chave_pessoa").getAsString())
                || !"nr".equals(block.get("chave_categoria").getAsString()) || !"situacao".equals(block.get("chave_situacao").getAsString())
                || !"local".equals(block.get("paginacao").getAsString())
                || !integer(block.get("pagina"), BigInteger.ONE, BigInteger.ONE) || !integer(block.get("total_paginas"), BigInteger.ONE, BigInteger.ONE)) return false;
        ChatMatrixBlock model = matrix(block);
        if (model.getColumns() == null || model.getColumns().isEmpty() || model.getColumns().size() > 10
                || model.getRows() == null || model.getRows().isEmpty() || !validDataset(block, model.getRows().size())) return false;
        Set<String> columns = new HashSet<>();
        for (ChatMatrixBlock.Column column : model.getColumns()) {
            if (column.getKey() == null || !column.getKey().matches("[a-z][a-z0-9_]{0,39}") || !columns.add(column.getKey())
                    || column.getTitle() == null || column.getTitle().isEmpty() || column.getTitle().length() > 100) return false;
        }
        Set<ChatMatrixBlock.CellKey> cells = new HashSet<>();
        Set<String> states = new HashSet<>(Arrays.asList("VIGENTE", "VENCIDA", "SEM_VALIDADE", "SEM_REGISTRO"));
        for (java.util.Map<String, JsonElement> row : model.getRows()) {
            if (!row.keySet().equals(columns) || !integer(row.get("referencia_pessoa"), BigInteger.ONE, null)
                    || !integer(row.get("nr"), BigInteger.ONE, BigInteger.valueOf(100))
                    || !text(row.get("situacao"), 30) || !states.contains(row.get("situacao").getAsString())
                    || !cells.add(new ChatMatrixBlock.CellKey(row.get("referencia_pessoa").getAsBigInteger(), row.get("nr").getAsInt()))) return false;
            for (JsonElement value : row.values()) if (!validCell(value)) return false;
        }
        return true;
    }

    public static boolean validCell(JsonElement value) {
        if (absent(value)) return true;
        if (value.isJsonPrimitive()) return !value.getAsJsonPrimitive().isString() || value.getAsString().length() <= 500;
        if (!value.isJsonObject()) return false;
        JsonObject image = value.getAsJsonObject();
        JsonElement url = image.get("url");
        return "imagem".equals(image.get("tipo").getAsString()) && text(image.get("descricao"), 280)
                && (absent(url) || (url.isJsonPrimitive() && url.getAsJsonPrimitive().isString() && url.getAsString().length() <= 4096));
    }

    private static boolean validContext(JsonObject block) {
        if (!text(block.get("fonte"), 200) || !text(block.get("escopo"), 200) || !date(block.get("referencia"))
                || !date(block.get("inicio")) || !date(block.get("fim"))
                || absent(block.get("inicio")) != absent(block.get("fim"))) return false;
        if (!absent(block.get("inicio")) && LocalDate.parse(block.get("fim").getAsString()).isBefore(LocalDate.parse(block.get("inicio").getAsString()))) return false;
        if (!validDataset(block.getAsJsonObject("conjunto"), -1) || !text(block.getAsJsonObject("conjunto").get("unidade_total"), 80)) return false;
        if (!stringList(block.get("criterios"), 1, 8, 500) || !stringList(block.get("avisos"), 0, 8, 500)) return false;
        JsonObject filters = block.getAsJsonObject("filtros");
        if (filters == null || filters.size() > 12 || !FILTERS.containsAll(filters.keySet())) return false;
        for (java.util.Map.Entry<String, JsonElement> entry : filters.entrySet()) {
            JsonElement value = entry.getValue();
            if (value == null || value.isJsonNull() || value.toString().length() > 300) return false;
            if (value.isJsonArray()) { if (!stringList(value, 0, 100, 300)) return false; }
            else if (!value.isJsonPrimitive()) return false;
        }
        return true;
    }

    private static boolean stringList(JsonElement value, int min, int max, int length) {
        if (value == null || !value.isJsonArray()) return false;
        JsonArray list = value.getAsJsonArray();
        if (list.size() < min || list.size() > max) return false;
        for (JsonElement item : list) if (!text(item, length)) return false;
        return true;
    }
    private static boolean date(JsonElement value) { if (absent(value)) return true; if (!text(value, 10)) return false; LocalDate.parse(value.getAsString()); return true; }
    private static boolean key(JsonElement value) { return text(value, 40) && value.getAsString().matches("[a-z][a-z0-9_]{0,39}"); }
    private static boolean bool(JsonElement value) { return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean(); }
    private static boolean absent(JsonElement value) { return value == null || value.isJsonNull(); }
    private static boolean numeric(JsonElement value) { if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return false; value.getAsBigDecimal(); return true; }
    private static boolean text(JsonElement value, int max) { return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() && !value.getAsString().isEmpty() && value.getAsString().length() <= max; }
    private static boolean integer(JsonElement value, BigInteger min, BigInteger max) {
        if (!numeric(value)) return false;
        BigInteger number = value.getAsBigDecimal().toBigIntegerExact();
        return number.compareTo(min) >= 0 && (max == null || number.compareTo(max) <= 0);
    }
}
