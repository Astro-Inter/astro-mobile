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
}
