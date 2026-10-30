package com.example.astro_mobile.data.chat;

import static org.junit.Assert.*;
import com.example.astro_mobile.data.api.dto.ChatSendError;
import com.example.astro_mobile.data.api.dto.ChatSendRequest;
import com.example.astro_mobile.data.api.dto.ChatSendResponse;
import com.example.astro_mobile.data.firebase.IdTokenProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;

public final class StompChatClientTest {
    private static final String CONVERSATION = "11111111-2222-4333-8444-555555555555";
    private static final String OPERATION = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";
    private static final String EVENT = "{\"clientMessageId\":\"" + OPERATION + "\",\"id\":\"22222222-3333-4444-8555-666666666666\","
            + "\"conversationId\":\"" + CONVERSATION + "\",\"senderId\":1,\"content\":\"Olá!\",\"createdAt\":\"2026-10-26T18:30:00Z\"}";

    @Test public void authenticatesConnectAndSubscribesOnlyOnce() {
        Fixture f = new Fixture();
        f.client.setActive(true); f.client.setActive(true);
        assertEquals(1, f.transport.listeners.size());
        assertEquals(List.of(true), f.refreshes);
        f.transport.listener().onOpen();
        String connect = f.transport.frames.get(0);
        assertTrue(connect.startsWith("CONNECT\n"));
        assertTrue(connect.contains("Authorization:Bearer token-1\n"));
        assertTrue(connect.contains("accept-version:1.2\n"));
        assertTrue(connect.contains("heart-beat:0,10000\n"));
        assertEquals(1, f.transport.frames.size());
        f.connected(); f.connected();
        assertEquals(3, f.transport.frames.size());
        assertTrue(f.transport.frames.get(1).contains("destination:/user/queue/messages"));
        assertTrue(f.transport.frames.get(2).contains("destination:/user/queue/errors"));
        assertEquals(ChatConnectionState.CONNECTED, f.client.getState());
    }

    @Test public void sendsContractAndReceivesPrivateConfirmationAndCorrelatedError() {
        Fixture f = new Fixture(); f.start();
        assertTrue(f.client.send(new ChatSendRequest(25L, null, "Olá!", OPERATION)));
        String send = f.transport.frames.get(3);
        assertTrue(send.startsWith("SEND\n"));
        assertTrue(send.contains("destination:/app/chat.send\n"));
        assertTrue(send.contains("\"recipientId\":25"));
        assertFalse(send.contains("conversationId"));
        assertFalse(send.contains("Authorization"));
        f.transport.listener().onText("MESSAGE\nsubscription:private-messages\n\n" + EVENT + "\0");
        assertEquals(OPERATION, f.messages.get(0).clientMessageId);
        f.transport.listener().onText("MESSAGE\nsubscription:private-errors\n\n"
                + "{\"clientMessageId\":\"" + OPERATION + "\",\"code\":\"FORBIDDEN\",\"message\":\"Sem acesso\"}\0");
        assertEquals("FORBIDDEN", f.errors.get(0).code);
        assertEquals(OPERATION, f.errors.get(0).clientMessageId);
    }

    @Test public void reconnectsWithFreshTokenAndNewSubscriptionsWithoutResending() {
        Fixture f = new Fixture(); f.start();
        f.client.send(new ChatSendRequest(null, CONVERSATION, "Olá!", OPERATION));
        ChatTransport.Listener old = f.transport.listener();
        old.onClosed();
        assertEquals(ChatConnectionState.RETRYING, f.client.getState());
        f.clock.advance(999); assertEquals(1, f.transport.listeners.size());
        f.clock.advance(1); assertEquals(2, f.transport.listeners.size());
        f.transport.listener().onOpen(); f.connected();
        old.onClosed(); old.onText("ERROR\n\nold\0");
        assertEquals(ChatConnectionState.CONNECTED, f.client.getState());
        assertEquals(List.of(true, true), f.refreshes);
        assertEquals(1, f.transport.frames.stream().filter(frame -> frame.startsWith("SEND\n")).count());
        assertEquals(4, f.transport.frames.stream().filter(frame -> frame.startsWith("SUBSCRIBE\n")).count());
        assertTrue(f.transport.frames.stream().anyMatch(frame -> frame.contains("Authorization:Bearer token-2")));
    }

