package com.example.astro_mobile.chat;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import com.example.astro_mobile.R;
import com.example.astro_mobile.data.ai.GoogleCalendarRepository;

/** A conexão só começa ao tocar no link; o token fica no Repository. */
final class ChatCalendarLinkHandler {
    private final Context context;
    private final GoogleCalendarRepository repository;

    ChatCalendarLinkHandler(Context context) {
        this(context, GoogleCalendarRepository.createDefault());
    }

    ChatCalendarLinkHandler(Context context, GoogleCalendarRepository repository) {
        this.context = context;
        this.repository = repository;
    }

    void connect() {
        if (repository.connect(this::showResult)) {
            Toast.makeText(context, R.string.chat_calendar_loading, Toast.LENGTH_SHORT).show();
        }
    }

    private void showResult(String url, GoogleCalendarRepository.Failure failure) {
        Uri uri = url == null ? null : Uri.parse(url);
        if (uri != null && "https".equalsIgnoreCase(uri.getScheme())
                && uri.getHost() != null && uri.getUserInfo() == null) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE));
                return;
            } catch (ActivityNotFoundException ignored) { }
        }
        int message = failure == GoogleCalendarRepository.Failure.SESSION ? R.string.chat_calendar_session_error
                : failure == GoogleCalendarRepository.Failure.TIMEOUT ? R.string.chat_calendar_timeout
                : R.string.chat_calendar_error;
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    void close() { repository.close(); }
}
