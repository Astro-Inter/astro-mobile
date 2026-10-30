package com.example.astro_mobile.data.chat;

import com.example.astro_mobile.data.api.dto.ChatSendError;
import com.example.astro_mobile.data.api.dto.ChatSendRequest;
import com.example.astro_mobile.data.api.dto.ChatSendResponse;
import com.example.astro_mobile.data.firebase.IdTokenProvider;
import com.google.gson.Gson;
import java.util.Map;

/** Uma sessão STOMP por proprietário. Chamadas e callbacks ficam na mesma fila serial. */
public final class StompChatClient {
    public interface Listener {
        void onState(ChatConnectionState state);
        void onMessage(ChatSendResponse message);
        void onError(ChatSendError error);
        default void onTyping(com.example.astro_mobile.data.api.dto.ChatTypingData event) { }
    }
    private final ChatTransport transport;
    private final IdTokenProvider tokens;
    private final ChatScheduler scheduler;
    private final Listener listener;
    private final Gson gson = new Gson();
    private final StompFrame.Decoder decoder = new StompFrame.Decoder();
    private ChatTransport.Socket socket;
    private ChatScheduler.Task timer, heartbeat;
    private ChatConnectionState state = ChatConnectionState.STOPPED;
    private boolean active;
    private boolean typingSupported;
    private int generation, retries;
    private long receivedAt, incomingHeartbeat;
    private Long authenticatedUserId;

    public StompChatClient(ChatTransport transport, IdTokenProvider tokens, ChatScheduler scheduler, Listener listener) {
        this.transport = transport; this.tokens = tokens; this.scheduler = scheduler; this.listener = listener;
    }

    public ChatConnectionState getState() { return state; }
    public Long getAuthenticatedUserId() { return authenticatedUserId; }

    public void setActive(boolean value) {
        if (active == value) return;
        active = value;
        if (!active) { stopSocket(); retries = 0; change(ChatConnectionState.STOPPED); }
        else connect();
    }

    public void retry() {
        if (!active || state == ChatConnectionState.CONNECTED || state == ChatConnectionState.CONNECTING) return;
        stopSocket(); retries = 0; connect();
    }

