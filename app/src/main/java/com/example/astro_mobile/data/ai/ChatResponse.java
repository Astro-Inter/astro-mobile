package com.example.astro_mobile.data.ai;

import com.google.gson.annotations.SerializedName;

public final class ChatResponse {
    @SerializedName("session_id") private String sessionId;
    @SerializedName("resposta") private String answer;
    @SerializedName("blocos") private com.google.gson.JsonElement blocks;

    public String getSessionId() { return sessionId; }
    public String getAnswer() {
        if (answer != null && !answer.trim().isEmpty()) return answer;
        if (blocks == null || !blocks.isJsonArray()) return null;
        StringBuilder text = new StringBuilder();
        for (com.google.gson.JsonElement block : blocks.getAsJsonArray()) {
            if (!block.isJsonObject()) continue;
            com.google.gson.JsonObject item = block.getAsJsonObject();
            com.google.gson.JsonElement type = item.get("tipo");
            com.google.gson.JsonElement content = item.get("conteudo");
            if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString()
                    || !"texto".equals(type.getAsString()) || content == null
                    || !content.isJsonPrimitive() || !content.getAsJsonPrimitive().isString()) continue;
            String value = content.getAsString().trim();
            if (value.isEmpty()) continue;
            if (text.length() > 0) text.append("\n\n");
            text.append(value);
        }
        return text.length() == 0 ? null : text.toString();
    }
    public com.google.gson.JsonElement getBlocks() { return blocks; }

    public boolean isValid() {
        return sessionId != null
                && sessionId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                && getAnswer() != null;
    }
}
