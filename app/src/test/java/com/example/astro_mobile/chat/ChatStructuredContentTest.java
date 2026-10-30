package com.example.astro_mobile.chat;

import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChatStructuredContentTest {
    @Test public void acceptsMixedBlocksInOrder() {
        String input = "[{\"tipo\":\"texto\",\"conteudo\":\"Antes\"},{\"tipo\":\"grafico_linhas\",\"versao\":1,\"titulo\":\"Evolução\",\"fonte\":\"Astro\",\"unidade\":\"participações\",\"dados\":[{\"categoria\":\"2026-01\",\"valor\":0}]},{\"tipo\":\"texto\",\"conteudo\":\"Depois\"}]";
        assertTrue(ChatStructuredContent.valid(JsonParser.parseString(input).getAsJsonArray()));
    }
    @Test public void rejectsUnknownAndInvalidGraphs() {
        assertFalse(ChatStructuredContent.valid(JsonParser.parseString("[{\"tipo\":\"grafico_desconhecido\",\"versao\":1,\"titulo\":\"x\"}]").getAsJsonArray()));
        assertFalse(ChatStructuredContent.valid(JsonParser.parseString("[{\"tipo\":\"grafico_barras\",\"versao\":1,\"titulo\":\"x\",\"fonte\":\"x\",\"unidade\":\"x\",\"dados\":[{\"categoria\":\"a\",\"valor\":-1}]}]").getAsJsonArray()));
    }
    @Test public void readsBlocksFromLiveAndHistoryAndPreservesFallback() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        com.example.astro_mobile.data.ai.ChatResponse response = gson.fromJson("{\"session_id\":\"94229143-d20b-4766-80a4-05341435c236\",\"resposta\":\"alternativa\",\"blocos\":[{\"tipo\":\"texto\",\"conteudo\":\"Olá\"}]}", com.example.astro_mobile.data.ai.ChatResponse.class);
        ChatMessage message = ChatMessage.fromResponse(response.getAnswer(), response.getBlocks());
        assertEquals("alternativa", message.getText());
        assertTrue(ChatStructuredContent.valid(message.getBlocks().getAsJsonArray()));
        com.example.astro_mobile.data.ai.SessionHistory history = gson.fromJson("{\"session_id\":\"94229143-d20b-4766-80a4-05341435c236\",\"status\":\"ativa\",\"total\":1,\"mensagens\":[{\"role\":\"assistant\",\"content\":\"alternativa\",\"blocos\":[{\"tipo\":\"texto\",\"conteudo\":\"Olá\"}]}]}", com.example.astro_mobile.data.ai.SessionHistory.class);
        assertTrue(history.isValid());
        assertTrue(ChatStructuredContent.valid(ChatMessage.fromHistory(history.messages.get(0).content, false, history.messages.get(0).blocos).getBlocks().getAsJsonArray()));
    }
}
