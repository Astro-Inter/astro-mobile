package com.example.astro_mobile.chat;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.astro_mobile.R;
import com.example.astro_mobile.BuildConfig;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    static final String CALENDAR_MARKER = "google-calendar-conectar";
    static final String CALENDAR_URL = BuildConfig.AI_API_BASE_URL + "integracoes/google-calendar/conectar";
    private static final Pattern CALENDAR_TEXT_LINK = Pattern.compile("\\[google-calendar-conectar\\]\\(([^\\r\\n)]*)\\)");
    private ChatMarkdown() { }

    public static Markwon create(Context context) {
        return create(context, null);
    }

    static Markwon create(Context context, Runnable connectCalendar) {
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
                    @Override public void beforeRender(@NonNull Node node) {
                        normalizeCalendarLinks(node);
                    }

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
                            if (CALENDAR_URL.equals(link)) {
                                if (connectCalendar != null) connectCalendar.run();
                                else Toast.makeText(view.getContext(), R.string.chat_link_unavailable, Toast.LENGTH_SHORT).show();
                                return;
                            }
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

    static void normalizeCalendarLinks(Node document) {
        document.accept(new AbstractVisitor() {
            @Override public void visit(Text text) {
                // Destinos com espaços não são links válidos no CommonMark. Aceita
                // o marcador enviado pela IA também nesse caso, fora de código.
                String literal = text.getLiteral();
                Matcher matcher = CALENDAR_TEXT_LINK.matcher(literal);
                int end = 0;
                boolean replaced = false;
                while (matcher.find()) {
                    replaced = true;
                    if (matcher.start() > end) text.insertBefore(new Text(literal.substring(end, matcher.start())));
                    String label = matcher.group(1).trim();
                    Link link = new Link(CALENDAR_URL, null);
                    link.appendChild(new Text(label.isEmpty() ? "Conectar Google Calendar" : label));
                    text.insertBefore(link);
                    end = matcher.end();
                }
                if (replaced) {
                    if (end < literal.length()) text.insertBefore(new Text(literal.substring(end)));
                    text.unlink();
                }
            }

            @Override public void visit(Link link) {
                String destination = link.getDestination();
                if (CALENDAR_MARKER.equals(destination) || ("/integracoes/google-calendar/conectar").equals(destination)) {
                    link.setDestination(CALENDAR_URL);
                } else if (link.getFirstChild() instanceof Text && link.getFirstChild() == link.getLastChild()
                        && CALENDAR_MARKER.equals(((Text) link.getFirstChild()).getLiteral())) {
                    // Também aceita o formato [google-calendar-conectar](Texto do link).
                    String label = destination == null || destination.trim().isEmpty()
                            ? "Conectar Google Calendar" : destination;
                    link.getFirstChild().unlink();
                    link.appendChild(new Text(label));
                    link.setDestination(CALENDAR_URL);
                }
                visitChildren(link);
            }
        });
    }
}