    @Test public void progressivelyBacksOffAndStopsAfterSixRetries() {
        Fixture f = new Fixture(); f.client.setActive(true);
        long[] waits = {1000, 2000, 4000, 8000, 16000, 30000};
        for (long wait : waits) {
            f.transport.listener().onClosed();
            int count = f.transport.listeners.size();
            f.clock.advance(wait - 1); assertEquals(count, f.transport.listeners.size());
            f.clock.advance(1); assertEquals(count + 1, f.transport.listeners.size());
        }
        f.transport.listener().onClosed();
        assertEquals(ChatConnectionState.FAILED, f.client.getState());
        f.clock.advance(60000); assertEquals(7, f.transport.listeners.size());
        f.client.retry(); assertEquals(8, f.transport.listeners.size());
    }

    @Test public void negotiatesHeartbeatsAndDetectsDeadConnection() {
        Fixture f = new Fixture(); f.start();
        f.clock.advance(10000);
        // A saída é verificada pelo ping/pong do transporte Android; não envia frame STOMP rejeitado.
        assertFalse(f.transport.frames.contains("\n"));
        f.transport.listener().onText("\n");
        f.clock.advance(20000);
        assertEquals(ChatConnectionState.CONNECTED, f.client.getState());
        f.clock.advance(6000);
        assertEquals(ChatConnectionState.RETRYING, f.client.getState());
    }

    @Test public void logoutUnsubscribesCancelsRetryAndDiscardsLateToken() {
        Fixture f = new Fixture(); f.start();
        f.client.setActive(false);
        assertEquals(ChatConnectionState.STOPPED, f.client.getState());
        assertEquals(2, f.transport.frames.stream().filter(frame -> frame.startsWith("UNSUBSCRIBE\n")).count());
        assertTrue(f.transport.frames.stream().anyMatch(frame -> frame.startsWith("DISCONNECT\n")));
        f.transport.listener().onClosed(); f.clock.advance(120000);
        assertEquals(1, f.transport.listeners.size());
        List<IdTokenProvider.Callback> tokens = new ArrayList<>();
        StompChatClient waiting = new StompChatClient(f.transport, (refresh, cb) -> tokens.add(cb), f.clock, f.listener);
        waiting.setActive(true); waiting.setActive(false); tokens.get(0).onToken("stale");
        assertEquals(1, f.transport.listeners.size());
    }

    @Test public void tokenSessionFailureDoesNotOpenTransportAndConnectErrorDoesNotLoop() {
        Fixture f = new Fixture();
        StompChatClient denied = new StompChatClient(f.transport, (refresh, cb) -> cb.onFailure(IdTokenProvider.Failure.SESSION), f.clock, f.listener);
        denied.setActive(true);
        assertEquals(ChatConnectionState.SESSION_EXPIRED, denied.getState());
        assertTrue(f.transport.listeners.isEmpty());
        f.client.setActive(true);
        f.transport.listener().onText("ERROR\nmessage:Rejected\n\n\0");
        f.clock.advance(120000);
        assertEquals(ChatConnectionState.REJECTED, f.client.getState());
        assertEquals(1, f.transport.listeners.size());
    }

    @Test public void foregroundOrNetworkReturnRestoresConnectionAndSubscriptions() {
        Fixture f = new Fixture(); f.start();
        f.client.setActive(false); f.client.setActive(true);
        f.transport.listener().onOpen(); f.connected();
        assertEquals(2, f.transport.listeners.size());
        assertEquals(4, f.transport.frames.stream().filter(frame -> frame.startsWith("SUBSCRIBE\n")).count());
    }
    @Test public void certificateFailureIsDistinctAndDoesNotBypassValidationOrLoop() {
        Fixture f = new Fixture(); f.client.setActive(true);
        f.transport.listener().onTlsFailure();
        assertEquals(ChatConnectionState.TLS_FAILED, f.client.getState());
        f.clock.advance(120000);
        assertEquals(1, f.transport.listeners.size());
    }

