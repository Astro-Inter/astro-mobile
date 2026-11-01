package com.example.astro_mobile.chat;

import com.example.astro_mobile.data.ai.*;
import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChatAnalyticalContractTest {
    static JsonArray mixed() throws Exception {
        try (InputStream input = ChatAnalyticalContractTest.class.getResourceAsStream("/chat/analytical-mixed.json")) {
            assertNotNull(input);
            return JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonArray();
        }
    }
    @Test public void supportsEveryNewDiscriminatorAndRejectsOtherVersions() throws Exception {
        JsonArray blocks = mixed();
        assertTrue(ChatStructuredContent.valid(blocks));
        for (int index : new int[]{1, 2, 4, 5}) {
            JsonObject block = blocks.get(index).getAsJsonObject().deepCopy();
            block.addProperty("versao", 2);
            assertFalse(ChatStructuredContent.validBlock(block));
        }
        assertEquals("indicadores", blocks.get(1).getAsJsonObject().get("tipo").getAsString());
        assertEquals("matriz_conformidade", blocks.get(2).getAsJsonObject().get("tipo").getAsString());
        assertEquals("grafico_rosca", blocks.get(4).getAsJsonObject().get("tipo").getAsString());
        assertEquals("contexto_dados", blocks.get(5).getAsJsonObject().get("tipo").getAsString());
    }
    @Test public void blockOnlyPostAndHistoryKeepTheSameData() throws Exception {
        JsonArray blocks = mixed();
        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        json.addProperty("session_id", "94229143-d20b-4766-80a4-05341435c236");
        // Sem resposta e sem bloco de texto: envelope analítico válido.
        blocks.remove(3); blocks.remove(0);
        json.add("blocos", blocks);
        ChatResponse post = gson.fromJson(json, ChatResponse.class);
        assertTrue(post.isValid());
        assertNull(post.getAnswer());
        ChatMessage live = ChatMessage.fromResponse(post.getAnswer(), post.getBlocks());
        JsonObject entry = new JsonObject(); entry.addProperty("role", "assistant"); entry.addProperty("content", ""); entry.add("blocos", blocks);
        JsonArray messages = new JsonArray(); messages.add(entry);
        json.addProperty("status", "ativa"); json.addProperty("total", 1); json.add("mensagens", messages);
        SessionHistory history = gson.fromJson(json, SessionHistory.class);
        assertTrue(history.isValid());
        ChatMessage restored = ChatMessage.fromHistory(history.messages.get(0).content, false, history.messages.get(0).blocos);
        assertEquals(live.getBlocks(), restored.getBlocks());
        assertEquals("", live.getText());
    }
    @Test public void localSummaryDistinguishesFulfilledRequestAndSafetyCuts() throws Exception {
        JsonObject summary = mixed().get(5).getAsJsonObject().getAsJsonObject("conjunto");
        summary.addProperty("solicitada", 100); summary.addProperty("retornada", 100);
        assertTrue(ChatAnalyticalContract.validDataset(summary, 100));
        summary.addProperty("limitado", true); // 100 pedidos e atendidos não são corte.
        assertFalse(ChatAnalyticalContract.validDataset(summary, 100));
        summary.addProperty("solicitada", 500); summary.addProperty("retornada", 200); summary.addProperty("motivo_limite", "maximo_resultados");
        assertTrue(ChatAnalyticalContract.validDataset(summary, 200));
        summary.addProperty("retornada", 0); summary.addProperty("motivo_limite", "tamanho_resposta");
        assertTrue(ChatAnalyticalContract.validDataset(summary, 0));
        assertEquals(273, ChatAnalyticalContract.dataset(summary).getTotal().intValueExact());
    }
    @Test public void nullPercentageIsNoBaseAndCountsRemainCounts() throws Exception {
        JsonObject block = mixed().get(1).getAsJsonObject();
        JsonObject percentage = block.getAsJsonArray("indicadores").get(1).getAsJsonObject();
        percentage.addProperty("numerador", 0); percentage.addProperty("denominador", 0); percentage.add("valor", JsonNull.INSTANCE);
        assertTrue(ChatAnalyticalContract.valid(block));
        assertNull(ChatAnalyticalContract.indicators(block).getIndicators().get(1).getValue());
        percentage.addProperty("valor", 0);
        assertFalse(ChatAnalyticalContract.valid(block));
        percentage.add("valor", JsonNull.INSTANCE);
        block.getAsJsonArray("indicadores").get(0).getAsJsonObject().addProperty("valor", 1.5);
        assertFalse(ChatAnalyticalContract.valid(block));
    }
    @Test public void matrixUsesPersonAndNrAndRejectsDuplicateCells() throws Exception {
        JsonObject matrix = mixed().get(2).getAsJsonObject();
        assertTrue(ChatAnalyticalContract.valid(matrix));
        ChatMatrixBlock model = ChatAnalyticalContract.matrix(matrix);
        assertEquals(2, model.getRows().size()); // Dois homônimos, nenhuma fusão por nome.
        matrix.getAsJsonArray("linhas").get(1).getAsJsonObject().addProperty("referencia_pessoa", 42);
        assertFalse(ChatAnalyticalContract.valid(matrix));
        matrix.getAsJsonArray("linhas").get(1).getAsJsonObject().addProperty("nr", 12);
        assertTrue(ChatAnalyticalContract.valid(matrix));
        matrix.getAsJsonArray("linhas").get(1).getAsJsonObject().addProperty("situacao", "DESCONHECIDA");
        assertFalse(ChatAnalyticalContract.valid(matrix));
    }
    @Test public void emptyDatasetAndSizeCutAreDifferentValidContexts() throws Exception {
        JsonObject block = mixed().get(5).getAsJsonObject();
        JsonObject summary = block.getAsJsonObject("conjunto");
        summary.addProperty("retornada", 0); summary.addProperty("total", 0); summary.addProperty("tem_mais", false);
        assertTrue(ChatAnalyticalContract.valid(block));
        summary.addProperty("total", 273); summary.addProperty("tem_mais", true); summary.addProperty("limitado", true); summary.addProperty("motivo_limite", "tamanho_resposta");
        assertTrue(ChatAnalyticalContract.valid(block));
        block.getAsJsonObject("filtros").addProperty("token", "nao-permitido");
        assertFalse(ChatAnalyticalContract.valid(block));
    }
    @Test public void zerosAndLegacyGraphsAreAcceptedWithoutChangingValues() throws Exception {
        JsonObject chart = mixed().get(4).getAsJsonObject();
        for (JsonElement point : chart.getAsJsonArray("dados")) point.getAsJsonObject().addProperty("valor", 0);
        for (String type : new String[]{"grafico_rosca", "grafico_setores", "grafico_barras", "grafico_linhas"}) {
            chart.addProperty("tipo", type); assertTrue(ChatStructuredContent.validBlock(chart));
        }
        assertEquals(3, chart.getAsJsonArray("dados").size());
        chart.getAsJsonArray("dados").get(0).getAsJsonObject().addProperty("valor", .5);
        assertFalse(ChatStructuredContent.validBlock(chart));
    }
}
