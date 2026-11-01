package com.example.astro_mobile.chat;

import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChatDisplayLabelsTest {
    @Test public void translatesOnlyPresentationAndPreservesUnknownValues() {
        JsonObject block = new JsonObject();
        block.addProperty("perfil", "GESTOR_WORKSPACE");
        assertEquals("Gestor do workspace", ChatDisplayLabels.format("perfil", block.get("perfil").getAsString()));
        assertEquals("GESTOR_WORKSPACE", block.get("perfil").getAsString());
        assertEquals("Híbrido", ChatDisplayLabels.format("modalidade", "HIBRIDO"));
        assertEquals("GESTOR_WORKSPACE", ChatDisplayLabels.format("nome", "GESTOR_WORKSPACE"));
        assertEquals("NOVO_PERFIL", ChatDisplayLabels.format("perfil", "NOVO_PERFIL"));
        assertNull(ChatDisplayLabels.format("perfil", null));
    }
}