    @Test public void typingIsNegotiatedAndNeverSentToLegacyBackend() {
        Fixture legacy = new Fixture(); legacy.start();
        assertFalse(legacy.client.sendTyping(new com.example.astro_mobile.data.api.dto.ChatTypingRequest(2L, null, true)));
        assertFalse(legacy.transport.frames.stream().anyMatch(frame -> frame.contains("queue/typing")));
        Fixture f = new Fixture(); f.client.setActive(true); f.transport.listener().onOpen();
        f.transport.listener().onText("CONNECTED\nversion:1.2\nheart-beat:0,0\nastro-typing:1\n\n\0");
        assertEquals(1, f.transport.frames.stream().filter(frame -> frame.contains("destination:/user/queue/typing")).count());
        assertTrue(f.client.sendTyping(new com.example.astro_mobile.data.api.dto.ChatTypingRequest(2L, CONVERSATION, true)));
        String sent = f.transport.frames.get(f.transport.frames.size() - 1);
        assertTrue(sent.contains("destination:/app/chat.typing"));
        assertTrue(sent.contains("\"typing\":true"));
        assertFalse(sent.contains("recipientId"));
        f.transport.listener().onText("MESSAGE\nsubscription:private-typing\n\n{\"conversationId\":\"" + CONVERSATION
                + "\",\"senderId\":2,\"typing\":true,\"expiresAt\":\"2026-10-30T18:00:00Z\"}\0");
        assertEquals(1, f.typing.size());
        f.transport.listener().onText("MESSAGE\nsubscription:private-typing\n\ninvalid-json\0");
        assertEquals(ChatConnectionState.CONNECTED, f.client.getState());
        f.client.setActive(false);
        assertTrue(f.transport.frames.stream().anyMatch(frame -> frame.startsWith("UNSUBSCRIBE") && frame.contains("private-typing")));
        assertFalse(f.client.sendTyping(new com.example.astro_mobile.data.api.dto.ChatTypingRequest(2L, null, true)));
    }

    @Test public void typingSubscriptionsReturnAfterReconnectWithoutReplayingEvents() {
        Fixture f = new Fixture(); f.client.setActive(true); f.transport.listener().onOpen();
        String connected = "CONNECTED\nversion:1.2\nheart-beat:0,0\nastro-typing:1\n\n\0";
        f.transport.listener().onText(connected);
        f.client.sendTyping(new com.example.astro_mobile.data.api.dto.ChatTypingRequest(2L, null, true));
        f.transport.listener().onClosed(); f.clock.advance(1000);
        f.transport.listener().onOpen(); f.transport.listener().onText(connected);
        assertEquals(2, f.transport.frames.stream().filter(frame -> frame.contains("destination:/user/queue/typing")).count());
        assertEquals(1, f.transport.frames.stream().filter(frame -> frame.contains("destination:/app/chat.typing")).count());
    }

    private static final class Fixture {
        final FakeTransport transport = new FakeTransport();
        final FakeClock clock = new FakeClock();
        final List<Boolean> refreshes = new ArrayList<>();
        final List<ChatSendResponse> messages = new ArrayList<>();
        final List<ChatSendError> errors = new ArrayList<>();
        final List<com.example.astro_mobile.data.api.dto.ChatTypingData> typing = new ArrayList<>();
        final StompChatClient.Listener listener = new StompChatClient.Listener() {
            @Override public void onState(ChatConnectionState state) { }
            @Override public void onMessage(ChatSendResponse message) { messages.add(message); }
            @Override public void onError(ChatSendError error) { errors.add(error); }
            @Override public void onTyping(com.example.astro_mobile.data.api.dto.ChatTypingData event) { typing.add(event); }
        };
        final StompChatClient client = new StompChatClient(transport, (refresh, cb) -> {
            refreshes.add(refresh); cb.onToken("token-" + refreshes.size());
        }, clock, listener);
        void connected() { transport.listener().onText("CONNECTED\nversion:1.2\nheart-beat:10000,10000\n\n\0"); }
        void start() { client.setActive(true); transport.listener().onOpen(); connected(); }
    }
    private static final class FakeTransport implements ChatTransport {
        final List<Listener> listeners = new ArrayList<>();
        final List<String> frames = new ArrayList<>();
        Listener listener() { return listeners.get(listeners.size() - 1); }
        @Override public Socket open(Listener listener) {
            listeners.add(listener);
            return new Socket() {
                @Override public boolean send(String text) { frames.add(text); return true; }
                @Override public void close() { }
            };
        }
    }
    private static final class Job {
        long when;
        Runnable runnable;
        boolean canceled;
    }
    private static final class FakeClock implements ChatScheduler {
        long now;
        final List<Job> jobs = new ArrayList<>();
        @Override public long now() { return now; }
        @Override public Task after(long delay, Runnable runnable) {
            Job job = new Job(); job.when = now + delay; job.runnable = runnable; jobs.add(job);
            return () -> job.canceled = true;
        }
        void advance(long duration) {
            long target = now + duration;
            while (true) {
                Job next = jobs.stream().filter(job -> !job.canceled && job.when <= target)
                        .min(Comparator.comparingLong(job -> job.when)).orElse(null);
                if (next == null) break;
                jobs.remove(next); now = next.when; next.runnable.run();
            }
            now = target;
        }
    }
}
