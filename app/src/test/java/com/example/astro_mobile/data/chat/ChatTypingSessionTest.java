package com.example.astro_mobile.data.chat;

import static org.junit.Assert.*;
import com.example.astro_mobile.data.api.dto.ChatTypingRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;

public final class ChatTypingSessionTest {
    private final Clock clock = new Clock();
    private final List<ChatTypingRequest> sent = new ArrayList<>();
    private final ChatTypingSession session = new ChatTypingSession(clock, request -> { sent.add(request); return true; }, () -> { });

    @Test public void throttlesKeystrokesAndStopsAfterInactivity() {
        session.open(2L, null); session.edit("a");
        clock.advance(1000); session.edit("ab");
        assertEquals(1, sent.size());
        clock.advance(1000); session.edit("abc");
        assertEquals(2, sent.size());
        assertTrue(sent.get(1).typing);
        clock.advance(2500);
        assertEquals(3, sent.size()); assertFalse(sent.get(2).typing);
    }

    @Test public void blankSendAndLeavingStopWithoutReplaying() {
        session.open(2L, null); session.edit("a"); session.edit("  ");
        assertFalse(sent.get(1).typing);
        session.edit("next"); session.stopLocal();
        assertFalse(sent.get(3).typing);
        session.edit("draft"); session.close();
        assertFalse(sent.get(5).typing);
        int count = sent.size(); clock.advance(10000);
        assertEquals(count, sent.size());
    }

    @Test public void remoteExpiresAndNeverLeaksAcrossConversationOrDisconnect() {
        session.open(2L, null); session.receive(true, 5000);
        assertTrue(session.isRemoteTyping());
        clock.advance(4000); session.receive(true, 5000);
        clock.advance(4000); assertTrue(session.isRemoteTyping());
        clock.advance(1000); assertFalse(session.isRemoteTyping());
        session.receive(true, 5000); session.open(3L, null);
        assertFalse(session.isRemoteTyping());
        session.receive(true, 5000); session.reset();
        assertFalse(session.isRemoteTyping());
        session.receive(true, -1); assertFalse(session.isRemoteTyping());
    }

    private static final class Job {
        long when; Runnable action; boolean canceled;
    }
    private static final class Clock implements ChatScheduler {
        long now; final List<Job> jobs = new ArrayList<>();
        public long now() { return now; }
        public Task after(long delay, Runnable action) {
            Job job = new Job(); job.when = now + delay; job.action = action; jobs.add(job);
            return () -> job.canceled = true;
        }
        void advance(long duration) {
            long target = now + duration;
            while (true) {
                Job next = jobs.stream().filter(job -> !job.canceled && job.when <= target)
                        .min(Comparator.comparingLong(job -> job.when)).orElse(null);
                if (next == null) break;
                jobs.remove(next); now = next.when; next.action.run();
            }
            now = target;
        }
    }
}
