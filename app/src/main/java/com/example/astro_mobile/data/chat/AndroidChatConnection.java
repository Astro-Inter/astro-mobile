package com.example.astro_mobile.data.chat;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import com.example.astro_mobile.BuildConfig;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/** Adapta o transporte existente OkHttp à sessão Firebase, à rede e à Activity. */
public final class AndroidChatConnection {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS)
            .pingInterval(10, TimeUnit.SECONDS).build();
    private final ConnectivityManager networks;
    private final FirebaseAuth auth;
    private final FirebaseAuth.AuthStateListener authListener;
    private final ConnectivityManager.NetworkCallback networkListener;
    private final StompChatClient client;
    private boolean foreground, requested, closed;
    private String uid;
    private Network activeNetwork;

    public AndroidChatConnection(Context context, StompChatClient.Listener listener, Runnable onSessionChanged) {
        this(context, FirebaseAuth.getInstance(), listener, onSessionChanged);
    }

    public AndroidChatConnection(Context context, FirebaseAuth auth, StompChatClient.Listener listener, Runnable onSessionChanged) {
        this.auth = auth;
        networks = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        client = new StompChatClient(callback -> {
            Request request = new Request.Builder().url(BuildConfig.CHAT_WEBSOCKET_URL).build();
            WebSocket socket = http.newWebSocket(request, new WebSocketListener() {
                @Override public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {
                    handler.post(callback::onOpen);
                }
                @Override public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
                    handler.post(() -> callback.onText(text));
                }
                @Override public void onMessage(@NonNull WebSocket ws, @NonNull ByteString bytes) {
                    handler.post(() -> callback.onText(bytes.utf8()));
                }
                @Override public void onClosing(@NonNull WebSocket ws, int code, @NonNull String reason) {
                    ws.close(code, null);
                    handler.post(callback::onClosed);
                }
                @Override public void onClosed(@NonNull WebSocket ws, int code, @NonNull String reason) {
                    handler.post(callback::onClosed);
                }
                @Override public void onFailure(@NonNull WebSocket ws, @NonNull Throwable error, Response response) {
                    if (response != null) response.close();
                    // Não registra frames, tokens ou dados pessoais, inclusive em falhas TLS.
                    if (error instanceof javax.net.ssl.SSLException) handler.post(callback::onTlsFailure);
                    else handler.post(callback::onClosed);
                }
            });
            return new ChatTransport.Socket() {
                @Override public boolean send(String text) { return socket.send(text); }
                @Override public void close() { socket.close(1000, null); }
            };
        }, new FirebaseIdTokenProvider(auth), new ChatScheduler() {
            @Override public Task after(long delayMillis, Runnable runnable) {
                handler.postDelayed(runnable, delayMillis);
                return () -> handler.removeCallbacks(runnable);
            }
            @Override public long now() { return SystemClock.elapsedRealtime(); }
        }, listener);
        uid = auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
        authListener = current -> {
            String next = current.getCurrentUser() == null ? null : current.getCurrentUser().getUid();
            if (!Objects.equals(uid, next)) {
                client.setActive(false);
                uid = next;
                requested = false;
                onSessionChanged.run();
            }
            update();
        };
        networkListener = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(@NonNull Network network) { handler.post(AndroidChatConnection.this::update); }
            @Override public void onLost(@NonNull Network network) { handler.post(AndroidChatConnection.this::update); }
            @Override public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                handler.post(AndroidChatConnection.this::update);
            }
        };
        networks.registerDefaultNetworkCallback(networkListener);
        auth.addAuthStateListener(authListener);
    }

    private void update() {
        if (closed) return;
        NetworkCapabilities caps = networks.getNetworkCapabilities(networks.getActiveNetwork());
        Network nextNetwork = networks.getActiveNetwork();
        if (!Objects.equals(activeNetwork, nextNetwork)) {
            client.setActive(false);
            activeNetwork = nextNetwork;
        }
        boolean online = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        client.setActive(requested && foreground && online && uid != null);
    }

    public StompChatClient client() { return client; }
    public void request() { requested = true; update(); }
    public void setForeground(boolean value) { foreground = value; update(); }
    public void stop() { requested = false; client.setActive(false); }
    public void close() {
        closed = true;
        stop();
        auth.removeAuthStateListener(authListener);
        networks.unregisterNetworkCallback(networkListener);
        http.dispatcher().cancelAll();
        http.connectionPool().evictAll();
        http.dispatcher().executorService().shutdown();
    }
}
