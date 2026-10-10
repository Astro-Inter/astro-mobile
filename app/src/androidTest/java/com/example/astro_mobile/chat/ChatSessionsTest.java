package com.example.astro_mobile.chat;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.astro_mobile.MainActivity;
import com.example.astro_mobile.R;
import com.example.astro_mobile.data.ai.*;
import com.example.astro_mobile.data.firebase.IdTokenProvider;
import com.example.astro_mobile.data.local.FlowPreferences;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import okhttp3.*;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Navegação real e servidor em memória: nenhuma mensagem/token é enviado à API real. */
@RunWith(AndroidJUnit4.class)
public class ChatSessionsTest {
    private static final String ACTIVE = "c4dd8100-1122-3344-5566-77889900aabb";
    private static final String CLOSED = "c4dd8100-1122-3344-5566-77889900aacc";
    private static final String NEW = "c4dd8100-1122-3344-5566-77889900aadd";

    @Test public void leavingReturningRecreatingAndBackgroundingKeepTheSession() {
        Server server = new Server();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            ChatViewModel model = open(scenario, server);
            select(scenario, model, ACTIVE);
            send("Pergunta na mesma sessão");
            await(() -> !model.isBusy());
            onView(withId(R.id.button_chat_close)).perform(click());
            scenario.onActivity(activity -> nav(activity).navigate(R.id.action_employee_home_to_chat));
            await(() -> !model.isBusy());
            assertSession(model, ACTIVE, 4);
            scenario.recreate();
            await(() -> !model.isBusy());
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            await(() -> !model.isBusy());
            send("Outra pergunta após voltar");
            await(() -> !model.isBusy());
            assertSession(model, ACTIVE, 6);
            assertEquals(Arrays.asList(ACTIVE, ACTIVE), server.sentSessions);
            assertNoEnd(server);
        }
    }

    @Test public void earlierClosedConversationResumesOnlyBeforeSending() {
        Server server = new Server();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            ChatViewModel model = open(scenario, server);
            select(scenario, model, CLOSED);
            assertSession(model, CLOSED, 2);
            assertFalse(server.paths.contains("POST /sessions/" + CLOSED + "/iniciar"));
            send("Continuar a conversa antiga");
            await(() -> !model.isBusy());
            assertSession(model, CLOSED, 4);
            assertEquals(Collections.singletonList(CLOSED), server.sentSessions);
            assertTrue(server.paths.indexOf("POST /sessions/" + CLOSED + "/iniciar")
                    < server.paths.indexOf("POST /chat/messages"));
            onMain(() -> assertEquals("ativa", model.getState().getValue().status));
            assertNoEnd(server);
        }
    }

    @Test public void newConversationDoesNotMutateOrDeleteThePreviousOne() {
        Server server = new Server();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            ChatViewModel model = open(scenario, server);
            select(scenario, model, ACTIVE);
            onView(withId(R.id.input_chat_message)).perform(replaceText("Rascunho anterior"), closeSoftKeyboard());
            onMain(() -> assertEquals("Rascunho anterior", model.getDraft()));
            onView(withId(R.id.button_chat_sessions)).perform(click());
            onView(withId(R.id.button_ai_sessions_new)).perform(click());
            onView(withId(R.id.container_chat_welcome)).check(matches(isDisplayed()));
            onMain(() -> assertNull(model.getState().getValue().sessionId));
            assertEquals(0, server.sentSessions.size());
            assertEquals(2, server.history.get(ACTIVE).size());
            send("Primeira pergunta da nova conversa");
            await(() -> !model.isBusy());
            assertSession(model, NEW, 2);
            assertEquals(Collections.singletonList(null), server.sentSessions);
            assertEquals("ativa", server.statuses.get(ACTIVE));
            select(scenario, model, ACTIVE);
            assertSession(model, ACTIVE, 2);
            onView(withId(R.id.input_chat_message)).check(matches(withText("Rascunho anterior")));
            assertEquals(2, server.history.get(NEW).size());
            assertNoEnd(server);
        }
    }

    @Test public void failedResumeKeepsHistoryAndDoesNotSendThePendingMessage() {
        Server server = new Server();
        server.resumeCode = 503;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            ChatViewModel model = open(scenario, server);
            select(scenario, model, CLOSED);
            send("Mensagem a recuperar");
            await(() -> !model.isBusy());
            onMain(() -> {
                ChatViewModel.State state = model.getState().getValue();
                assertEquals(ChatFailure.SERVER, state.failure);
                assertEquals(CLOSED, state.sessionId);
                assertEquals(3, state.messages.size());
                assertTrue(state.messages.get(2).hasFailed());
            });
            assertTrue(server.sentSessions.isEmpty());
            assertEquals(2, server.history.get(CLOSED).size());
            // Recuperação consulta o histórico; somente um retry manual posterior envia.
            onMain(model::retry);
            await(() -> !model.isBusy());
            assertTrue(server.sentSessions.isEmpty());
            server.resumeCode = 200;
            onMain(model::retry);
            await(() -> !model.isBusy());
            assertSession(model, CLOSED, 4);
            assertEquals(1, server.sentSessions.size());
            assertNoEnd(server);
        }
    }

    @Test public void invalidResumeAndConflictNeverSendOrReplaceTheSession() {
        for (int code : new int[]{403, 404, 409}) {
            Server server = new Server(); server.resumeCode = code;
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                ChatViewModel model = open(scenario, server);
                select(scenario, model, CLOSED);
                send("Preservar minha mensagem");
                await(() -> !model.isBusy());
                onMain(() -> {
                    assertEquals(CLOSED, model.getState().getValue().sessionId);
                    assertNotNull(model.getState().getValue().failure);
                });
                assertTrue(server.sentSessions.isEmpty());
                assertNoEnd(server);
            }
        }
    }

    @Test public void closingTheApplicationKeepsTheConversationInRemoteHistory() {
        Server server = new Server();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            ChatViewModel model = open(scenario, server);
            select(scenario, model, ACTIVE);
            send("Mensagem antes de fechar");
            await(() -> !model.isBusy());
        }
        assertNoEnd(server);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            ChatViewModel model = open(scenario, server);
            select(scenario, model, ACTIVE);
            assertSession(model, ACTIVE, 4);
            send("Mensagem após abrir novamente");
            await(() -> !model.isBusy());
            assertSession(model, ACTIVE, 6);
            assertEquals(Arrays.asList(ACTIVE, ACTIVE), server.sentSessions);
        }
        assertNoEnd(server);
    }

    private ChatViewModel open(ActivityScenario<MainActivity> scenario, Server server) {
        AtomicReference<ChatViewModel> model = new AtomicReference<>();
        scenario.onActivity(activity -> {
            FlowPreferences.clear(activity);
            NavController navigation = nav(activity);
            NavGraph graph = navigation.getNavInflater().inflate(R.navigation.nav_graph);
            graph.setStartDestination(R.id.employeeHomeFragment);
            navigation.setGraph(graph);
            model.set(new ViewModelProvider(navigation.getBackStackEntry(R.id.employeeHomeFragment),
                    new ViewModelProvider.Factory() {
                        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> cls) {
                            IdTokenProvider tokens = (force, callback) -> callback.onToken("local-test-token");
                            return cls.cast(new ChatViewModel(new AiChatRepository(server.api, tokens),
                                    new AiSessionsRepository(server.api, tokens), null));
                        }
                    }).get(ChatViewModel.class));
            navigation.navigate(R.id.action_employee_home_to_chat);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        return model.get();
    }

    private void select(ActivityScenario<MainActivity> scenario, ChatViewModel model, String id) {
        onView(withId(R.id.button_chat_sessions)).perform(click());
        await(() -> !model.getListState().getValue().loading);
        onView(withText(id.equals(ACTIVE) ? "Conversa anterior" : "Conversa antiga encerrada")).perform(click());
        await(() -> !model.isBusy());
        onView(withId(R.id.input_chat_message)).check(matches(isDisplayed()));
        onView(withText("Encerrar conversa")).check(doesNotExist());
    }

    private void send(String text) {
        onView(withId(R.id.input_chat_message)).perform(replaceText(text), closeSoftKeyboard());
        onView(withId(R.id.button_chat_send)).perform(click());
    }

    private void assertSession(ChatViewModel model, String id, int count) {
        onMain(() -> {
            assertEquals(id, model.getState().getValue().sessionId);
            assertEquals(count, model.getState().getValue().messages.size());
            assertNull(model.getState().getValue().failure);
        });
    }

    private void assertNoEnd(Server server) {
        for (String path : server.paths) {
            assertFalse("Chamada proibida: " + path, path.endsWith("/encerrar"));
            assertFalse("Não deve apagar conversas", path.startsWith("DELETE "));
        }
    }

    private void await(BooleanSupplier done) {
        long end = SystemClock.uptimeMillis() + 15_000;
        AtomicReference<Boolean> finished = new AtomicReference<>(false);
        while (SystemClock.uptimeMillis() < end) {
            onMain(() -> finished.set(done.getAsBoolean()));
            if (finished.get()) { InstrumentationRegistry.getInstrumentation().waitForIdleSync(); return; }
            SystemClock.sleep(50);
        }
        fail("A operação simulada não terminou");
    }

    private static void onMain(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    private NavController nav(MainActivity activity) {
        return ((NavHostFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment)).getNavController();
    }

    private static final class Server {
        final Gson gson = new Gson();
        final Map<String, List<Map<String, String>>> history = new LinkedHashMap<>();
        final Map<String, String> statuses = new LinkedHashMap<>();
        final List<String> paths = Collections.synchronizedList(new ArrayList<>());
        final List<String> sentSessions = Collections.synchronizedList(new ArrayList<>());
        final AiChatApi api;
        volatile int resumeCode = 200;

        Server() {
            history.put(ACTIVE, new ArrayList<>(Arrays.asList(message("user", "Pergunta anterior"), message("assistant", "Resposta anterior"))));
            history.put(CLOSED, new ArrayList<>(Arrays.asList(message("user", "Pergunta antiga"), message("assistant", "Resposta antiga"))));
            statuses.put(ACTIVE, "ativa"); statuses.put(CLOSED, "encerrada");
            OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
                Request request = chain.request();
                assertEquals("Bearer local-test-token", request.header("Authorization"));
                String path = request.url().encodedPath();
                paths.add(request.method() + " " + path);
                Object body; int code = 200;
                synchronized (this) {
                    if (path.equals("/sessions")) {
                        List<Map<String, String>> rows = new ArrayList<>();
                        for (String id : history.keySet()) {
                            Map<String, String> row = new LinkedHashMap<>();
                            row.put("session_id", id); row.put("status", statuses.get(id));
                            row.put("title", id.equals(ACTIVE) ? "Conversa anterior" : id.equals(CLOSED) ? "Conversa antiga encerrada" : "Nova conversa criada");
                            row.put("last_message_preview", history.get(id).get(history.get(id).size() - 1).get("content"));
                            row.put("created_at", "2026-10-08T10:00:00Z"); row.put("updated_at", "2026-10-10T10:00:00Z");
                            rows.add(row);
                        }
                        body = Collections.singletonMap("sessions", rows);
                    } else if (path.endsWith("/messages") && path.startsWith("/sessions/")) {
                        String id = path.split("/")[2];
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("session_id", id); result.put("status", statuses.get(id));
                        result.put("total", history.get(id).size()); result.put("mensagens", history.get(id));
                        body = result;
                    } else if (path.endsWith("/iniciar")) {
                        String id = path.split("/")[2]; code = resumeCode;
                        if (code == 200) statuses.put(id, "ativa");
                        Map<String, String> result = new LinkedHashMap<>();
                        result.put("session_id", id); result.put("status", statuses.get(id)); body = result;
                    } else if (path.equals("/chat/messages")) {
                        Buffer buffer = new Buffer(); request.body().writeTo(buffer);
                        JsonObject data = gson.fromJson(buffer.readUtf8(), JsonObject.class);
                        String id = data.has("session_id") ? data.get("session_id").getAsString() : null;
                        sentSessions.add(id);
                        if (id == null) { id = NEW; history.put(id, new ArrayList<>()); statuses.put(id, "ativa"); }
                        assertEquals("ativa", statuses.get(id));
                        history.get(id).add(message("user", data.get("message").getAsString()));
                        history.get(id).add(message("assistant", "Resposta simulada"));
                        Map<String, String> result = new LinkedHashMap<>();
                        result.put("session_id", id); result.put("resposta", "Resposta simulada"); body = result;
                    } else throw new java.io.IOException("Endpoint inesperado: " + path);
                    return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                            .code(code).message("local-test")
                            .body(ResponseBody.create(MediaType.get("application/json"), gson.toJson(body))).build();
                }
            }).build();
            api = new Retrofit.Builder().baseUrl("https://example.com/").client(http)
                    .addConverterFactory(GsonConverterFactory.create()).build().create(AiChatApi.class);
        }

        private static Map<String, String> message(String role, String content) {
            Map<String, String> result = new LinkedHashMap<>();
            result.put("role", role); result.put("content", content); return result;
        }
    }
}
