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
    @Test public void readsTextFromBlocksWithoutLegacyAnswer() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        com.example.astro_mobile.data.ai.ChatResponse response = gson.fromJson(
            "{\"session_id\":\"94229143-d20b-4766-80a4-05341435c236\",\"blocos\":[{\"tipo\":\"texto\",\"conteudo\":\"Antes\"},{\"tipo\":\"texto\",\"conteudo\":\"Depois\"}]}",
            com.example.astro_mobile.data.ai.ChatResponse.class);
        assertTrue(response.isValid());
        assertEquals("Antes\n\nDepois", response.getAnswer());
    }
    @Test public void acceptsTypedPhotoInsideTable() {
        String input = "[{\"tipo\":\"tabela\",\"versao\":1,\"titulo\":\"Pessoas\",\"colunas\":[{\"chave\":\"foto\",\"titulo\":\"Foto\"}],\"linhas\":[{\"foto\":{\"tipo\":\"imagem\",\"descricao\":\"Foto de Ana\",\"url\":null}}]}]";
        assertTrue(ChatStructuredContent.valid(JsonParser.parseString(input).getAsJsonArray()));
    }
    @Test public void validatesUserCardCompliance() {
        String input = "[{\"tipo\": \"ficha_usuario\", \"versao\": 1, \"titulo\": \"Ficha\", \"nome\": \"Ana\", \"email\": null, \"cargo\": null, \"unidade\": null, \"perfil\": null, \"situacao\": null, \"foto\": {\"tipo\": \"imagem\", \"url\": null, \"descricao\": \"Foto de Ana\"}, \"conformidade\": {\"percentual\": 50, \"total\": 2, \"vigentes\": 1, \"criterio\": \"NRs vigentes / NRs aplicáveis retornadas\", \"data_referencia\": \"2026-10-30\"}}]";
        assertTrue(ChatStructuredContent.valid(JsonParser.parseString(input).getAsJsonArray()));
        com.google.gson.JsonArray blocks = JsonParser.parseString(input).getAsJsonArray();
        blocks.get(0).getAsJsonObject().getAsJsonObject("conformidade").addProperty("percentual", 100);
        assertFalse(ChatStructuredContent.valid(blocks));
    }
    @Test public void acceptsMissingAndNullTableCellsAndIndependentGallery() {
        String table = "[{\"tipo\":\"tabela\",\"versao\":1,\"titulo\":\"Usuários\",\"pagina\":1,\"total_paginas\":2,\"total\":3,\"colunas\":[{\"chave\":\"nome\",\"titulo\":\"Nome\"},{\"chave\":\"email\",\"titulo\":\"E-mail\"}],\"linhas\":[{\"nome\":\"Ana\"},{\"nome\":\"Bia\",\"email\":null}]}]";
        assertTrue(ChatStructuredContent.valid(JsonParser.parseString(table).getAsJsonArray()));
        assertTrue(ChatStructuredContent.valid(JsonParser.parseString("[{\"tipo\":\"galeria_usuarios\",\"versao\":1,\"titulo\":\"Pessoas\",\"imagens\":[{\"nome\":\"Ana Exemplo\",\"url\":null}]}]").getAsJsonArray()));
        assertEquals("AE", ChatStructuredContent.initials("Ana Exemplo"));
        assertEquals("A", ChatStructuredContent.initials("Ana"));
    }
}
