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
    @Test public void validityColorsStayConsistentAcrossOrderAndChartType() {
        assertEquals(ChatStructuredContent.chartColor("grafico_barras", "Vencidas", 0),
                ChatStructuredContent.chartColor("grafico_setores", "Vencidas", 4));
        assertNotEquals(ChatStructuredContent.chartColor("grafico_setores", "Vencidas", 0),
                ChatStructuredContent.chartColor("grafico_setores", "Sem validade", 0));
    }
    static com.google.gson.JsonArray localTable(int count, int total, boolean limited) {
        com.google.gson.JsonObject table = new com.google.gson.JsonObject();
        table.addProperty("tipo", "tabela"); table.addProperty("versao", 2); table.addProperty("titulo", "Usuários");
        table.addProperty("paginacao", "local"); table.addProperty("solicitada", limited ? 500 : count);
        table.addProperty("retornada", count); table.addProperty("total", total);
        table.addProperty("tem_mais", total > count); table.addProperty("limitado", limited);
        table.add("motivo_limite", limited ? new com.google.gson.JsonPrimitive("maximo_resultados") : com.google.gson.JsonNull.INSTANCE);
        table.add("colunas", JsonParser.parseString("[{\"chave\":\"nome\",\"titulo\":\"Nome\"}]"));
        com.google.gson.JsonArray rows = new com.google.gson.JsonArray();
        for (int n=0; n<count; n++) { com.google.gson.JsonObject row = new com.google.gson.JsonObject(); row.addProperty("nome", "Pessoa " + n); rows.add(row); }
        table.add("linhas", rows); com.google.gson.JsonArray blocks = new com.google.gson.JsonArray(); blocks.add(table); return blocks;
    }
    @Test public void acceptsLocalTablesUpTo200AndRejectsBrokenMetadata() {
        assertTrue(ChatStructuredContent.valid(localTable(100,143,false)));
        assertTrue(ChatStructuredContent.valid(localTable(200,300,true)));
        assertFalse(ChatStructuredContent.valid(localTable(201,300,true)));
        com.google.gson.JsonArray blocks = localTable(100,143,false);
        blocks.get(0).getAsJsonObject().addProperty("retornada", 99);
        assertFalse(ChatStructuredContent.valid(blocks));
        blocks = localTable(100,143,false); blocks.get(0).getAsJsonObject().addProperty("versao",1);
        assertFalse(ChatStructuredContent.valid(blocks));
    }
    @Test public void acceptsOwnProfileWithAbsentOptionalFields() {
        assertTrue(ChatStructuredContent.valid(JsonParser.parseString("[{\"tipo\":\"ficha_usuario\",\"versao\":1,\"titulo\":\"Meu perfil\",\"origem\":\"usuario_autenticado\",\"nome\":null,\"modalidade\":\"HIBRIDO\",\"foto\":{\"tipo\":\"imagem\",\"url\":null,\"descricao\":\"Foto do usuário autenticado\"}}]").getAsJsonArray()));
        assertEquals("●", ChatStructuredContent.initials(null));
    }
}
