package com.example.astro_mobile.chat;

import com.example.astro_mobile.data.ai.ChatFileBlock;
import com.example.astro_mobile.data.ai.ChatResponse;
import com.example.astro_mobile.data.ai.SessionHistory;
import com.google.gson.*;
import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChatFileBlockTest {
    private JsonObject file() {
        return JsonParser.parseString("{\"tipo\":\"arquivo\",\"versao\":1,\"id\":\"fc9acaf4-4ea1-416b-a1ed-dc2d1c3d90b4\",\"nome\":\"relatorio-de-rh.pdf\",\"mime_type\":\"application/pdf\",\"tamanho_bytes\":76360,\"url\":\"https://files.example/relatorio.pdf?assinatura=EXEMPLO\",\"expira_em\":\"2026-11-01T20:00:00Z\",\"estado\":\"disponivel\"}").getAsJsonObject();
    }
    @Test public void acceptsExactContractAndNullableLegacySize() {
        JsonObject b = file(); assertTrue(ChatStructuredContent.validBlock(b));
        assertEquals(Long.valueOf(76360), ChatFileBlock.fromJson(b).getSize());
        b.add("tamanho_bytes", JsonNull.INSTANCE); assertTrue(ChatFileBlock.valid(b));
        assertNull(ChatFileBlock.fromJson(b).getSize());
        b.remove("tamanho_bytes"); b.remove("url"); assertTrue(ChatFileBlock.valid(b));
        assertEquals(ChatFileBlock.Availability.UNAVAILABLE, ChatFileBlock.fromJson(b).availability(Instant.parse("2026-11-01T19:00:00Z")));
    }
    @Test public void expiryBoundaryAndServerStateControlDownload() {
        JsonObject b = file(); Instant before = Instant.parse("2026-11-01T19:59:59Z");
        assertEquals(ChatFileBlock.Availability.AVAILABLE, ChatFileBlock.fromJson(b).availability(before));
        assertEquals(ChatFileBlock.Availability.EXPIRED, ChatFileBlock.fromJson(b).availability(Instant.parse("2026-11-01T20:00:00Z")));
        b.addProperty("estado", "expirado"); assertEquals(ChatFileBlock.Availability.EXPIRED, ChatFileBlock.fromJson(b).availability(before));
        b.addProperty("estado", "indisponivel"); assertEquals(ChatFileBlock.Availability.UNAVAILABLE, ChatFileBlock.fromJson(b).availability(before));
        b.addProperty("estado", "disponivel"); b.add("url", JsonNull.INSTANCE);
        assertEquals(ChatFileBlock.Availability.UNAVAILABLE, ChatFileBlock.fromJson(b).availability(before));
    }
    @Test public void rejectsUnsafeUrlsNamesVersionsAndMetadata() {
        String[][] invalid = {{"url","http://files.example/a.pdf"},{"url","https://user:secret@files.example/a.pdf"},
                {"url","javascript:alert(1)"},{"url","https:///a.pdf"},{"nome","../relatorio.pdf"},
                {"nome","relatorio.txt"},{"nome","relatorio com espacos.pdf"},{"mime_type","text/html"},
                {"expira_em","2026-11-01"},{"id","42"},{"estado","erro"}};
        for (String[] pair : invalid) { JsonObject b=file();b.addProperty(pair[0],pair[1]);assertFalse(pair[0],ChatFileBlock.valid(b)); }
        for (double number : new double[]{0, -1, 2000001, 1.5}) {JsonObject b=file();b.addProperty("tamanho_bytes",number);assertFalse(ChatFileBlock.valid(b));}
        JsonObject b=file();b.addProperty("versao",2);assertFalse(ChatFileBlock.valid(b));
        b=file();b.addProperty("versao","1");assertFalse(ChatFileBlock.valid(b));
        b=file();b.addProperty("token","nao-permitido");assertFalse(ChatFileBlock.valid(b));
    }
    @Test public void postAndHistoryKeepSameFileAndOriginalDeadline() {
        JsonArray blocks=new JsonArray();blocks.add(file());
        JsonObject post=new JsonObject();post.addProperty("session_id","94229143-d20b-4766-80a4-05341435c236");post.add("blocos",blocks);
        Gson gson=new Gson();ChatResponse parsed=gson.fromJson(post,ChatResponse.class);assertTrue(parsed.isValid());
        JsonObject message=new JsonObject();message.addProperty("role","assistant");message.add("blocos",blocks.deepCopy());
        JsonArray messages=new JsonArray();messages.add(message);JsonObject history=new JsonObject();history.add("mensagens",messages);
        SessionHistory h=gson.fromJson(history,SessionHistory.class);
        assertEquals(parsed.getBlocks(),h.messages.get(0).blocos);
        assertEquals(Instant.parse("2026-11-01T20:00:00Z"),ChatFileBlock.fromJson(file()).getExpiration());
        assertEquals("fc9acaf4-4ea1-416b-a1ed-dc2d1c3d90b4",ChatFileBlock.fromJson(file()).getId());
    }
}
