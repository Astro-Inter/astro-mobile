package com.example.astro_mobile.chat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import androidx.core.content.res.ResourcesCompat;
import com.example.astro_mobile.R;
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

/** Somente dados validados; respostas desconhecidas usam o texto original. */
final class ChatStructuredContent {
    private static final int[] COLORS = {0xffe1c9ff, 0xff9de5d7, 0xffffc38f, 0xffbfbaff, 0xffffbbdb, 0xffa9dcff, 0xffd4e68e, 0xffffc6bd, 0xff9fdbdf, 0xffd6beff, 0xffffd1a5, 0xffc4dfaf};
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
                if (b.get("versao").getAsInt() != 1 || !bounded(str(b,"titulo"),200)) return false;
                if (type.startsWith("grafico_")) {
                    if (!(type.equals("grafico_barras") || type.equals("grafico_linhas") || type.equals("grafico_setores")) || !bounded(str(b,"fonte"),200) || !bounded(str(b,"unidade"),100)) return false;
                    for (JsonElement p : arr(b,"dados",12)) { JsonObject point = p.getAsJsonObject(); double v = point.get("valor").getAsDouble(); if (!bounded(str(point,"categoria"),100) || !Double.isFinite(v) || v < 0 || v > 1000000000) return false; }
                } else if ("tabela".equals(type)) {
                    JsonArray cols = arr(b,"colunas",10); JsonArray rows = arr(b,"linhas",50);
                    for (JsonElement c : cols) { JsonObject col = c.getAsJsonObject(); if (!str(col,"chave").matches("[a-z][a-z0-9_]{0,39}") || !bounded(str(col,"titulo"),100)) return false; }
                    for (JsonElement r : rows) for (JsonElement c : cols) { JsonElement value = r.getAsJsonObject().get(str(c.getAsJsonObject(),"chave")); if (value != null && value.isJsonObject()) { JsonObject photo = value.getAsJsonObject(); if (!"imagem".equals(str(photo,"tipo")) || !bounded(str(photo,"descricao"),280) || !photo.has("url") || (!photo.get("url").isJsonNull() && (!photo.get("url").isJsonPrimitive() || !photo.get("url").getAsJsonPrimitive().isString()))) return false; } else if (value != null && !value.isJsonNull() && !value.isJsonPrimitive()) return false; }
                } else if ("ficha_usuario".equals(type)) {
                    if (!bounded(str(b,"nome"),255) || !b.get("foto").isJsonObject()) return false;
                    JsonObject photo = b.getAsJsonObject("foto");
                    if (!"imagem".equals(str(photo,"tipo")) || !bounded(str(photo,"descricao"),280) || !photo.has("url") || (!photo.get("url").isJsonNull() && (!photo.get("url").isJsonPrimitive() || !photo.get("url").getAsJsonPrimitive().isString()))) return false;
                    for (String key : new String[]{"email","cargo","unidade","perfil","situacao"}) if (!b.has(key) || (!b.get(key).isJsonNull() && (!b.get(key).isJsonPrimitive() || !b.get(key).getAsJsonPrimitive().isString() || !bounded(str(b,key),255)))) return false;
                    if (!b.has("conformidade")) return false;
                    if (!b.get("conformidade").isJsonNull()) {
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
    static void render(LinearLayout root, Markwon md, String fallback, JsonElement raw) {
        JsonArray blocks = raw != null && raw.isJsonArray() ? raw.getAsJsonArray() : null;
        if (!valid(blocks)) { ChatMarkdownContent.render(root, md, fallback); return; }
        root.removeAllViews(); Context context = root.getContext();
        for (JsonElement element : blocks) {
            JsonObject b = element.getAsJsonObject(); String type = str(b,"tipo");
            if ("texto".equals(type)) { LinearLayout part = new LinearLayout(context); part.setOrientation(LinearLayout.VERTICAL); root.addView(part); ChatMarkdownContent.render(part, md, str(b,"conteudo")); continue; }
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (root.getChildCount() > 0) cardParams.topMargin = dp(context, 12);
            root.addView(card, cardParams);
            TextView title = label(card, str(b, "titulo"), true);
            title.setAccessibilityHeading(true);
            if (type.startsWith("grafico_")) {
                JsonArray data = arr(b, "dados", 12);
                card.addView(new ChartView(context, type, data), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 140)));
                int index = 0;
                for (JsonElement p : data) {
                    JsonObject point = p.getAsJsonObject();
                    LinearLayout legend = row(context);
                    legend.setGravity(Gravity.CENTER_VERTICAL);
                    View swatch = new View(context);
                    GradientDrawable dot = new GradientDrawable();
                    dot.setColor(COLORS["grafico_linhas".equals(type) ? 0 : index]);
                    dot.setShape(GradientDrawable.OVAL);
                    swatch.setBackground(dot);
                    swatch.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                    legend.addView(swatch, new LinearLayout.LayoutParams(dp(context, 10), dp(context, 10)));
                    TextView value = label(legend, str(point, "categoria") + ": " + str(point, "valor"), false);
                    value.setPadding(dp(context, 8), dp(context, 4), 0, dp(context, 4));
                    card.addView(legend);
                    index++;
                }
                label(card, str(b, "fonte") + " · " + str(b, "unidade"), false);
            } else if ("ficha_usuario".equals(type)) {
                imageCell(card, b.getAsJsonObject("foto"), false);
                for (String key : new String[]{"nome","email","cargo","unidade","perfil","situacao"}) if (!b.get(key).isJsonNull()) label(card, key + ": " + str(b,key), false);
                if (!b.get("conformidade").isJsonNull()) {
                    JsonObject c = b.getAsJsonObject("conformidade");
                    label(card, "Conformidade cadastral: " + (c.get("percentual").isJsonNull() ? "Sem NRs aplicáveis" : str(c,"percentual") + "%"), true);
                    label(card, str(c,"vigentes") + " de " + str(c,"total") + " NRs vigentes · " + str(c,"criterio"), false);
                    if (c.has("data_referencia") && !c.get("data_referencia").isJsonNull()) label(card, "Referência: " + str(c,"data_referencia"), false);
                }
            } else if ("tabela".equals(type)) {
                renderTable(card, b);
            } else {
                renderGallery(card, arr(b, "imagens", 50));
            }
        }
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
        for (JsonElement c : columns) cell(header, str(c.getAsJsonObject(), "titulo"), true);
        for (JsonElement entry : arr(block, "linhas", 50)) {
            LinearLayout line = row(context);
            table.addView(line);
            StringBuilder description = new StringBuilder();
            for (JsonElement c : columns) {
                JsonObject column = c.getAsJsonObject();
                JsonElement value = entry.getAsJsonObject().get(str(column, "chave"));
                String text;
                if (value != null && value.isJsonObject()) {
                    JsonObject photo = value.getAsJsonObject();
                    text = str(photo, "descricao");
                    imageCell(line, photo, true);
                } else {
                    text = value == null || value.isJsonNull() ? "—" : value.getAsString();
                    cell(line, text, false).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                }
                if (description.length() > 0) description.append(", ");
                description.append(str(column, "titulo")).append(": ").append(text);
            }
            line.setContentDescription(description.toString());
            line.setFocusable(true);
        }
        if (block.has("pagina") && block.has("total_paginas") && block.has("total")) {
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
    private static void imageCell(LinearLayout line, JsonObject photo, boolean tableCell) {
        Context context = line.getContext();
        FrameLayout frame = new FrameLayout(context);
        frame.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
        line.addView(frame, new LinearLayout.LayoutParams(tableCell ? dp(context, 160) : dp(context, 64), dp(context, 64)));
        TextView initials = new TextView(context);
        initials.setText(initials(str(photo, "descricao")));
        initials.setTextColor(context.getColor(R.color.chat_text));
        initials.setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable();
        background.setColor(context.getColor(R.color.chat_user));
        background.setShape(GradientDrawable.OVAL);
        initials.setBackground(background);
        frame.addView(initials, new FrameLayout.LayoutParams(dp(context, 40), dp(context, 40)));
        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setClipToOutline(true);
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(android.graphics.Color.TRANSPARENT);
        mask.setShape(GradientDrawable.OVAL);
        image.setBackground(mask);
        frame.addView(image, new FrameLayout.LayoutParams(dp(context, 40), dp(context, 40)));
        frame.setContentDescription(str(photo, "descricao"));
        if (tableCell) frame.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        String url = photo.get("url").isJsonNull() ? null : photo.get("url").getAsString();
        if (safeUrl(url)) loadAvatar(image, url);
    }
    static String initials(String name) {
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
    private static int dp(Context c,int n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
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
    private static TextView label(LinearLayout row, String value, boolean bold) {
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
        final String type; final List<Float> values = new ArrayList<>(); final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ChartView(Context c,String type,JsonArray points) { super(c); this.type=type; for(JsonElement e:points) values.add(e.getAsJsonObject().get("valor").getAsFloat()); setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) { super.onDraw(canvas); float w=getWidth(), h=getHeight(), max=1,total=0; for(float v:values) { max=Math.max(max,v); total+=v; }
            if ("grafico_setores".equals(type)) { float start=-90; for(int i=0;i<values.size();i++) { float sweep=total>0?360*values.get(i)/total:0; paint.setColor(COLORS[i]); canvas.drawArc((w-h)/2+8,8,(w+h)/2-8,h-8,start,sweep,true,paint); start+=sweep; } }
            else if ("grafico_barras".equals(type)) { float step=w/values.size(); for(int i=0;i<values.size();i++) { paint.setColor(COLORS[i]); canvas.drawRect(i*step+step*.2f,h-values.get(i)/max*(h-12),i*step+step*.8f,h,paint); } }
            else { paint.setColor(COLORS[0]); paint.setStrokeWidth(dp(getContext(),2)); for(int i=1;i<values.size();i++) canvas.drawLine((i-1)*w/Math.max(1,values.size()-1),h-6-values.get(i-1)/max*(h-18),i*w/Math.max(1,values.size()-1),h-6-values.get(i)/max*(h-18),paint); for(int i=0;i<values.size();i++) canvas.drawCircle(i*w/Math.max(1,values.size()-1),h-6-values.get(i)/max*(h-18),dp(getContext(),3),paint); }
        }
    }
}
