package com.example.astro_mobile.data.chat;

import com.example.astro_mobile.data.api.dto.ChatTypingRequest;
import java.util.function.Predicate;

/** Estado temporário de digitação, independente da confirmação de mensagens. */
public final class ChatTypingSession {
    private final ChatScheduler scheduler;
    private final Predicate<ChatTypingRequest> sender;
    private final Runnable changed;
    private ChatScheduler.Task idle, expiry;
    private Long recipientId;
    private String conversationId;
    private boolean sent, remoteTyping;
    private long lastSent;

    public ChatTypingSession(ChatScheduler scheduler, Predicate<ChatTypingRequest> sender, Runnable changed) {
        this.scheduler = scheduler; this.sender = sender; this.changed = changed;
    }

    public void open(Long recipientId, String conversationId) {
        reset();
        this.recipientId = recipientId; this.conversationId = conversationId;
    }

    public void edit(String text) {
        if (recipientId == null && conversationId == null) return;
        if (text == null || text.codePoints().allMatch(Character::isWhitespace)) { stopLocal(); return; }
        if (!sent || scheduler.now() - lastSent >= 2000) {
            sent = sender.test(new ChatTypingRequest(recipientId, conversationId, true));
            lastSent = scheduler.now();
        }
        if (idle != null) idle.cancel();
        idle = scheduler.after(2500, this::stopLocal);
    }

    public void stopLocal() {
        if (idle != null) { idle.cancel(); idle = null; }
        if (sent) sender.test(new ChatTypingRequest(recipientId, conversationId, false));
        sent = false;
    }

    public void receive(boolean typing, long remainingMillis) {
        if (!typing || remainingMillis <= 0) { clearRemote(); return; }
        if (expiry != null) expiry.cancel();
        boolean wasTyping = remoteTyping;
        remoteTyping = true;
        expiry = scheduler.after(Math.min(5000, remainingMillis), this::clearRemote);
        if (!wasTyping) changed.run();
    }

    public boolean isRemoteTyping() { return remoteTyping; }

    public void clearRemote() {
        if (expiry != null) { expiry.cancel(); expiry = null; }
        if (remoteTyping) { remoteTyping = false; changed.run(); }
    }

    public void reset() { stopLocal(); clearRemote(); }
    public void close() { reset(); recipientId = null; conversationId = null; }
}
