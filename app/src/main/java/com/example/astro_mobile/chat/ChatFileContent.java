package com.example.astro_mobile.chat;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.astro_mobile.R;
import com.example.astro_mobile.data.ai.ChatFileBlock;
import com.example.astro_mobile.data.ai.ChatFileRepository;
import java.time.Instant;

/** Cartão de arquivo separado do texto; nenhuma requisição durante a renderização. */
final class ChatFileContent {
    private ChatFileContent() { }
    static void render(LinearLayout card, ChatFileBlock file) {
        Context context = card.getContext();
        GradientDrawable background = new GradientDrawable();
        background.setColor(context.getColor(R.color.chat_user));
        background.setCornerRadius(ChatStructuredContent.dp(context, 12));
        card.setBackground(background);
        int padding = ChatStructuredContent.dp(context, 12);
        card.setPadding(padding, padding, padding, padding);
        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_chat_pdf);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(icon, new LinearLayout.LayoutParams(ChatStructuredContent.dp(context, 28), ChatStructuredContent.dp(context, 32)));
        TextView name = ChatStructuredContent.label(header, file.getName(), true);
        name.setAccessibilityHeading(true);
        name.setPadding(padding, 0, 0, 0);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(header);
        if (file.getSize() != null) {
            TextView size = ChatStructuredContent.label(card, context.getString(R.string.chat_file_size,
                    java.text.NumberFormat.getIntegerInstance().format(file.getSize())), false);
            size.setTextColor(context.getColor(R.color.chat_muted));
            size.setTextSize(12);
        }
        TextView status = ChatStructuredContent.label(card, "", false);
        status.setTextSize(12);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        Button download = new Button(context);
        download.setText(R.string.chat_file_download);
        download.setAllCaps(false);
        download.setTextColor(context.getColor(R.color.chat_link));
        download.setBackgroundResource(R.drawable.bg_home_icon_ripple);
        download.setMinHeight(ChatStructuredContent.dp(context, 48));
        download.setContentDescription(context.getString(R.string.chat_file_download_named, file.getName()));
        card.addView(download);
        boolean[] busy = {false};
        Runnable update = () -> {
            ChatFileBlock.Availability availability = file.availability(Instant.now());
            download.setEnabled(!busy[0] && availability == ChatFileBlock.Availability.AVAILABLE);
            download.setAlpha(download.isEnabled() ? 1f : .5f);
            status.setText(busy[0] ? R.string.chat_file_downloading : availability == ChatFileBlock.Availability.EXPIRED
                    ? R.string.chat_file_expired : availability == ChatFileBlock.Availability.UNAVAILABLE
                    ? R.string.chat_file_unavailable : R.string.chat_file_pdf);
        };
        Runnable refresh = new Runnable() {
            @Override public void run() {
                update.run();
                if (file.availability(Instant.now()) == ChatFileBlock.Availability.AVAILABLE) {
                    Instant now = Instant.now();
                    long delay = file.getExpiration().isAfter(now.plusSeconds(60)) ? 60_000
                            : Math.max(1, java.time.Duration.between(now, file.getExpiration()).toMillis());
                    card.postDelayed(this, delay);
                }
            }
        };
        card.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { card.removeCallbacks(refresh); refresh.run(); }
            @Override public void onViewDetachedFromWindow(View view) { card.removeCallbacks(refresh); }
        });
        download.setOnClickListener(view -> {
            update.run();
            if (!download.isEnabled()) return;
            busy[0] = true; update.run();
            Context app = context.getApplicationContext();
            new ChatFileRepository(app).download(file, result -> {
                busy[0] = false; update.run();
                int message = result == ChatFileRepository.Result.SAVED ? R.string.chat_file_saved
                        : result == ChatFileRepository.Result.EXPIRED ? R.string.chat_file_expired
                        : result == ChatFileRepository.Result.BUSY ? R.string.chat_file_already_downloading : R.string.chat_file_failed;
                status.setText(message);
                Toast.makeText(app, message, Toast.LENGTH_LONG).show();
            });
        });
        update.run();
    }
}
