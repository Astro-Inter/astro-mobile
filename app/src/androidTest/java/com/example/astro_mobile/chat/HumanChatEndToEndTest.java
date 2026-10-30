package com.example.astro_mobile.chat;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.astro_mobile.data.api.AstroApi;
import com.example.astro_mobile.data.api.AstroApiClient;
import com.example.astro_mobile.data.api.dto.ChatContactData;
import com.example.astro_mobile.data.api.dto.ChatSendRequest;
import com.example.astro_mobile.data.api.dto.ChatSendResponse;
import com.example.astro_mobile.data.api.dto.ChatSendError;
import com.example.astro_mobile.data.chat.AndroidChatConnection;
import com.example.astro_mobile.data.chat.ChatConnectionState;
import com.example.astro_mobile.data.chat.StompChatClient;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Opt-in: duas contas explicitamente autorizadas, fornecidas por arquivo privado fora do Git.
 * Persiste três mensagens identificadas como teste e conserva a sessão padrão do usuário.
 */
@RunWith(AndroidJUnit4.class)
public final class HumanChatEndToEndTest {
    @Test public void authorizedUsersExchangeConfirmDeduplicateRecoverAndLogout() throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        var context = instrumentation.getTargetContext();
        File fixture = new File(context.getFilesDir(), "chat-e2e.properties");
        assumeTrue("Requer fixture privada de contas autorizadas", fixture.isFile());
        Properties accounts = new Properties();
        try (FileInputStream input = new FileInputStream(fixture)) { accounts.load(input); }
        FirebaseApp firstApp = FirebaseApp.initializeApp(context, FirebaseApp.getInstance().getOptions(), "chat-e2e-first");
        FirebaseApp secondApp = FirebaseApp.initializeApp(context, FirebaseApp.getInstance().getOptions(), "chat-e2e-second");
        FirebaseAuth firstAuth = FirebaseAuth.getInstance(firstApp), secondAuth = FirebaseAuth.getInstance(secondApp);
        Peer first = new Peer(), second = new Peer();
        try {
            Tasks.await(firstAuth.signInWithEmailAndPassword(accounts.getProperty("first.email"), accounts.getProperty("first.password")), 30, TimeUnit.SECONDS);
            Tasks.await(secondAuth.signInWithEmailAndPassword(accounts.getProperty("second.email"), accounts.getProperty("second.password")), 30, TimeUnit.SECONDS);
            instrumentation.runOnMainSync(() -> {
                first.connection = new AndroidChatConnection(context, firstAuth, first, () -> { });
                second.connection = new AndroidChatConnection(context, secondAuth, second, () -> { });
                first.connection.setForeground(true); first.connection.request();
                second.connection.setForeground(true); second.connection.request();
            });
            first.awaitConnection(); second.awaitConnection();
            Long firstId = first.connection.client().getAuthenticatedUserId();
            Long secondId = second.connection.client().getAuthenticatedUserId();
            assertNotNull("Servidor não informou identidade autenticada do primeiro usuário", firstId);
            assertNotNull("Servidor não informou identidade autenticada do segundo usuário", secondId);
            assertNotEquals(firstId, secondId);
            String firstBearer = bearer(firstAuth), secondBearer = bearer(secondAuth);
            AstroApi api = AstroApiClient.getApi();
            var firstContacts = api.getChatContacts(firstBearer).execute();
            var secondContacts = api.getChatContacts(secondBearer).execute();
            assertEquals("Contatos do primeiro usuário", 200, firstContacts.code());
            assertEquals("Contatos do segundo usuário", 200, secondContacts.code());
            assertNotNull(firstContacts.body()); assertNotNull(secondContacts.body());
            assertAllowed(firstContacts.body().getData(), secondId);
            assertAllowed(secondContacts.body().getData(), firstId);

            ChatSendRequest initial = new ChatSendRequest(secondId, null,
                    "[teste mobile] conexão STOMP e confirmação", UUID.randomUUID().toString());
            instrumentation.runOnMainSync(() -> assertTrue(first.connection.client().send(initial)));
            ChatSendResponse confirmation = first.awaitMessage();
            ChatSendResponse delivery = second.awaitMessage();
            assertEquals(initial.clientMessageId, confirmation.clientMessageId);
            assertEquals(confirmation.id, delivery.id);
            assertEquals(firstId, delivery.senderId);
            assertEquals(initial.content, delivery.content);
            String conversationId = confirmation.conversationId;

            // Repetição explícita da mesma operação confirma idempotência sem criar outra mensagem.
            instrumentation.runOnMainSync(() -> assertTrue(first.connection.client().send(initial)));
            assertEquals(confirmation.id, first.awaitMessage().id);
            assertEquals(confirmation.id, second.awaitMessage().id);
            var history = api.getChatMessages(secondBearer, conversationId, 30, null).execute();
            assertEquals(200, history.code()); assertNotNull(history.body());
            assertEquals(1, history.body().getData().messages.stream().filter(item -> confirmation.id.equals(item.id)).count());
            assertEquals(204, api.markChatRead(secondBearer, conversationId).execute().code());
            var readList = api.getChats(secondBearer).execute();
            assertEquals(200, readList.code()); assertNotNull(readList.body());
            assertEquals(0, readList.body().stream().filter(item -> conversationId.equals(item.id)).findFirst().get().unreadCount);

            ChatSendRequest reply = new ChatSendRequest(null, conversationId,
                    "[teste mobile] resposta pelo outro fluxo", UUID.randomUUID().toString());
            instrumentation.runOnMainSync(() -> assertTrue(second.connection.client().send(reply)));
            assertEquals(reply.clientMessageId, second.awaitMessage().clientMessageId);
            ChatSendResponse response = first.awaitMessage();
            assertEquals(secondId, response.senderId);
            assertEquals(conversationId, response.conversationId);

            instrumentation.runOnMainSync(() -> second.connection.setForeground(false));
            assertEquals(ChatConnectionState.STOPPED, second.state.get());
            ChatSendRequest offline = new ChatSendRequest(null, conversationId,
                    "[teste mobile] recuperação do histórico após reconectar", UUID.randomUUID().toString());
            instrumentation.runOnMainSync(() -> assertTrue(first.connection.client().send(offline)));
            ChatSendResponse offlineConfirmed = first.awaitMessage();
            second.connected = new CountDownLatch(1);
            instrumentation.runOnMainSync(() -> second.connection.setForeground(true));
            second.awaitConnection();
            var recovered = api.getChatMessages(secondBearer, conversationId, 30, null).execute();
            assertEquals(200, recovered.code()); assertNotNull(recovered.body());
            assertTrue(recovered.body().getData().messages.stream().anyMatch(item -> offlineConfirmed.id.equals(item.id)));

            // A assinatura recriada volta a receber, inclusive a repetição com ID estável.
            instrumentation.runOnMainSync(() -> assertTrue(first.connection.client().send(offline)));
            assertEquals(offlineConfirmed.id, first.awaitMessage().id);
            assertEquals(offlineConfirmed.id, second.awaitMessage().id);
            instrumentation.runOnMainSync(secondAuth::signOut);
            instrumentation.waitForIdleSync();
            assertEquals(ChatConnectionState.STOPPED, second.state.get());
        } finally {
            instrumentation.runOnMainSync(() -> {
                if (first.connection != null) first.connection.close();
                if (second.connection != null) second.connection.close();
                firstAuth.signOut(); secondAuth.signOut();
                firstApp.delete(); secondApp.delete();
            });
            fixture.delete();
            accounts.clear();
        }
    }

    private static void assertAllowed(List<ChatContactData> contacts, Long id) {
        assertNotNull(contacts);
        assertTrue("As duas contas precisam aparecer como contatos mutuamente autorizados", contacts.stream().anyMatch(item -> id.equals(item.id)));
    }
    private static String bearer(FirebaseAuth auth) throws Exception {
        return "Bearer " + Tasks.await(auth.getCurrentUser().getIdToken(true), 30, TimeUnit.SECONDS).getToken();
    }
    private static final class Peer implements StompChatClient.Listener {
        AndroidChatConnection connection;
        CountDownLatch connected = new CountDownLatch(1);
        final AtomicReference<ChatConnectionState> state = new AtomicReference<>();
        final LinkedBlockingQueue<ChatSendResponse> messages = new LinkedBlockingQueue<>();
        final AtomicReference<String> errorCode = new AtomicReference<>();
        @Override public void onState(ChatConnectionState value) {
            state.set(value);
            if (value == ChatConnectionState.CONNECTED) connected.countDown();
        }
        @Override public void onMessage(ChatSendResponse message) { messages.add(message); }
        @Override public void onError(ChatSendError error) { errorCode.set(error.code); }
        void awaitConnection() throws Exception {
            assertTrue("STOMP não conectou; estado=" + state.get(), connected.await(45, TimeUnit.SECONDS));
        }
        ChatSendResponse awaitMessage() throws Exception {
            ChatSendResponse event = messages.poll(30, TimeUnit.SECONDS);
            assertNotNull("Sem evento privado; erro=" + errorCode.get(), event);
            return event;
        }
    }
}
