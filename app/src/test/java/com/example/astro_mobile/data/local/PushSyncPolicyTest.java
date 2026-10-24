package com.example.astro_mobile.data.local;

import org.junit.Test;
import static org.junit.Assert.*;

public final class PushSyncPolicyTest {
    @Test public void unchangedSessionDoesNotNeedFirestoreReadsOrWrites() {
        assertFalse(PushSyncPolicy.needsSync("uid", "token", "uid", "token", 1000, 2000));
    }
    @Test public void tokenAccountAndHeartbeatChangesRequireSynchronization() {
        assertTrue(PushSyncPolicy.needsSync("uid", "new", "uid", "old", 1000, 2000));
        assertTrue(PushSyncPolicy.needsSync("new", "token", "old", "token", 1000, 2000));
        assertTrue(PushSyncPolicy.needsSync("uid", "token", "uid", "token", 1000, 1000 + PushSyncPolicy.REFRESH_MILLIS));
        assertTrue(PushSyncPolicy.needsSync("uid", "token", "uid", "token", 1000, 500));
        assertFalse(PushSyncPolicy.needsSync(null, "token", null, null, 0, 1000));
    }
}
