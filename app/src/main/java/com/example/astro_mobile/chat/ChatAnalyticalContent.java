package com.example.astro_mobile.chat;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.astro_mobile.R;
import com.example.astro_mobile.data.ai.ChatContextBlock;
import com.example.astro_mobile.data.ai.ChatDatasetSummary;
import com.example.astro_mobile.data.ai.ChatIndicatorsBlock;
import com.google.gson.JsonElement;
import java.util.Map;
import static com.example.astro_mobile.chat.ChatStructuredContent.dp;
import static com.example.astro_mobile.chat.ChatStructuredContent.label;

/** Apresentação dos modelos analíticos; não calcula totais nem solicita páginas. */
final class ChatAnalyticalContent {
    private ChatAnalyticalContent() { }

    static void indicators(LinearLayout card, ChatIndicatorsBlock block) {
        Context context = card.getContext();
        muted(card, block.getUniverse());
        for (ChatIndicatorsBlock.Indicator item : block.getIndicators()) {
            LinearLayout tile = new LinearLayout(context);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
            GradientDrawable background = new GradientDrawable();
            background.setColor(0x44362361);
            background.setCornerRadius(dp(context, 12));
            background.setStroke(dp(context, 1), 0x22ffffff);
            tile.setBackground(background);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.topMargin = dp(context, 8);
            card.addView(tile, params);
            label(tile, item.getLabel(), true);
            String value = item.getValue() == null ? context.getString(R.string.chat_no_base)
                    : item.getValue().toPlainString().replace('.', ',') + ("%".equals(item.getUnit()) ? "%" : "");
            TextView number = label(tile, value, true);
            number.setTextSize(24);
            if (!"%".equals(item.getUnit())) muted(tile, ChatDisplayLabels.unit(item.getUnit()));
            if (item.getNumerator() != null) muted(tile, context.getString(R.string.chat_indicator_basis,
                    item.getNumerator().toString(), item.getDenominator().toString()));
            muted(tile, item.getCriterion());
            tile.setFocusable(true);
            tile.setContentDescription(item.getLabel() + ": " + value + ". " + ChatDisplayLabels.unit(item.getUnit())
                    + (item.getNumerator() == null ? "" : ". " + context.getString(R.string.chat_indicator_basis,
                    item.getNumerator().toString(), item.getDenominator().toString())) + ". " + item.getCriterion());
            tile.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            for (int index = 0; index < tile.getChildCount(); index++) tile.getChildAt(index).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    static void context(LinearLayout card, ChatContextBlock block) {
        Context context = card.getContext();
        card.setPadding(0, dp(context, 10), 0, 0);
        field(card, context.getString(R.string.chat_data_source), block.getSource());
        if (block.getReference() != null) field(card, context.getString(R.string.chat_data_reference), block.getReference());
        if (block.getStart() != null) field(card, context.getString(R.string.chat_data_period), block.getStart() + " — " + block.getEnd());
        field(card, context.getString(R.string.chat_data_scope), block.getScope());
        for (Map.Entry<String, JsonElement> filter : block.getFilters().entrySet()) {
            field(card, ChatDisplayLabels.field(filter.getKey()), filterValue(filter.getKey(), filter.getValue()));
        }
        for (String criterion : block.getCriteria()) muted(card, criterion);
        for (String warning : block.getWarnings()) label(card, warning, false);
        summary(card, block.getDataset());
    }

    static void summary(LinearLayout card, ChatDatasetSummary dataset) {
        Context context = card.getContext();
        String unit = dataset.getTotalUnit();
        label(card, context.getString(R.string.chat_dataset_received, dataset.getReturned(), dataset.getTotal().toString(),
                unit == null ? context.getString(R.string.chat_data_records) : unit, dataset.getRequested().toString()), false);
        if (dataset.getTotal().signum() == 0) label(card, context.getString(R.string.chat_dataset_empty), false);
        else if (dataset.getReturned() == 0) label(card, context.getString(R.string.chat_dataset_no_rows), false);
        if (dataset.isLimited()) label(card, context.getString("tamanho_resposta".equals(dataset.getLimitReason())
                ? R.string.chat_table_size_limit : R.string.chat_table_count_limit), false);
        else if (dataset.hasMore()) label(card, context.getString(R.string.chat_table_partial), false);
    }

    private static void field(LinearLayout card, String caption, String value) {
        label(card, caption + ": " + value, false);
    }
    private static TextView muted(LinearLayout card, String value) {
        TextView text = label(card, value, false);
        text.setTextColor(card.getContext().getColor(R.color.chat_muted));
        text.setTextSize(12);
        return text;
    }
    private static String filterValue(String key, JsonElement value) {
        if (value.isJsonArray()) {
            StringBuilder text = new StringBuilder();
            for (JsonElement item : value.getAsJsonArray()) {
                if (text.length() > 0) text.append(", ");
                text.append(ChatDisplayLabels.format(key, item.getAsString()));
            }
            return text.toString();
        }
        if (value.getAsJsonPrimitive().isBoolean()) return value.getAsBoolean() ? "Sim" : "Não";
        return ChatDisplayLabels.format(key, value.getAsString());
    }
}
