package com.example.astro_mobile.data.chat;

public interface ChatScheduler {
    interface Task { void cancel(); }
    Task after(long delayMillis, Runnable runnable);
    long now();
}
