package com.example.astro_mobile.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.astro_mobile.R;
import com.example.astro_mobile.data.ai.ChatMatrixBlock;
import com.google.gson.*;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ChatAnalyticalContentDeviceTest {
    static JsonArray fixture() throws Exception {
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("chat/analytical-mixed.json");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int size;
            while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);
            return JsonParser.parseString(new String(output.toByteArray(), StandardCharsets.UTF_8)).getAsJsonArray();
        }
    }
    static JsonArray withRows(JsonArray input, int count) {
        JsonArray blocks = input.deepCopy();
        JsonObject table = blocks.get(2).getAsJsonObject();
        JsonArray rows = new JsonArray();
        for (int index = 0; index < count; index++) {
            JsonObject row = table.getAsJsonArray("linhas").get(0).getAsJsonObject().deepCopy();
            row.addProperty("referencia_pessoa", 1000 + index); rows.add(row);
        }
        table.add("linhas", rows); table.addProperty("solicitada", count); table.addProperty("retornada", count);
        return blocks;
    }
    private LinearLayout render(JsonArray blocks) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LinearLayout root = new LinearLayout(context); root.setOrientation(LinearLayout.VERTICAL);
        ChatStructuredContent.render(root, ChatMarkdown.create(context), "alternativa", blocks);
        return root;
    }
    static List<String> texts(View view) {
        List<String> result = new ArrayList<>();
        if (view instanceof TextView) result.add(((TextView) view).getText().toString());
        if (view instanceof ViewGroup) for (int index = 0; index < ((ViewGroup) view).getChildCount(); index++) result.addAll(texts(((ViewGroup) view).getChildAt(index)));
        return result;
    }
    @Test public void mixedPostAndHistoryHaveIdenticalOrderAndAccessibleIndicators() throws Exception {
        JsonArray fixture = fixture();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ChatMessage live = ChatMessage.fromResponse("", fixture);
            ChatMessage history = ChatMessage.fromHistory("", false, fixture);
            LinearLayout root = render(live.getBlocks().getAsJsonArray());
            LinearLayout restored = render(history.getBlocks().getAsJsonArray());
            assertEquals(6, root.getChildCount());
            assertEquals(texts(root), texts(restored));
            List<String> content = texts(root);
            assertTrue(content.indexOf("Resumo dos dados") < content.indexOf("Matriz de conformidade da equipe"));
            assertTrue(content.indexOf("Matriz de conformidade da equipe") < content.indexOf("Distribuição de exemplo"));
            assertTrue(content.contains("75,0%")); assertTrue(content.contains("pares pessoa/NR"));
            assertTrue(content.contains("Base: 90 de 120"));
            LinearLayout indicators = (LinearLayout) root.getChildAt(1);
            View tile = indicators.getChildAt(2);
            assertTrue(tile.isFocusable()); assertTrue(tile.getContentDescription().toString().contains("120"));
            LinearLayout matrix = (LinearLayout) root.getChildAt(2);
            LinearLayout rows = (LinearLayout) ((HorizontalScrollView) matrix.getChildAt(1)).getChildAt(0);
            assertNotEquals(rows.getChildAt(1).getTag(), rows.getChildAt(2).getTag());
            assertTrue(rows.getChildAt(1).getTag() instanceof ChatMatrixBlock.CellKey);
            assertTrue(rows.getChildAt(1).getContentDescription().toString().contains("42"));
            assertTrue(rows.getChildAt(2).getContentDescription().toString().contains("43"));
            for (int dp : new int[]{240, 600}) {
                int width = ChatStructuredContent.dp(root.getContext(), dp);
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                root.layout(0, 0, width, root.getMeasuredHeight());
                assertTrue(rows.getWidth() > ((HorizontalScrollView) matrix.getChildAt(1)).getWidth() || dp == 600);
            }
        });
    }
    @Test public void hundredAndTwoHundredRowsHaveTenPerPageAndIndependentGraph() throws Exception {
        JsonArray sample = fixture();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (int count : new int[]{100, 200}) {
                JsonArray blocks = withRows(sample, count);
                LinearLayout root = render(blocks);
                LinearLayout matrix = (LinearLayout) root.getChildAt(2);
                LinearLayout grid = (LinearLayout) ((HorizontalScrollView) matrix.getChildAt(1)).getChildAt(0);
                LinearLayout controls = (LinearLayout) matrix.getChildAt(matrix.getChildCount() - 1);
                List<String> graph = texts(root.getChildAt(4));
                String payload = blocks.toString();
                assertEquals(11, grid.getChildCount());
                assertEquals("1 / " + count / 10, ((TextView) controls.getChildAt(1)).getText().toString());
                for (int page = 1; page < count / 10; page++) controls.getChildAt(2).performClick();
                assertEquals(11, grid.getChildCount()); assertFalse(controls.getChildAt(2).isEnabled());
                assertEquals(graph, texts(root.getChildAt(4))); assertEquals(payload, blocks.toString());
                assertEquals(String.valueOf(1000 + count - 10), ((TextView) ((LinearLayout) grid.getChildAt(1)).getChildAt(0)).getText().toString());
            }
        });
    }
    @Test public void unknownBlocksDoNotEraseValidComponentsOrExposeJson() throws Exception {
        JsonArray sample = fixture();
        sample.add(JsonParser.parseString("{\"tipo\":\"novo_tipo\",\"versao\":9,\"segredo\":\"nao-exibir\"}"));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            LinearLayout root = render(sample);
            assertEquals(7, root.getChildCount());
            List<String> content = texts(root);
            assertTrue(content.contains("Resumo dos dados"));
            assertTrue(content.contains(root.getContext().getString(R.string.chat_block_unavailable)));
            for (String text : content) assertFalse(text.contains("nao-exibir"));
        });
    }
    @Test public void emptyAndSizeCutContextsAndZeroDonutAreDistinct() throws Exception {
        JsonArray sample = fixture();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            JsonObject indicator = sample.get(1).getAsJsonObject().deepCopy();
            JsonObject percentage = indicator.getAsJsonArray("indicadores").get(1).getAsJsonObject();
            percentage.add("valor", JsonNull.INSTANCE); percentage.addProperty("numerador", 0); percentage.addProperty("denominador", 0);
            indicator.getAsJsonArray("indicadores").get(0).getAsJsonObject().addProperty("valor", 0);
            JsonObject context = sample.get(5).getAsJsonObject().deepCopy();
            JsonObject summary = context.getAsJsonObject("conjunto");
            summary.addProperty("retornada", 0); summary.addProperty("total", 0); summary.addProperty("tem_mais", false);
            JsonArray blocks = new JsonArray(); blocks.add(indicator); blocks.add(context);
            LinearLayout root = render(blocks);
            assertTrue(texts(root).contains("Sem base"));
            assertTrue(texts(root).contains(root.getContext().getString(R.string.chat_dataset_empty)));
            assertFalse(texts(root).contains("0%"));
            summary.addProperty("total", 273); summary.addProperty("tem_mais", true); summary.addProperty("limitado", true); summary.addProperty("motivo_limite", "tamanho_resposta");
            root = render(blocks);
            assertTrue(texts(root).contains(root.getContext().getString(R.string.chat_dataset_no_rows)));
            assertFalse(texts(root).contains(root.getContext().getString(R.string.chat_dataset_empty)));
            JsonObject chart = sample.get(4).getAsJsonObject().deepCopy();
            for (JsonElement point : chart.getAsJsonArray("dados")) point.getAsJsonObject().addProperty("valor", 0);
            blocks = new JsonArray(); blocks.add(chart); root = render(blocks);
            assertTrue(texts(root).contains(root.getContext().getString(R.string.chat_chart_no_values)));
            assertTrue(texts(root).contains("Vigentes: 0"));
        });
    }
    @Test public void donutHasTransparentCenterAndKeepsZeroCategory() throws Exception {
        JsonArray sample = fixture();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            JsonArray blocks = new JsonArray(); blocks.add(sample.get(4));
            LinearLayout root = render(blocks);
            LinearLayout card = (LinearLayout) root.getChildAt(0);
            View chart = card.getChildAt(1);
            int side = ChatStructuredContent.dp(root.getContext(), 240);
            chart.measure(View.MeasureSpec.makeMeasureSpec(side, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(side, View.MeasureSpec.EXACTLY));
            chart.layout(0, 0, side, side);
            Bitmap bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
            try {
                chart.draw(new android.graphics.Canvas(bitmap));
                assertEquals(0, android.graphics.Color.alpha(bitmap.getPixel(side / 2, side / 2)));
                assertNotEquals(0, android.graphics.Color.alpha(bitmap.getPixel(side / 2, side / 8)));
            } finally { bitmap.recycle(); }
            assertTrue(texts(root).contains("Sem base: 0"));
            assertNotNull(chart.getContentDescription());
            assertTrue(chart.isLongClickable());
        });
    }
}
