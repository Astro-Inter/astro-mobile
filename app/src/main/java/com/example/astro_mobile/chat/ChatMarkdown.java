package com.example.astro_mobile.chat;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.astro_mobile.R;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TableAwareMovementMethod;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tables.TableTheme;
import io.noties.markwon.movement.MovementMethodPlugin;

public final class ChatMarkdown {
    private ChatMarkdown() { }

    public static Markwon create(Context context) {
        return Markwon.builder(context)
                .usePlugin(TablePlugin.create(TableTheme.buildWithDefaults(context)
                        .tableBorderColor(context.getColor(R.color.chat_outline))
                        .tableBorderWidth(Math.max(1, Math.round(context.getResources().getDisplayMetrics().density)))
                        .tableCellPadding(Math.round(8 * context.getResources().getDisplayMetrics().density))
                        .tableHeaderRowBackgroundColor(context.getColor(R.color.astro_input_background))
                        .tableEvenRowBackgroundColor(context.getColor(R.color.chat_assistant))
                        .tableOddRowBackgroundColor(context.getColor(R.color.chat_assistant))
                        .build()))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(MovementMethodPlugin.create(TableAwareMovementMethod.create()))
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
                        builder.linkColor(context.getColor(R.color.chat_link))
                                .isLinkUnderlined(true)
                                .codeTypeface(Typeface.MONOSPACE)
                                .codeTextColor(context.getColor(R.color.chat_text))
                                .codeBackgroundColor(context.getColor(R.color.chat_code))
                                .codeBlockBackgroundColor(context.getColor(R.color.chat_code))
                                .codeBlockTextColor(context.getColor(R.color.chat_text))
                                .blockQuoteColor(context.getColor(R.color.chat_link))
                                .headingBreakColor(context.getColor(R.color.chat_outline))
                                .thematicBreakColor(context.getColor(R.color.chat_outline))
                                .headingTextSizeMultipliers(new float[]{1.4f, 1.25f, 1.12f, 1f, 1f, 1f});
                    }

                    @Override public void configureConfiguration(@NonNull MarkwonConfiguration.Builder builder) {
                        builder.linkResolver((view, link) -> {
                            Uri uri = Uri.parse(link);
                            String scheme = uri.getScheme();
                            if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
                                Toast.makeText(view.getContext(), R.string.chat_link_unavailable, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            try {
                                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, uri)
                                        .addCategory(Intent.CATEGORY_BROWSABLE));
                            } catch (ActivityNotFoundException error) {
                                Toast.makeText(view.getContext(), R.string.chat_link_unavailable, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).build();
    }
}
