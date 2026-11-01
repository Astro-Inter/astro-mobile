package com.example.astro_mobile.data.ai;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Arquivo v1 da API de IA. A URL é uma capacidade temporária, nunca uma credencial Astro. */
public final class ChatFileBlock {
    public enum Availability { AVAILABLE, EXPIRED, UNAVAILABLE }
    public static final int MAX_BYTES = 2_000_000;
    private static final Set<String> FIELDS = new HashSet<>(Arrays.asList(
            "tipo", "versao", "id", "nome", "mime_type", "tamanho_bytes", "url", "expira_em", "estado"));
    @SerializedName("id") private String id;
    @SerializedName("nome") private String name;
    @SerializedName("mime_type") private String mimeType;
    @SerializedName("tamanho_bytes") private Long size;
    @SerializedName("url") private String url;
    @SerializedName("expira_em") private String expiresAt;
    @SerializedName("estado") private String state;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getMimeType() { return mimeType; }
    public Long getSize() { return size; }
    public String getUrl() { return url; }
    public Instant getExpiration() { return OffsetDateTime.parse(expiresAt).toInstant(); }
    public Availability availability(Instant now) {
        if ("expirado".equals(state) || !getExpiration().isAfter(now)) return Availability.EXPIRED;
        return "disponivel".equals(state) && url != null ? Availability.AVAILABLE : Availability.UNAVAILABLE;
    }
    public static ChatFileBlock fromJson(JsonObject block) { return new Gson().fromJson(block, ChatFileBlock.class); }
    public static boolean valid(JsonObject block) {
        try {
            if (!FIELDS.containsAll(block.keySet()) || !"arquivo".equals(string(block, "tipo"))
                    || !block.get("versao").isJsonPrimitive() || !block.get("versao").getAsJsonPrimitive().isNumber()
                    || block.get("versao").getAsBigDecimal().compareTo(java.math.BigDecimal.ONE) != 0
                    || !string(block, "id").matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
                    || !string(block, "nome").matches("[a-zA-Z0-9_-]+\\.pdf") || string(block, "nome").length() > 100
                    || !"application/pdf".equals(string(block, "mime_type"))
                    || !Arrays.asList("disponivel", "expirado", "indisponivel").contains(string(block, "estado"))) return false;
            OffsetDateTime.parse(string(block, "expira_em"));
            JsonElement size = block.get("tamanho_bytes");
            if (size != null && !size.isJsonNull() && (!size.isJsonPrimitive() || !size.getAsJsonPrimitive().isNumber()
                    || size.getAsBigDecimal().stripTrailingZeros().scale() > 0
                    || size.getAsBigDecimal().compareTo(java.math.BigDecimal.ONE) < 0
                    || size.getAsBigDecimal().compareTo(java.math.BigDecimal.valueOf(MAX_BYTES)) > 0)) return false;
            JsonElement url = block.get("url");
            if (url != null && !url.isJsonNull()) {
                String value = string(block, "url");
                URI uri = new URI(value);
                if (value.length() > 8192 || !"https".equals(uri.getScheme()) || uri.getHost() == null
                        || uri.getRawUserInfo() != null) return false;
            }
            return true;
        } catch (RuntimeException | java.net.URISyntaxException ignored) { return false; }
    }
    private static String string(JsonObject block, String key) {
        JsonElement value = block.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException();
        return value.getAsString();
    }
}
