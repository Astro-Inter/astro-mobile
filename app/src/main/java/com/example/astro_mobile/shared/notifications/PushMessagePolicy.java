package com.example.astro_mobile.shared.notifications;

public final class PushMessagePolicy {
    private PushMessagePolicy() { }
    public static boolean shouldDisplay(String type, String recipient, boolean currentUser, long cycle, long previousCycle) {
        return "pending_events".equals(type) && recipient != null && currentUser && cycle > 0 && cycle > previousCycle;
    }
}
