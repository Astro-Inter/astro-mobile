package com.example.astro_mobile.chat;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.astro_mobile.data.api.dto.ChatSendResponse;
import com.example.astro_mobile.data.api.dto.ChatSendError;
import com.example.astro_mobile.data.chat.AndroidChatConnection;
import com.example.astro_mobile.data.chat.ChatConnectionState;
import com.example.astro_mobile.data.chat.StompChatClient;
import com.google.firebase.auth.FirebaseAuth;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Usa a sessão já autenticada no aparelho; nunca extrai tokens nem envia mensagens. */
@RunWith(AndroidJUnit4.class)
public final class HumanChatDeviceTest {
    @Test public void authenticatedConnectAndForegroundReturnRestoreSubscriptions() throws Exception {
        assumeTrue("Requer login Firebase prévio no aparelho", FirebaseAuth.getInstance().getCurrentUser() != null);
        AtomicReference<AndroidChatConnection> connection = new AtomicReference<>();
        AtomicReference<CountDownLatch> connected = new AtomicReference<>(new CountDownLatch(1));
        AtomicReference<ChatConnectionState> last = new AtomicReference<>();
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        try {
            instrumentation.runOnMainSync(() -> {
                AndroidChatConnection runtime = new AndroidChatConnection(instrumentation.getTargetContext(), new StompChatClient.Listener() {
                    @Override public void onState(ChatConnectionState state) {
                        last.set(state);
                        if (state == ChatConnectionState.CONNECTED) connected.get().countDown();
                    }
                    @Override public void onMessage(ChatSendResponse message) { }
                    @Override public void onError(ChatSendError error) { }
                }, () -> { });
                connection.set(runtime);
                runtime.setForeground(true); runtime.request();
            });
            assertTrue("Conexão autenticada não concluída; estado=" + last.get(), connected.get().await(45, TimeUnit.SECONDS));
            // Inclui ao menos três ciclos de heartbeat para detectar rejeições tardias.
            Thread.sleep(32000);
            assertEquals("A sessão deve permanecer conectada durante os heartbeats", ChatConnectionState.CONNECTED, last.get());
            instrumentation.runOnMainSync(() -> connection.get().setForeground(false));
            assertEquals(ChatConnectionState.STOPPED, last.get());
            connected.set(new CountDownLatch(1));
            instrumentation.runOnMainSync(() -> connection.get().setForeground(true));
            assertTrue("Retorno ao primeiro plano não reconectou; estado=" + last.get(), connected.get().await(45, TimeUnit.SECONDS));
        } finally {
            if (connection.get() != null) instrumentation.runOnMainSync(() -> connection.get().close());
        }
    }
}
