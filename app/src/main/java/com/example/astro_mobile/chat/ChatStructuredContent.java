package com.example.astro_mobile.chat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.noties.markwon.Markwon;
import java.util.ArrayList;
import java.util.List;

/** Somente dados validados; respostas desconhecidas usam o texto original. */
final class ChatStructuredContent {
    private static final int[] COLORS = {0xff6848b7, 0xff2b8b82, 0xffe09152, 0xff7771c5, 0xffad6794, 0xff4e93b8, 0xff96a649, 0xffb77a70, 0xff568790, 0xff72639f, 0xffca8560, 0xff828f6b};
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
                    for (JsonElement r : rows) for (JsonElement c : cols) { JsonElement value = r.getAsJsonObject().get(str(c.getAsJsonObject(),"chave")); if (value == null || (!value.isJsonNull() && !value.isJsonPrimitive())) return false; }
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
            LinearLayout card = new LinearLayout(context); card.setOrientation(LinearLayout.VERTICAL); int pad = dp(context,12); card.setPadding(pad,pad,pad,pad); root.addView(card);
            label(card,str(b,"titulo"), true);
            if (type.startsWith("grafico_")) {
                JsonArray data = arr(b,"dados",12); card.addView(new ChartView(context,type,data), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(context,130)));
                for (JsonElement p : data) { JsonObject row = p.getAsJsonObject(); label(card,str(row,"categoria") + ": " + str(row,"valor"), false); }
                label(card,str(b,"fonte") + " · " + str(b,"unidade"), false);
            } else if ("tabela".equals(type)) {
                HorizontalScrollView scroll = new HorizontalScrollView(context); LinearLayout table = new LinearLayout(context); table.setOrientation(LinearLayout.VERTICAL); scroll.addView(table); card.addView(scroll);
                JsonArray cols = arr(b,"colunas",10); LinearLayout header = row(context); table.addView(header);
                for (JsonElement c : cols) cell(header,str(c.getAsJsonObject(),"titulo"),true);
                for (JsonElement entry : arr(b,"linhas",50)) { LinearLayout line = row(context); table.addView(line); for (JsonElement c : cols) { JsonElement value = entry.getAsJsonObject().get(str(c.getAsJsonObject(),"chave")); cell(line,value.isJsonNull() ? "—" : value.getAsString(),false); } }
            } else {
                for (JsonElement entry : arr(b,"imagens",50)) { JsonObject item = entry.getAsJsonObject(); LinearLayout line = row(context); card.addView(line); ImageView image = new ImageView(context); line.addView(image,new LinearLayout.LayoutParams(dp(context,40),dp(context,40))); String name = str(item,"nome"); image.setContentDescription("Foto de " + name); String url = item.has("url") && !item.get("url").isJsonNull() ? item.get("url").getAsString() : null;
                    if (safeUrl(url)) Glide.with(image).load(url).placeholder(android.R.drawable.ic_menu_myplaces).error(android.R.drawable.ic_menu_myplaces).circleCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(image); else image.setImageResource(android.R.drawable.ic_menu_myplaces);
                    label(line,name,false);
                }
            }
        }
    }
    private static boolean safeUrl(String value) { try { Uri uri = Uri.parse(value); return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getHost().endsWith(".r2.cloudflarestorage.com") && uri.getUserInfo() == null; } catch (Exception e) { return false; } }
    private static int dp(Context c,int n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    private static LinearLayout row(Context c) { LinearLayout r = new LinearLayout(c); r.setOrientation(LinearLayout.HORIZONTAL); return r; }
    private static void cell(LinearLayout r,String value,boolean bold) { TextView t = label(r,value,bold); t.setWidth(dp(r.getContext(),120)); }
    private static TextView label(LinearLayout r,String value,boolean bold) { TextView t = new TextView(r.getContext()); t.setText(value); t.setTextColor(0xff302a43); t.setTextSize(bold ? 14 : 12); if (bold) t.setTypeface(null,1); t.setPadding(0,dp(r.getContext(),4),dp(r.getContext(),8),dp(r.getContext(),4)); r.addView(t); return t; }
    private static final class ChartView extends View {
        final String type; final List<Float> values = new ArrayList<>(); final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ChartView(Context c,String type,JsonArray points) { super(c); this.type=type; for(JsonElement e:points) values.add(e.getAsJsonObject().get("valor").getAsFloat()); setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) { super.onDraw(canvas); float w=getWidth(), h=getHeight(), max=1,total=0; for(float v:values) { max=Math.max(max,v); total+=v; }
            if ("grafico_setores".equals(type)) { float start=-90; for(int i=0;i<values.size();i++) { float sweep=total>0?360*values.get(i)/total:0; paint.setColor(COLORS[i]); canvas.drawArc((w-h)/2+8,8,(w+h)/2-8,h-8,start,sweep,true,paint); start+=sweep; } }
            else if ("grafico_barras".equals(type)) { float step=w/values.size(); for(int i=0;i<values.size();i++) { paint.setColor(COLORS[i]); canvas.drawRect(i*step+step*.2f,h-values.get(i)/max*(h-12),i*step+step*.8f,h,paint); } }
            else { paint.setColor(COLORS[0]); paint.setStrokeWidth(dp(getContext(),2)); for(int i=1;i<values.size();i++) canvas.drawLine((i-1)*w/Math.max(1,values.size()-1),h-values.get(i-1)/max*(h-12),i*w/Math.max(1,values.size()-1),h-values.get(i)/max*(h-12),paint); }
        }
    }
}