    private void connect() {
        stopSocket();
        if (!active) return;
        int id = generation;
        change(ChatConnectionState.CONNECTING);
        timer = scheduler.after(20000, () -> lost(id));
        // O token é renovado antes de CADA conexão, nunca colocado na URL ou no handshake HTTP.
        tokens.getToken(true, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != generation || !active) return;
                socket = transport.open(new ChatTransport.Listener() {
                    @Override public void onOpen() {
                        if (id != generation) return;
                        sendFrame("CONNECT", Map.of("accept-version", "1.2", "host", "api.astro-app.solutions",
                                // O interceptor atual rejeita heartbeats STOMP do cliente (comando null).
                                // Recebe STOMP a cada 10s; OkHttp verifica saída com ping/pong WS em 10s.
                                "heart-beat", "0,10000", "Authorization", "Bearer " + token), "");
                    }
                    @Override public void onText(String text) {
                        if (id != generation) return;
                        receivedAt = scheduler.now();
                        try { decoder.accept(text, frame -> receive(id, frame)); }
                        catch (RuntimeException invalid) { reject(ChatConnectionState.FAILED); }
                    }
                    @Override public void onClosed() { lost(id); }
                    @Override public void onTlsFailure() {
                        if (id == generation) reject(ChatConnectionState.TLS_FAILED);
                    }
                });
            }
            @Override public void onFailure(IdTokenProvider.Failure failure) {
                if (id != generation) return;
                if (failure == IdTokenProvider.Failure.SESSION) reject(ChatConnectionState.SESSION_EXPIRED);
                else lost(id);
            }
        });
    }

    private void receive(int id, StompFrame frame) {
        if (id != generation) return;
        if ("ERROR".equals(frame.command)) {
            // Spring não fornece código de autenticação estável no ERROR de CONNECT.
            reject(ChatConnectionState.REJECTED);
        } else if ("CONNECTED".equals(frame.command) && state == ChatConnectionState.CONNECTING) {
            if (!"1.2".equals(frame.headers.get("version"))) { reject(ChatConnectionState.FAILED); return; }
            // Spring informa Principal.getName() no CONNECTED; no Astro ele é o ID PostgreSQL.
            String userName = frame.headers.get("user-name");
            authenticatedUserId = userName == null ? null : Long.valueOf(userName);
            String[] beats = frame.headers.getOrDefault("heart-beat", "0,0").split(",");
            long serverSend = Long.parseLong(beats[0]), serverReceive = Long.parseLong(beats[1]);
            if (serverSend < 0 || serverReceive < 0) throw new IllegalArgumentException("Heartbeat inválido");
            incomingHeartbeat = serverSend == 0 ? 0 : Math.max(10000, serverSend);
            typingSupported = "1".equals(frame.headers.get("astro-typing"));
            if (!sendFrame("SUBSCRIBE", Map.of("id", "private-messages", "destination", "/user/queue/messages", "ack", "auto"), "")
                    || !sendFrame("SUBSCRIBE", Map.of("id", "private-errors", "destination", "/user/queue/errors", "ack", "auto"), "")) {
                lost(id); return;
            }
            if (typingSupported && !sendFrame("SUBSCRIBE", Map.of("id", "private-typing", "destination", "/user/queue/typing", "ack", "auto"), "")) {
                lost(id); return;
            }
            if (timer != null) timer.cancel();
            retries = 0;
            receivedAt = scheduler.now();
            change(ChatConnectionState.CONNECTED);
            tick(id);
        } else if ("MESSAGE".equals(frame.command) && state == ChatConnectionState.CONNECTED) {
            String subscription = frame.headers.get("subscription");
            if ("private-messages".equals(subscription)) {
                ChatSendResponse message = gson.fromJson(frame.body, ChatSendResponse.class);
                if (message == null || !message.isValid()) throw new IllegalArgumentException("Evento inválido");
                listener.onMessage(message);
            } else if ("private-errors".equals(subscription)) {
                ChatSendError error = gson.fromJson(frame.body, ChatSendError.class);
                if (error == null || error.code == null) throw new IllegalArgumentException("Erro inválido");
                listener.onError(error);
            } else if (typingSupported && "private-typing".equals(subscription)) {
                try {
                    com.example.astro_mobile.data.api.dto.ChatTypingData event = gson.fromJson(frame.body,
                            com.example.astro_mobile.data.api.dto.ChatTypingData.class);
                    if (event != null && event.isValid()) listener.onTyping(event);
                } catch (com.google.gson.JsonParseException ignored) { }
            }
        }
    }

    public boolean send(ChatSendRequest request) {
        if (state != ChatConnectionState.CONNECTED) return false;
        boolean queued = sendFrame("SEND", Map.of("destination", "/app/chat.send", "content-type", "application/json"), gson.toJson(request));
        if (!queued) lost(generation);
        return queued;
    }

    public boolean sendTyping(com.example.astro_mobile.data.api.dto.ChatTypingRequest request) {
        if (!typingSupported || state != ChatConnectionState.CONNECTED) return false;
        return sendFrame("SEND", Map.of("destination", "/app/chat.typing", "content-type", "application/json"), gson.toJson(request));
    }

    private boolean sendFrame(String command, Map<String, String> headers, String body) {
        if (socket == null) return false;
        return socket.send(StompFrame.encode(command, headers, body));
    }

    private void tick(int id) {
        heartbeat = scheduler.after(1000, () -> {
            if (id != generation || state != ChatConnectionState.CONNECTED) return;
            long now = scheduler.now();
            if (incomingHeartbeat > 0 && now - receivedAt > incomingHeartbeat * 2 + 5000) { lost(id); return; }
            tick(id);
        });
    }

    private void lost(int id) {
        if (id != generation || !active) return;
        stopSocket();
        if (retries >= 6) { change(ChatConnectionState.FAILED); return; }
        long delay = Math.min(30000, 1000L << retries++);
        change(ChatConnectionState.RETRYING);
        timer = scheduler.after(delay, this::connect);
    }

    private void reject(ChatConnectionState reason) { stopSocket(); change(reason); }

    private void stopSocket() {
        ++generation;
        if (timer != null) { timer.cancel(); timer = null; }
        if (heartbeat != null) { heartbeat.cancel(); heartbeat = null; }
        if (socket != null) {
            if (state == ChatConnectionState.CONNECTED) {
                sendFrame("UNSUBSCRIBE", Map.of("id", "private-messages"), "");
                sendFrame("UNSUBSCRIBE", Map.of("id", "private-errors"), "");
                if (typingSupported) sendFrame("UNSUBSCRIBE", Map.of("id", "private-typing"), "");
                sendFrame("DISCONNECT", Map.of(), "");
            }
            socket.close(); socket = null;
        }
        decoder.reset();
        authenticatedUserId = null;
        typingSupported = false;
    }

    private void change(ChatConnectionState value) { state = value; listener.onState(value); }
}
