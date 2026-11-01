package com.example.astro_mobile.chat;

import android.content.Context;
import android.app.Dialog;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.ScrollView;
import android.view.Window;
import java.text.Normalizer;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import androidx.core.content.res.ResourcesCompat;
import com.example.astro_mobile.R;
import com.example.astro_mobile.data.ai.ChatAnalyticalContract;
import com.example.astro_mobile.data.ai.ChatMatrixBlock;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Paint;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.noties.markwon.Markwon;
import java.util.ArrayList;
import java.util.List;

/** Renderiza blocos válidos em ordem; um tipo desconhecido não oculta os demais. */
final class ChatStructuredContent {
    private static final int[] COLORS = {0xffc4a1ff, 0xff59c9bc, 0xffffbe69, 0xff79b9f2, 0xfff38cae, 0xffb7d778, 0xffe49666, 0xffa3a6ef, 0xff67c9da, 0xffd9a2d9, 0xffc6bf89, 0xffaab4c8};
    private static final ExecutorService AVATARS = Executors.newFixedThreadPool(3);
    private ChatStructuredContent() { }
    private static String str(JsonObject o, String key) { return o.get(key).getAsString(); }
    private static JsonArray arr(JsonObject o, String key, int limit) {
        JsonArray a = o.getAsJsonArray(key); if (a == null || a.isEmpty() || a.size() > limit) throw new IllegalArgumentException(); return a;
    }
    private static boolean bounded(String s, int max) { return s != null && !s.isEmpty() && s.length() <= max; }
    static boolean valid(JsonArray blocks) {
        if (blocks == null || blocks.isEmpty() || blocks.size() > 10) return false;
        try {
            for (JsonElement e : blocks) {
                JsonObject b = e.getAsJsonObject(); String type = str(b, "tipo");
                if ("texto".equals(type)) { if (!bounded(str(b,"conteudo"),6000)) return false; continue; }
                if ("indicadores".equals(type) || "matriz_conformidade".equals(type) || "contexto_dados".equals(type)) {
                    if (!ChatAnalyticalContract.valid(b)) return false;
                    continue;
                }
                int version = b.get("versao").getAsInt();
                if ((version != 1 && !("tabela".equals(type) && version == 2)) || !bounded(str(b,"titulo"),200)) return false;
                if (type.startsWith("grafico_")) {
                    if (!(type.equals("grafico_barras") || type.equals("grafico_linhas") || type.equals("grafico_setores") || type.equals("grafico_rosca")) || !bounded(str(b,"fonte"),200) || !java.util.Arrays.asList("participações", "pessoas", "pendências", "registros").contains(str(b,"unidade"))) return false;
                    for (JsonElement p : arr(b,"dados",12)) { JsonObject point = p.getAsJsonObject(); JsonElement number = point.get("valor"); if (number == null || !number.isJsonPrimitive() || !number.getAsJsonPrimitive().isNumber()) return false; double v = number.getAsDouble(); if (!bounded(str(point,"categoria"),100) || !Double.isFinite(v) || v < 0 || number.getAsBigDecimal().stripTrailingZeros().scale() > 0) return false; }
                } else if ("tabela".equals(type)) {
                    JsonArray cols = arr(b,"colunas",10); JsonArray rows = arr(b,"linhas",version == 2 ? 200 : 50);
                    if (version == 2 && !validLocalTable(b, rows)) return false;
                    java.util.Set<String> keys = new java.util.HashSet<>();
                    for (JsonElement c : cols) { JsonObject col = c.getAsJsonObject(); if (!str(col,"chave").matches("[a-z][a-z0-9_]{0,39}") || !bounded(str(col,"titulo"),100)) return false; }
                    for (JsonElement c : cols) if (!keys.add(str(c.getAsJsonObject(), "chave"))) return false;
                    if (version == 2) for (JsonElement row : rows) {
                        if (!row.getAsJsonObject().keySet().equals(keys)) return false;
                        for (java.util.Map.Entry<String, JsonElement> cell : row.getAsJsonObject().entrySet()) if (!ChatAnalyticalContract.validCell(cell.getValue())) return false;
                    }
                    for (JsonElement r : rows) for (JsonElement c : cols) { JsonElement value = r.getAsJsonObject().get(str(c.getAsJsonObject(),"chave")); if (value != null && value.isJsonObject()) { JsonObject photo = value.getAsJsonObject(); if (!"imagem".equals(str(photo,"tipo")) || !bounded(str(photo,"descricao"),280) || !photo.has("url") || (!photo.get("url").isJsonNull() && (!photo.get("url").isJsonPrimitive() || !photo.get("url").getAsJsonPrimitive().isString()))) return false; } else if (value != null && !value.isJsonNull() && !value.isJsonPrimitive()) return false; }
                } else if ("ficha_usuario".equals(type)) {
                    if (!optionalText(b, "nome") || !b.get("foto").isJsonObject()) return false;
                    JsonObject photo = b.getAsJsonObject("foto");
                    if (!"imagem".equals(str(photo,"tipo")) || !bounded(str(photo,"descricao"),280) || !photo.has("url") || (!photo.get("url").isJsonNull() && (!photo.get("url").isJsonPrimitive() || !photo.get("url").getAsJsonPrimitive().isString()))) return false;
                    for (String key : new String[]{"email","cargo","unidade","perfil","situacao","modalidade"}) if (!optionalText(b, key)) return false;
                    if (b.has("conformidade") && !b.get("conformidade").isJsonNull()) {
                        JsonObject c = b.getAsJsonObject("conformidade");
                        double total = c.get("total").getAsDouble(), valid = c.get("vigentes").getAsDouble();
                        if (!Double.isFinite(total) || !Double.isFinite(valid) || total < 0 || valid < 0 || valid > total || total != Math.floor(total) || valid != Math.floor(valid) || !bounded(str(c,"criterio"),200)) return false;
                        if (total == 0 ? !c.get("percentual").isJsonNull() : c.get("percentual").isJsonNull() || c.get("percentual").getAsDouble() != Math.round(10000 * valid / total) / 100.0) return false;
                    }
                } else if ("galeria_usuarios".equals(type)) {
                    for (JsonElement image : arr(b,"imagens",50)) { JsonObject img = image.getAsJsonObject(); if (!bounded(str(img,"nome"),255) || (img.has("url") && !img.get("url").isJsonNull() && !img.get("url").isJsonPrimitive())) return false; }
                } else return false;
            }
            return true;
        } catch (RuntimeException error) { return false; }
    }
    static boolean validBlock(JsonElement block) {
        JsonArray single = new JsonArray(); single.add(block);
        return valid(single);
    }
    static void render(LinearLayout root, Markwon md, String fallback, JsonElement raw) {
        render(root, md, fallback, raw, false);
    }
    private static void render(LinearLayout root, Markwon md, String fallback, JsonElement raw, boolean expanded) {
        JsonArray blocks = raw != null && raw.isJsonArray() ? raw.getAsJsonArray() : null;
        boolean supported = false;
        if (blocks != null && blocks.size() <= 10) for (JsonElement block : blocks) supported |= validBlock(block);
        if (!supported) {
            ChatMarkdownContent.render(root, md, fallback == null ? "" : fallback);
            if (root.getChildCount() == 0) label(root, root.getContext().getString(R.string.chat_block_unavailable), false);
            return;
        }
        root.removeAllViews(); Context context = root.getContext();
        for (JsonElement element : blocks) {
            if (!validBlock(element)) {
                label(root, context.getString(R.string.chat_block_unavailable), false);
                continue;
            }
            JsonObject b = element.getAsJsonObject(); String type = str(b,"tipo");
            if ("texto".equals(type)) { LinearLayout part = new LinearLayout(context); part.setOrientation(LinearLayout.VERTICAL); root.addView(part); ChatMarkdownContent.render(part, md, str(b,"conteudo")); continue; }
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (root.getChildCount() > 0) cardParams.topMargin = dp(context, 12);
            root.addView(card, cardParams);
            if (!"contexto_dados".equals(type)) {
                TextView title = label(card, str(b, "titulo"), true);
                title.setAccessibilityHeading(true);
                if (expanded) {
                title.setTextSize(20);
                title.setPadding(0, 0, 0, dp(context, 12));
                }
            }
            if ("indicadores".equals(type)) {
                ChatAnalyticalContent.indicators(card, ChatAnalyticalContract.indicators(b));
            } else if ("contexto_dados".equals(type)) {
                ChatAnalyticalContent.context(card, ChatAnalyticalContract.context(b));
            } else if (type.startsWith("grafico_")) {
                JsonArray data = arr(b, "dados", 12);
                ChartView chart = new ChartView(context, type, data);
                boolean radial = "grafico_setores".equals(type) || "grafico_rosca".equals(type);
                boolean hasValues = false;
                for (JsonElement point : data) hasValues |= point.getAsJsonObject().get("valor").getAsDouble() > 0;
                if (!radial && data.size() > 6) {
                    HorizontalScrollView viewport = new HorizontalScrollView(context);
                    viewport.setHorizontalScrollBarEnabled(true);
                    viewport.addView(chart, new HorizontalScrollView.LayoutParams(dp(context, data.size() * 56 + 72), dp(context, expanded ? 260 : 190)));
                    card.addView(viewport);
                } else card.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, radial && !hasValues ? 0 : expanded ? 260 : 190)));
                if (radial && !hasValues) label(card, context.getString(R.string.chat_chart_no_values), false);
                int index = 0;
                for (JsonElement p : data) {
                    JsonObject point = p.getAsJsonObject();
                    LinearLayout legend = row(context);
                    legend.setGravity(Gravity.CENTER_VERTICAL);
                    View swatch = new View(context);
                    GradientDrawable dot = new GradientDrawable();
                    dot.setColor(chartColor(type, str(point, "categoria"), index));
                    dot.setShape(GradientDrawable.OVAL);
                    swatch.setBackground(dot);
                    swatch.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                    legend.addView(swatch, new LinearLayout.LayoutParams(dp(context, 10), dp(context, 10)));
                    String detail = (radial ? "" : (index + 1) + ". ") + ChatDisplayLabels.format("categoria", str(point, "categoria")) + ": " + str(point, "valor");
                    TextView value = label(legend, detail, false);
                    value.setPadding(dp(context, 8), dp(context, 4), 0, dp(context, 4));
                    card.addView(legend);
                    index++;
                }
                label(card, str(b, "fonte") + " · " + str(b, "unidade"), false);
                if (!expanded) {
                    chart.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                    chart.setContentDescription(context.getString(R.string.chat_chart_expand_named, str(b, "titulo")));
                    chart.setOnLongClickListener(view -> {
                        showChartDetails(context, md, b);
                        return true;
                    });
                    androidx.core.view.ViewCompat.replaceAccessibilityAction(chart,
                            androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
                            context.getString(R.string.chat_chart_expand), (view, arguments) -> chart.performLongClick());

                }
            } else if ("ficha_usuario".equals(type)) {
                renderProfile(card, b);
                if (b.has("conformidade") && !b.get("conformidade").isJsonNull()) {
                    JsonObject c = b.getAsJsonObject("conformidade");
                    label(card, "Conformidade cadastral: " + (c.get("percentual").isJsonNull() ? "Sem NRs aplicáveis" : str(c,"percentual") + "%"), true);
                    label(card, str(c,"vigentes") + " de " + str(c,"total") + " NRs vigentes · " + str(c,"criterio"), false);
                    if (c.has("data_referencia") && !c.get("data_referencia").isJsonNull()) label(card, "Referência: " + str(c,"data_referencia"), false);
                }
            } else if ("tabela".equals(type) || "matriz_conformidade".equals(type)) {
                renderTable(card, b);
            } else {
                renderGallery(card, arr(b, "imagens", 50));
            }
        }
    }
    private static void showChartDetails(Context context, Markwon markdown, JsonObject block) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable panel = new GradientDrawable();
        panel.setColor(context.getColor(R.color.chat_surface));
        panel.setCornerRadius(dp(context, 24));
        page.setBackground(panel);
        page.setClipToOutline(true);
        int padding = dp(context, 20);
        page.setPadding(padding, dp(context, 12), padding, padding);
        LinearLayout header = row(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = label(header, context.getString(R.string.chat_chart_details), true);
        heading.setTextColor(context.getColor(R.color.chat_muted));
        heading.setTextSize(13);
        heading.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        ImageButton close = chartIcon(context, R.drawable.chat_chart_cross,
                context.getString(R.string.chat_chart_close));
        close.setOnClickListener(view -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(context, 48), dp(context, 48)));
        page.addView(header);
        ScrollView scroll = new ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        JsonArray blocks = new JsonArray();
        blocks.add(block.deepCopy());
        render(content, markdown, "", blocks, true);
        dialog.setContentView(page);
        dialog.show();
        if (dialog.getWindow() != null) {
            int width = Math.min(context.getResources().getDisplayMetrics().widthPixels - dp(context, 24), dp(context, 640));
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(.72f);
            dialog.getWindow().setLayout(width, (int) (context.getResources().getDisplayMetrics().heightPixels * .88f));
        }
    }
    private static ImageButton chartIcon(Context context, int icon, String description) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(icon);
        button.setContentDescription(description);
        button.setTooltipText(description);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setBackgroundResource(R.drawable.bg_home_icon_ripple);
        return button;
    }
    static int chartColor(String type, String category, int index) {
        if ("grafico_linhas".equals(type)) return COLORS[0];
        String key = Normalizer.normalize(category, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (key.equals("vencidas")) return 0xffff8c97;
        if (key.equals("hoje a 7 dias")) return 0xffffb86b;
        if (key.equals("8 a 30 dias")) return 0xfff2d577;
        if (key.equals("mais de 30 dias")) return 0xff64d3bb;
        if (key.equals("sem validade")) return 0xffaab4c8;
        return COLORS[index % COLORS.length];
    }
    private static void renderTable(LinearLayout card, JsonObject block) {
        Context context = card.getContext();
        JsonArray columns = arr(block, "colunas", 10);
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setFillViewport(true);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout table = new LinearLayout(context);
        table.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable tableBackground = new GradientDrawable();
        tableBackground.setColor(0x44362361);
        tableBackground.setCornerRadius(dp(context, 12));
        table.setBackground(tableBackground);
        table.setClipToOutline(true);
        GradientDrawable divider = new GradientDrawable();
        divider.setColor(0x33ffffff);
        divider.setSize(1, Math.max(1, dp(context, 1)));
        table.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        table.setDividerDrawable(divider);
        scroll.addView(table);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollParams.topMargin = dp(context, 8);
        card.addView(scroll, scrollParams);
        LinearLayout header = row(context);
        header.setBackgroundColor(0x66362361);
        header.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        table.addView(header);
        JsonArray rows = block.getAsJsonArray("linhas");
        for (JsonElement c : columns) {
            JsonObject column = c.getAsJsonObject();
            TextView heading = cell(header, str(column, "titulo"), true);
            if (imageColumn(column, rows)) {
                heading.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 72), ViewGroup.LayoutParams.MATCH_PARENT));
                heading.setGravity(Gravity.CENTER);
            }
        }
        if (block.get("versao").getAsInt() == 2 || "matriz_conformidade".equals(str(block, "tipo"))) {
            renderLocalPages(card, table, columns, rows, block);
        } else {
            appendRows(table, columns, rows, block, 0, rows.size());
        }
        if ("tabela".equals(str(block, "tipo")) && block.get("versao").getAsInt() == 1 && block.has("pagina") && block.has("total_paginas") && block.has("total")) {
            try {
                int page = block.get("pagina").getAsInt();
                int pages = block.get("total_paginas").getAsInt();
                int total = block.get("total").getAsInt();
                if (page > 0 && pages >= page && total >= 0) {
                    TextView pagination = label(card, context.getString(R.string.chat_table_pagination, page, pages, total), false);
                    pagination.setTextColor(context.getColor(R.color.chat_muted));
                    pagination.setTextSize(12);
                    pagination.setPadding(dp(context, 2), dp(context, 8), 0, 0);
                }
            } catch (RuntimeException ignored) { /* Metadados opcionais inválidos não ocultam a tabela. */ }
        }
    }
    private static boolean imageColumn(JsonObject column, JsonArray rows) {
        String key = str(column, "chave");
        boolean photo = "foto".equals(key);
        for (JsonElement row : rows) {
            JsonElement value = row.getAsJsonObject().get(key);
            if (value == null || value.isJsonNull()) continue;
            if (!value.isJsonObject() || !"imagem".equals(optional(value.getAsJsonObject(), "tipo"))) return false;
            photo = true;
        }
        return photo;
    }
    private static void appendRows(LinearLayout table, JsonArray columns, JsonArray rows, JsonObject block, int start, int end) {
        Context context = table.getContext();
        for (int index = start; index < end; index++) {
            JsonElement entry = rows.get(index);
            LinearLayout line = row(context);
            line.setGravity(Gravity.CENTER_VERTICAL);
            if ("matriz_conformidade".equals(str(block, "tipo"))) {
                JsonObject matrixRow = entry.getAsJsonObject();
                line.setTag(new ChatMatrixBlock.CellKey(matrixRow.get("referencia_pessoa").getAsBigInteger(), matrixRow.get("nr").getAsInt()));
            }
            table.addView(line);
            StringBuilder description = new StringBuilder();
            for (JsonElement c : columns) {
                JsonObject column = c.getAsJsonObject();
                JsonElement value = entry.getAsJsonObject().get(str(column, "chave"));
                String text;
                if (value != null && value.isJsonObject()) {
                    JsonObject photo = value.getAsJsonObject();
                    text = str(photo, "descricao");
                    FrameLayout avatar = imageCell(line, photo, true, optional(entry.getAsJsonObject(), "nome"));
                    if (!imageColumn(column, rows)) avatar.getLayoutParams().width = dp(context, 160);
                } else {
                    text = value == null || value.isJsonNull() ? "—" : ChatDisplayLabels.format(str(column, "chave"), value.getAsString());
                    TextView field = cell(line, text, false);
                    if (imageColumn(column, rows)) {
                        field.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 72), ViewGroup.LayoutParams.MATCH_PARENT));
                        field.setGravity(Gravity.CENTER);
                    }
                    field.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                }
                if (description.length() > 0) description.append(", ");
                description.append(str(column, "titulo")).append(": ").append(text);
            }
            line.setContentDescription(description.toString());
            line.setFocusable(true);
        }
    }
    private static void renderLocalPages(LinearLayout card, LinearLayout table, JsonArray columns, JsonArray rows, JsonObject block) {
        Context context = card.getContext();
        label(card, context.getString(R.string.chat_table_received, rows.size(), block.get("total").getAsString()), false);
        label(card, context.getString(R.string.chat_table_requested, block.get("solicitada").getAsString()), false);
        if (block.get("limitado").getAsBoolean()) {
            label(card, context.getString("tamanho_resposta".equals(optional(block, "motivo_limite"))
                    ? R.string.chat_table_size_limit : R.string.chat_table_count_limit), false);
        } else if (block.get("tem_mais").getAsBoolean()) label(card, context.getString(R.string.chat_table_partial), false);
        LinearLayout controls = row(context);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(controls);
        Button previous = new Button(context);
        previous.setText(R.string.chat_table_previous);
        controls.addView(previous);
        TextView position = label(controls, "", false);
        position.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        position.setGravity(Gravity.CENTER);
        position.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        Button next = new Button(context);
        next.setText(R.string.chat_table_next);
        controls.addView(next);
        for (Button button : new Button[]{previous, next}) {
            button.setTextColor(context.getColor(R.color.chat_link));
            button.setTextSize(12);
            button.setAllCaps(false);
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setBackgroundResource(R.drawable.bg_home_icon_ripple);
        }
        int[] page = {0};
        int pages = (rows.size() + 9) / 10;
        Runnable update = () -> {
            if (table.getChildCount() > 1) table.removeViews(1, table.getChildCount() - 1);
            appendRows(table, columns, rows, block, page[0] * 10, Math.min(rows.size(), (page[0] + 1) * 10));
            position.setText(context.getString(R.string.chat_table_local_page, page[0] + 1, pages));
            previous.setEnabled(page[0] > 0);
            next.setEnabled(page[0] + 1 < pages);
        };
        previous.setOnClickListener(view -> { if (page[0] > 0) { page[0]--; update.run(); } });
        next.setOnClickListener(view -> { if (page[0] + 1 < pages) { page[0]++; update.run(); } });
        update.run();
    }
    private static boolean optionalText(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() || (value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isString() && value.getAsString().length() <= 255);
    }
    private static String optional(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }
    private static boolean validLocalTable(JsonObject block, JsonArray rows) {
        return "local".equals(str(block, "paginacao")) && ChatAnalyticalContract.validDataset(block, rows.size())
                && (!block.has("pagina") || block.get("pagina").getAsInt() == 1)
                && (!block.has("total_paginas") || block.get("total_paginas").getAsInt() == 1);
    }
    private static void renderProfile(LinearLayout card, JsonObject block) {
        Context context = card.getContext();
        LinearLayout identity = row(context);
        identity.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(identity);
        imageCell(identity, block.getAsJsonObject("foto"), false, optional(block, "nome"));
        String name = optional(block, "nome");
        if (name != null && !name.trim().isEmpty()) {
            TextView title = label(identity, name, true);
            title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            title.setTextSize(18);
        }
        String[] keys = {"email", "perfil", "cargo", "unidade", "modalidade", "situacao"};
        int[] labels = {R.string.chat_profile_email, R.string.chat_profile_role, R.string.chat_profile_job,
                R.string.chat_profile_unit, R.string.chat_profile_mode, R.string.chat_profile_state};
        for (int index = 0; index < keys.length; index++) {
            if ("usuario_autenticado".equals(optional(block, "origem")) && "situacao".equals(keys[index])) continue;
            String value = optional(block, keys[index]);
            if (value == null || value.trim().isEmpty()) continue;
            LinearLayout field = row(context);
            field.setPadding(0, dp(context, 6), 0, dp(context, 6));
            card.addView(field);
            TextView caption = label(field, context.getString(labels[index]), false);
            caption.setTextSize(12);
            caption.setTextColor(context.getColor(R.color.chat_muted));
            caption.setLayoutParams(new LinearLayout.LayoutParams(0, -2, .38f));
            TextView detail = label(field, ChatDisplayLabels.format(keys[index], value), false);
            detail.setLayoutParams(new LinearLayout.LayoutParams(0, -2, .62f));

        }
    }
    private static void renderGallery(LinearLayout card, JsonArray images) {
        Context context = card.getContext();
        for (JsonElement entry : images) {
            JsonObject item = entry.getAsJsonObject();
            String name = str(item, "nome");
            LinearLayout line = row(context);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(0, dp(context, 6), 0, dp(context, 6));
            card.addView(line);
            FrameLayout avatar = new FrameLayout(context);
            line.addView(avatar, new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));
            TextView initials = new TextView(context);
            initials.setText(initials(name));
            initials.setTextColor(context.getColor(R.color.chat_text));
            initials.setGravity(Gravity.CENTER);
            initials.setTypeface(null, Typeface.BOLD);
            GradientDrawable background = new GradientDrawable();
            background.setColor(context.getColor(R.color.chat_user));
            background.setShape(GradientDrawable.OVAL);
            initials.setBackground(background);
            avatar.addView(initials, new FrameLayout.LayoutParams(-1, -1));
            ImageView image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setClipToOutline(true);
            GradientDrawable mask = new GradientDrawable();
            mask.setColor(android.graphics.Color.TRANSPARENT);
            mask.setShape(GradientDrawable.OVAL);
            image.setBackground(mask);
            avatar.addView(image, new FrameLayout.LayoutParams(-1, -1));
            avatar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            TextView nameView = label(line, name, false);
            nameView.setPadding(dp(context, 12), dp(context, 4), 0, dp(context, 4));
            nameView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            String url = item.has("url") && !item.get("url").isJsonNull() ? item.get("url").getAsString() : null;
            if (safeUrl(url)) loadAvatar(image, url);
        }
    }
    private static FrameLayout imageCell(LinearLayout line, JsonObject photo, boolean tableCell, String name) {
        Context context = line.getContext();
        FrameLayout frame = new FrameLayout(context);
        frame.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
        line.addView(frame, new LinearLayout.LayoutParams(tableCell ? dp(context, 72) : dp(context, 64), dp(context, 64)));
        TextView initials = new TextView(context);
        initials.setText(initials(name));
        initials.setTextColor(context.getColor(R.color.chat_text));
        initials.setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable();
        background.setColor(context.getColor(R.color.chat_user));
        background.setShape(GradientDrawable.OVAL);
        initials.setBackground(background);
        frame.addView(initials, new FrameLayout.LayoutParams(dp(context, 40), dp(context, 40), Gravity.CENTER));
        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setClipToOutline(true);
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(android.graphics.Color.TRANSPARENT);
        mask.setShape(GradientDrawable.OVAL);
        image.setBackground(mask);
        frame.addView(image, new FrameLayout.LayoutParams(dp(context, 40), dp(context, 40), Gravity.CENTER));
        frame.setContentDescription(str(photo, "descricao"));
        if (tableCell) frame.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        JsonElement receivedUrl = photo.get("url");
        String url = receivedUrl == null || receivedUrl.isJsonNull() ? null : receivedUrl.getAsString();
        if (safeUrl(url)) loadAvatar(image, url);
        return frame;
    }
    static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "●";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "?";
        String first = parts[0].substring(0, parts[0].offsetByCodePoints(0, 1));
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, parts[parts.length - 1].offsetByCodePoints(0, 1)) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }
    private static void loadAvatar(ImageView image, String value) {
        // A URL assinada nunca é registrada nem armazenada em cache.
        AVATARS.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(value).openConnection();
                connection.setUseCaches(false);
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                if (connection.getResponseCode() != 200) return;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream input = connection.getInputStream()) {
                    byte[] buffer = new byte[8192]; int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (bytes.size() + count > 2 * 1024 * 1024) return;
                        bytes.write(buffer, 0, count);
                    }
                }
                byte[] data = bytes.toByteArray();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
                if (options.outWidth <= 0 || options.outHeight <= 0) return;
                options.inJustDecodeBounds = false;
                options.inSampleSize = 1;
                while (options.outWidth / options.inSampleSize > 256 || options.outHeight / options.inSampleSize > 256)
                    options.inSampleSize = Math.max(1, options.inSampleSize) * 2;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                if (bitmap != null) image.post(() -> image.setImageBitmap(bitmap));
            } catch (Exception ignored) { /* Mantém as iniciais também em caso de acesso negado. */ }
            finally { if (connection != null) connection.disconnect(); }
        });
    }
    private static boolean safeUrl(String value) { try { Uri uri = Uri.parse(value); return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getHost().endsWith(".r2.cloudflarestorage.com") && uri.getUserInfo() == null; } catch (Exception e) { return false; } }
    static int dp(Context c,int n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    private static LinearLayout row(Context c) { LinearLayout r = new LinearLayout(c); r.setOrientation(LinearLayout.HORIZONTAL); return r; }
    private static TextView cell(LinearLayout row, String value, boolean bold) {
        Context context = row.getContext();
        TextView text = label(row, value, bold);
        text.setPadding(dp(context, 12), dp(context, 11), dp(context, 12), dp(context, 11));
        text.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 160), ViewGroup.LayoutParams.MATCH_PARENT));
        text.setTextSize(bold ? 13 : 14);
        if (bold) text.setTextColor(context.getColor(R.color.chat_link));
        return text;
    }
    static TextView label(LinearLayout row, String value, boolean bold) {
        Context context = row.getContext();
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextColor(context.getColor(R.color.chat_text));
        text.setTextSize(bold ? 15 : 14);
        text.setTypeface(ResourcesCompat.getFont(context, R.font.montserrat_family), bold ? Typeface.BOLD : Typeface.NORMAL);
        text.setPadding(0, dp(context, 4), dp(context, 8), dp(context, 4));
        row.addView(text);
        return text;
    }
    private static final class ChartView extends View {
        final List<String> categories = new ArrayList<>();
        final String type; final List<Double> values = new ArrayList<>(); final List<String> exactValues = new ArrayList<>(); final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ChartView(Context c,String type,JsonArray points) { super(c); this.type=type; for(JsonElement e:points) { values.add(e.getAsJsonObject().get("valor").getAsDouble()); exactValues.add(str(e.getAsJsonObject(), "valor")); categories.add(str(e.getAsJsonObject(), "categoria")); } setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w=getWidth(), h=getHeight(); double max=1, total=0;
            for(double v:values) max=Math.max(max,v);
            for(double v:values) total+=v/max;
            if ("grafico_setores".equals(type) || "grafico_rosca".equals(type)) {
                float side=Math.min(w,h)-dp(getContext(),16), left=(w-side)/2, top=(h-side)/2, start=-90;
                boolean ring = "grafico_rosca".equals(type);
                float stroke = side * .22f;
                paint.setStyle(ring ? Paint.Style.STROKE : Paint.Style.FILL);
                paint.setStrokeWidth(stroke);
                float inset = ring ? stroke / 2 : 0;
                for(int i=0;i<values.size();i++) {
                    float sweep=total>0?(float)(360*(values.get(i)/max)/total):0;
                    paint.setColor(chartColor(type,categories.get(i),i));
                    canvas.drawArc(left+inset,top+inset,left+side-inset,top+side-inset,start,sweep,!ring,paint); start+=sweep;
                }
                paint.setStyle(Paint.Style.FILL);
                return;
            }
            paint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
            float left=Math.max(dp(getContext(),36),paint.measureText(String.format(Locale.ROOT,"%.0f",max))+dp(getContext(),8));
            float right=w-dp(getContext(),12), top=dp(getContext(),24), bottom=h-dp(getContext(),26);
            float height=Math.max(1,bottom-top), width=Math.max(1,right-left);
            for(int tick=0;tick<=2;tick++) {
                float y=bottom-height*tick/2;
                paint.setStrokeWidth(dp(getContext(),1)); paint.setColor(0x33ffffff);
                canvas.drawLine(left,y,right,y,paint);
                paint.setColor(getContext().getColor(R.color.chat_muted)); paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(java.math.BigDecimal.valueOf(max*tick/2d).stripTrailingZeros().toPlainString().replace('.', ','),left-dp(getContext(),6),y+dp(getContext(),3),paint);
            }
            float step=width/values.size();
            for(int i=0;i<values.size();i++) {
                float x=left+step*(i+.5f), y=bottom-(float)(values.get(i)/max*height);
                paint.setColor(chartColor(type,categories.get(i),i));
                if ("grafico_barras".equals(type)) canvas.drawRect(x-step*.28f,y,x+step*.28f,bottom,paint);
                else {
                    paint.setStrokeWidth(dp(getContext(),2));
                    if(i>0)canvas.drawLine(left+step*(i-.5f),bottom-(float)(values.get(i-1)/max*height),x,y,paint);
                    canvas.drawCircle(x,y,dp(getContext(),3),paint);
                }
                paint.setTextAlign(Paint.Align.CENTER); paint.setColor(getContext().getColor(R.color.chat_text));
                canvas.drawText(exactValues.get(i),x,Math.max(paint.getTextSize(),y-dp(getContext(),6)),paint);
                canvas.drawText(String.valueOf(i+1),x,bottom+dp(getContext(),18),paint);
            }
        }
    }
}
