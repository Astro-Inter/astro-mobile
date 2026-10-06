package com.example.astro_mobile.chat;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Layout;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.example.astro_mobile.R;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Document;
import org.commonmark.node.Node;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.Table;

import java.util.ArrayList;
import java.util.List;

/** Divide blocos do parser Markdown, preservando a posição das tabelas na resposta. */
final class ChatMarkdownContent {
    private ChatMarkdownContent() { }

    static void render(LinearLayout container, Markwon markdown, String source) {
        container.removeAllViews();
        Node document = markdown.parse(source);
        Document text = new Document();
        for (Node node = document.getFirstChild(); node != null; ) {
            Node next = node.getNext();
            if (node instanceof TableBlock) {
                appendText(container, markdown, text);
                text = new Document();
                appendTable(container, markdown, (TableBlock) node);
            } else {
                // O AST distingue tabelas de pipes em código, links e texto comum.
                node.unlink();
                text.appendChild(node);
            }
            node = next;
        }
        appendText(container, markdown, text);
    }

    private static void appendText(LinearLayout container, Markwon markdown, Document document) {
        if (document.getFirstChild() == null) return;
        TextView view = textView(container.getContext());
        markdown.setParsedMarkdown(view, markdown.render(document));
        appendBlock(container, view);
    }

    private static void appendTable(LinearLayout container, Markwon markdown, TableBlock block) {
        Context context = container.getContext();
        Table table = parseTable(markdown, block);
        int columns = 0;
        for (Table.Row row : table.rows()) columns = Math.max(columns, row.columns().size());
        int[] widths = new int[columns];
        TextView measure = textView(context);
        for (Table.Row row : table.rows()) {
            measure.setTypeface(measure.getTypeface(), row.header() ? Typeface.BOLD : Typeface.NORMAL);
            for (int index = 0; index < row.columns().size(); index++) {
                float desired = Layout.getDesiredWidth(row.columns().get(index).content(), measure.getPaint());
                widths[index] = Math.max(widths[index], Math.min(dp(context, 224),
                        Math.max(dp(context, 104), (int) Math.ceil(desired) + dp(context, 24))));
            }
        }
        TableLayout grid = new TableLayout(context);
        grid.setStretchAllColumns(true);
        for (Table.Row row : table.rows()) {
            TableRow nativeRow = new TableRow(context);
            for (int index = 0; index < columns; index++) {
                TextView cell = textView(context);
                cell.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
                cell.setTypeface(cell.getTypeface(), row.header() ? Typeface.BOLD : Typeface.NORMAL);
                GradientDrawable background = new GradientDrawable();
                background.setColor(context.getColor(row.header() ? R.color.chat_code : R.color.chat_assistant));
                background.setStroke(dp(context, 1), context.getColor(R.color.chat_outline));
                cell.setBackground(background);
                if (index < row.columns().size()) {
                    Table.Column column = row.columns().get(index);
                    int alignment = column.alignment() == Table.Alignment.CENTER ? Gravity.CENTER_HORIZONTAL
                            : column.alignment() == Table.Alignment.RIGHT ? Gravity.RIGHT : Gravity.LEFT;
                    cell.setGravity(Gravity.TOP | alignment);
                    markdown.setParsedMarkdown(cell, column.content());
                }
                nativeRow.addView(cell, new TableRow.LayoutParams(widths[index], ViewGroup.LayoutParams.MATCH_PARENT));
            }
            grid.addView(nativeRow, new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setFillViewport(true);
        scroll.setHorizontalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(false);
        scroll.addView(grid, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        appendBlock(container, scroll);
    }

    private static Table parseTable(Markwon markdown, TableBlock block) {
        List<Table.Row> rows = new ArrayList<>();
        block.accept(new AbstractVisitor() {
            private List<Table.Column> cells = new ArrayList<>();
            private boolean header;

            @Override public void visit(CustomNode node) {
                if (node instanceof TableCell) {
                    TableCell cell = (TableCell) node;
                    Document inline = new Document();
                    // O TablePlugin desenha células dentro de um único span. Renderizamos
                    // seus filhos diretamente para manter texto e spans em views nativas.
                    while (cell.getFirstChild() != null) inline.appendChild(cell.getFirstChild());
                    Table.Alignment alignment = cell.getAlignment() == TableCell.Alignment.RIGHT
                            ? Table.Alignment.RIGHT : cell.getAlignment() == TableCell.Alignment.CENTER
                            ? Table.Alignment.CENTER : Table.Alignment.LEFT;
                    cells.add(new Table.Column(alignment, markdown.render(inline)));
                    header = cell.isHeader();
                } else {
                    visitChildren(node);
                    if ((node instanceof TableHead || node instanceof org.commonmark.ext.gfm.tables.TableRow)
                            && !cells.isEmpty()) {
                        rows.add(new Table.Row(header, cells));
                        cells = new ArrayList<>();
                    }
                }
            }
        });
        return new Table(rows);
    }

    private static TextView textView(Context context) {
        TextView view = new TextView(context);
        view.setTypeface(ResourcesCompat.getFont(context, R.font.montserrat_family));
        view.setTextSize(17);
        view.setTextColor(context.getColor(R.color.chat_text));
        view.setLinkTextColor(context.getColor(R.color.chat_link));
        view.setIncludeFontPadding(false);
        view.setTextIsSelectable(true);
        return view;
    }

    private static void appendBlock(LinearLayout container, android.view.View view) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        if (container.getChildCount() > 0) params.topMargin = dp(container.getContext(), 12);
        container.addView(view, params);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
