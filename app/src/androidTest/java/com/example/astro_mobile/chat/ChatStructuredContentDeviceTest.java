package com.example.astro_mobile.chat;

import static org.junit.Assert.*;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.astro_mobile.R;
import com.google.gson.JsonParser;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ChatStructuredContentDeviceTest {
    @Test public void rendersOrderedAccessibleTableGalleryAndFallback() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            String blocks = "[{\"tipo\":\"texto\",\"conteudo\":\"Antes\"},"
                + "{\"tipo\":\"tabela\",\"versao\":1,\"titulo\":\"Usuários\",\"pagina\":2,\"total_paginas\":3,\"total\":51,"
                + "\"colunas\":[{\"chave\":\"nome\",\"titulo\":\"Nome\"},{\"chave\":\"email\",\"titulo\":\"E-mail\"}],"
                + "\"linhas\":[{\"nome\":\"Ana Exemplo\"}]},"
                + "{\"tipo\":\"galeria_usuarios\",\"versao\":1,\"titulo\":\"Fotos\",\"imagens\":[{\"nome\":\"Ana Exemplo\",\"url\":null}]},"
                + "{\"tipo\":\"texto\",\"conteudo\":\"Depois\"}]";
            ChatStructuredContent.render(root, ChatMarkdown.create(context), "alternativa", JsonParser.parseString(blocks));
            assertEquals(4, root.getChildCount());
            assertEquals("Antes", ((TextView) ((LinearLayout) root.getChildAt(0)).getChildAt(0)).getText().toString());
            LinearLayout tableCard = (LinearLayout) root.getChildAt(1);
            assertEquals(context.getColor(R.color.chat_text), ((TextView) tableCard.getChildAt(0)).getCurrentTextColor());
            HorizontalScrollView scroll = (HorizontalScrollView) tableCard.getChildAt(1);
            LinearLayout table = (LinearLayout) scroll.getChildAt(0);
            LinearLayout line = (LinearLayout) table.getChildAt(1);
            assertEquals("Nome: Ana Exemplo, E-mail: —", line.getContentDescription().toString());
            assertEquals(context.getColor(R.color.chat_text), ((TextView) line.getChildAt(0)).getCurrentTextColor());
            assertTrue(((TextView) tableCard.getChildAt(2)).getText().toString().contains("2 de 3"));
            LinearLayout gallery = (LinearLayout) root.getChildAt(2);
            LinearLayout person = (LinearLayout) gallery.getChildAt(1);
            FrameLayout avatar = (FrameLayout) person.getChildAt(0);
            assertEquals("AE", ((TextView) avatar.getChildAt(0)).getText().toString());
            assertEquals("Depois", ((TextView) ((LinearLayout) root.getChildAt(3)).getChildAt(0)).getText().toString());
            int width = Math.round(240 * context.getResources().getDisplayMetrics().density);
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            root.layout(0, 0, width, root.getMeasuredHeight());
            assertTrue(table.getWidth() > scroll.getWidth());
            ChatStructuredContent.render(root, ChatMarkdown.create(context), "alternativa", JsonParser.parseString("[{\"tipo\":\"desconhecido\"}]"));
            assertEquals("alternativa", ((TextView) root.getChildAt(0)).getText().toString());
        });
    }
    @Test public void captureTabletPreviewWithSyntheticTable() throws Exception {
        try (androidx.test.core.app.ActivityScenario<com.example.astro_mobile.MainActivity> scenario =
                     androidx.test.core.app.ActivityScenario.launch(com.example.astro_mobile.MainActivity.class)) {
            scenario.onActivity(activity -> {
                android.widget.ScrollView page = new android.widget.ScrollView(activity);
                page.setBackgroundColor(activity.getColor(R.color.chat_surface));
                LinearLayout content = new LinearLayout(activity);
                content.setOrientation(LinearLayout.VERTICAL);
                int pad = Math.round(20 * activity.getResources().getDisplayMetrics().density);
                content.setPadding(pad, pad, pad, pad);
                content.setBackgroundResource(R.drawable.bg_chat_assistant);
                page.addView(content);
                activity.setContentView(page);
                String blocks = "[{\"tipo\":\"texto\",\"conteudo\":\"Confira os usuários encontrados:\"},"
                    + "{\"tipo\":\"tabela\",\"versao\":1,\"titulo\":\"Usuários\",\"pagina\":1,\"total_paginas\":2,\"total\":60,"
                    + "\"colunas\":[{\"chave\":\"nome\",\"titulo\":\"Nome\"},{\"chave\":\"email\",\"titulo\":\"E-mail\"},{\"chave\":\"cargo\",\"titulo\":\"Cargo\"},{\"chave\":\"unidade\",\"titulo\":\"Unidade\"}],"
                    + "\"linhas\":[{\"nome\":\"Ana Exemplo\",\"email\":\"ana.exemplo@example.com\",\"cargo\":\"Operadora de máquinas\",\"unidade\":\"Matriz São Paulo\"},{\"nome\":\"Bruno Exemplo\",\"email\":null,\"cargo\":\"Técnico de segurança\",\"unidade\":\"Matriz São Paulo\"}]},"
                    + "{\"tipo\":\"galeria_usuarios\",\"versao\":1,\"titulo\":\"Fotos\",\"imagens\":[{\"nome\":\"Ana Exemplo\",\"url\":null}]}]";
                ChatStructuredContent.render(content, ChatMarkdown.create(activity), "alternativa", JsonParser.parseString(blocks));
            });
            android.os.SystemClock.sleep(4000);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            android.graphics.Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            assertNotNull(bitmap);
            java.io.File file = new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("chat-qa"), "blocos-tablet.png");
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(file)) {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output);
            } finally { bitmap.recycle(); }
        }
    }
    @Test public void rendersAllChartsWithReadableLegendsOnNarrowViewport() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            for (String type : new String[]{"grafico_barras", "grafico_linhas", "grafico_setores"}) {
                com.google.gson.JsonObject block = new com.google.gson.JsonObject();
                block.addProperty("tipo", type); block.addProperty("versao", 1);
                block.addProperty("titulo", "Distribuição de exemplo");
                block.addProperty("fonte", "Dados simulados"); block.addProperty("unidade", "pessoas");
                com.google.gson.JsonArray points = new com.google.gson.JsonArray();
                for (int n = 0; n < 8; n++) {
                    com.google.gson.JsonObject point = new com.google.gson.JsonObject();
                    point.addProperty("categoria", n == 0 ? "GESTOR_WORKSPACE" : "Categoria longa de exemplo " + n);
                    point.addProperty("valor", n); points.add(point);
                }
                block.add("dados", points);
                com.google.gson.JsonArray blocks = new com.google.gson.JsonArray(); blocks.add(block);
                LinearLayout root = new LinearLayout(context); root.setOrientation(LinearLayout.VERTICAL);
                ChatStructuredContent.render(root, ChatMarkdown.create(context), "", blocks);
                LinearLayout card = (LinearLayout) root.getChildAt(0);
                if (!"grafico_setores".equals(type)) assertTrue(card.getChildAt(1) instanceof HorizontalScrollView);
                LinearLayout legend = (LinearLayout) card.getChildAt(2);
                assertTrue(((TextView) legend.getChildAt(1)).getText().toString().contains("Gestor do workspace: 0"));
                assertEquals("GESTOR_WORKSPACE", points.get(0).getAsJsonObject().get("categoria").getAsString());
                int width = Math.round(240 * context.getResources().getDisplayMetrics().density);
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                root.layout(0, 0, width, root.getMeasuredHeight());
                android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, root.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
                try { root.draw(new android.graphics.Canvas(bitmap)); } finally { bitmap.recycle(); }
            }
        });
    }
    private String activityCloseLabel() { return InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.chat_chart_close); }
    @Test public void chartDetailsCanBeOpenedAndClosed() throws Exception {
        try (androidx.test.core.app.ActivityScenario<com.example.astro_mobile.MainActivity> scenario =
                     androidx.test.core.app.ActivityScenario.launch(com.example.astro_mobile.MainActivity.class)) {
            scenario.onActivity(activity -> {
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setBackgroundColor(activity.getColor(R.color.chat_assistant));
                activity.setContentView(root);
                String blocks = "[{\"tipo\":\"grafico_setores\",\"versao\":1,\"titulo\":\"Validades por faixa\",\"fonte\":\"Dados fictícios para teste\",\"unidade\":\"registros\",\"dados\":[{\"categoria\":\"Vencidas\",\"valor\":2},{\"categoria\":\"Mais de 30 dias\",\"valor\":90},{\"categoria\":\"Sem validade\",\"valor\":140}]}]";
                ChatStructuredContent.render(root, ChatMarkdown.create(activity), "", JsonParser.parseString(blocks));
            });
            android.os.SystemClock.sleep(4000);
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withContentDescription("Ampliar gráfico: Validades por faixa"))
                    .perform(androidx.test.espresso.action.ViewActions.longClick());
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withContentDescription(activityCloseLabel()))
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            android.graphics.Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            java.io.File file = new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("chat-qa"), "chart-focus.png");
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(file)) { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output); }
            finally { bitmap.recycle(); }
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withContentDescription(activityCloseLabel()))
                    .perform(androidx.test.espresso.action.ViewActions.click());
        }
    }
    @Test public void localPagesRenderTenRowsAndPreserveProfileOrder() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            com.google.gson.JsonObject table = new com.google.gson.JsonObject();
            table.addProperty("tipo", "tabela"); table.addProperty("versao", 2); table.addProperty("titulo", "Usuários");
            table.addProperty("paginacao", "local"); table.addProperty("solicitada", 100); table.addProperty("retornada",100);
            table.addProperty("total",143); table.addProperty("tem_mais",true); table.addProperty("limitado",false);
            table.add("motivo_limite",com.google.gson.JsonNull.INSTANCE);
            table.add("colunas",JsonParser.parseString("[{\"chave\":\"nome\",\"titulo\":\"Nome\"}]"));
            com.google.gson.JsonArray rows = new com.google.gson.JsonArray();
            for(int n=0;n<100;n++){com.google.gson.JsonObject row=new com.google.gson.JsonObject();row.addProperty("nome","Pessoa "+n);rows.add(row);}
            table.add("linhas",rows); com.google.gson.JsonArray blocks=new com.google.gson.JsonArray();blocks.add(table);
            LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);
            ChatStructuredContent.render(root,ChatMarkdown.create(context),"",blocks);
            LinearLayout card=(LinearLayout)root.getChildAt(0);
            LinearLayout grid=(LinearLayout)((HorizontalScrollView)card.getChildAt(1)).getChildAt(0);
            assertEquals(11,grid.getChildCount());
            LinearLayout controls=(LinearLayout)card.getChildAt(card.getChildCount()-1);
            controls.getChildAt(2).performClick();
            assertEquals(11,grid.getChildCount());
            assertEquals("Pessoa 10",((TextView)((LinearLayout)grid.getChildAt(1)).getChildAt(0)).getText().toString());
            ChatStructuredContent.render(root,ChatMarkdown.create(context),"",blocks);
            grid=(LinearLayout)((HorizontalScrollView)((LinearLayout)root.getChildAt(0)).getChildAt(1)).getChildAt(0);
            assertEquals("Pessoa 0",((TextView)((LinearLayout)grid.getChildAt(1)).getChildAt(0)).getText().toString());
            ChatStructuredContent.render(root,ChatMarkdown.create(context),"",JsonParser.parseString("[{\"tipo\":\"texto\",\"conteudo\":\"Antes\"},{\"tipo\":\"ficha_usuario\",\"versao\":1,\"titulo\":\"Meu perfil\",\"origem\":\"usuario_autenticado\",\"nome\":null,\"modalidade\":\"HIBRIDO\",\"foto\":{\"tipo\":\"imagem\",\"url\":null,\"descricao\":\"Foto\"}},{\"tipo\":\"texto\",\"conteudo\":\"Depois\"}]"));
            assertEquals(3,root.getChildCount());
            LinearLayout profile=(LinearLayout)root.getChildAt(1);
            assertEquals(3,profile.getChildCount());
            assertEquals("Híbrido",((TextView)((LinearLayout)profile.getChildAt(2)).getChildAt(1)).getText().toString());
        });
    }
    @Test public void cappedTableAndFullProfilePreserveDetails() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);
            com.google.gson.JsonObject table=JsonParser.parseString("{\"tipo\":\"tabela\",\"versao\":2,\"titulo\":\"Usuários\",\"paginacao\":\"local\",\"solicitada\":500,\"retornada\":200,\"total\":300,\"tem_mais\":true,\"limitado\":true,\"motivo_limite\":\"maximo_resultados\",\"colunas\":[{\"chave\":\"nome\",\"titulo\":\"Nome\"}]}").getAsJsonObject();
            com.google.gson.JsonArray rows=new com.google.gson.JsonArray();
            for(int n=0;n<200;n++){com.google.gson.JsonObject row=new com.google.gson.JsonObject();row.addProperty("nome","Pessoa "+n);rows.add(row);}
            table.add("linhas",rows);com.google.gson.JsonArray blocks=new com.google.gson.JsonArray();blocks.add(table);
            ChatStructuredContent.render(root,ChatMarkdown.create(context),"",blocks);
            LinearLayout card=(LinearLayout)root.getChildAt(0);
            assertEquals(context.getString(R.string.chat_table_count_limit),((TextView)card.getChildAt(4)).getText().toString());
            LinearLayout controls=(LinearLayout)card.getChildAt(5);
            for(int n=0;n<19;n++)controls.getChildAt(2).performClick();
            assertFalse(controls.getChildAt(2).isEnabled());
            LinearLayout grid=(LinearLayout)((HorizontalScrollView)card.getChildAt(1)).getChildAt(0);
            assertEquals(11,grid.getChildCount());
            assertEquals("Pessoa 190",((TextView)((LinearLayout)grid.getChildAt(1)).getChildAt(0)).getText().toString());
            blocks=JsonParser.parseString("[{\"tipo\":\"ficha_usuario\",\"versao\":1,\"titulo\":\"Meu perfil\",\"origem\":\"usuario_autenticado\",\"nome\":\"Pessoa Exemplo\",\"email\":\"pessoa@example.com\",\"perfil\":\"COLABORADOR\",\"cargo\":\"Analista\",\"unidade\":\"Matriz\",\"modalidade\":\"HIBRIDO\",\"situacao\":\"ATIVO\",\"conformidade\":null,\"foto\":{\"tipo\":\"imagem\",\"url\":null,\"descricao\":\"Foto\"}}]").getAsJsonArray();
            ChatStructuredContent.render(root,ChatMarkdown.create(context),"",blocks);
            card=(LinearLayout)root.getChildAt(0);
            boolean hasMode=false;
            for(int i=0;i<card.getChildCount();i++) {
                View item=card.getChildAt(i);
                if (item instanceof LinearLayout && ((LinearLayout)item).getChildCount() == 2) item=((LinearLayout)item).getChildAt(1);
                if (!(item instanceof TextView)) continue;
                String text=((TextView)item).getText().toString();
                assertNotEquals("ATIVO",text);
                assertFalse(text.contains("0%"));
                if ("Híbrido".equals(text)) hasMode=true;
            }
            assertTrue(hasMode);
            LinearLayout identity=(LinearLayout)card.getChildAt(1);
            FrameLayout avatar=(FrameLayout)identity.getChildAt(0);
            assertEquals("PE",((TextView)avatar.getChildAt(0)).getText().toString());
            int width=Math.round(240*context.getResources().getDisplayMetrics().density);
            root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            assertTrue(root.getMeasuredHeight()>0);
        });
    }
}
