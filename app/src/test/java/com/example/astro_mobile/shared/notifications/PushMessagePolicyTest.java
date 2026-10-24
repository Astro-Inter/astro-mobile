package com.example.astro_mobile.shared.notifications;

import org.junit.Test;
import static org.junit.Assert.*;

public final class PushMessagePolicyTest {
    @Test public void acceptsOnlyPendingEventsForCurrentAccountAndNewCycle() {
        assertTrue(PushMessagePolicy.shouldDisplay("pending_events", "uid", true, 200, 100));
        assertFalse(PushMessagePolicy.shouldDisplay("other", "uid", true, 200, 100));
        assertFalse(PushMessagePolicy.shouldDisplay("pending_events", "uid", false, 200, 100));
        assertFalse(PushMessagePolicy.shouldDisplay("pending_events", "uid", true, 100, 100));
        assertFalse(PushMessagePolicy.shouldDisplay("pending_events", "uid", true, 50, 100));
        assertFalse(PushMessagePolicy.shouldDisplay("pending_events", null, true, 200, 100));
    }
}
