package com.example.astro_mobile.notifications;

import androidx.annotation.StringRes;

public final class NotificationItem {
    @StringRes public final int title;
    @StringRes public final int time;
    public final boolean unread;

    public NotificationItem(@StringRes int title, @StringRes int time, boolean unread) {
        this.title = title;
        this.time = time;
        this.unread = unread;
    }
}
