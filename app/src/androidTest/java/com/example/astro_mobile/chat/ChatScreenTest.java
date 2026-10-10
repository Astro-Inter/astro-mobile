package com.example.astro_mobile.chat;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.astro_mobile.MainActivity;
import com.example.astro_mobile.R;
import com.example.astro_mobile.data.ai.AiChatApi;
import com.example.astro_mobile.data.ai.AiChatRepository;
import com.example.astro_mobile.data.ai.ChatFailure;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Usa respostas locais, sem tokens reais nem chamadas à API de IA. */
@RunWith(AndroidJUnit4.class)
public class ChatScreenTest {
    private static final String MARKDOWN = "## Normas Regulamentadoras\n\n"
            + "As **NRs** orientam a segurança no trabalho.\n\n"
            + "- Proteção das pessoas\n- Prevenção de acidentes\n\n"
            + "> Confira as normas aplicáveis à sua atividade.\n\n"
            + "| Norma | Tema | Status | Responsável |\n| :--- | :---: | ---: | --- |\n"
            + "| **NR 6** | EPI | OK | Equipe de segurança |\n| NR 10 | Eletricidade | Revisar | Manutenção |\n\n"
            + "Acesse [a plataforma](https://example.com). Use `Astro`.";

    @Test public void markdownLoadingDraftAndRecreation() throws Exception {
        Fixture f = new Fixture(200);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<ChatViewModel> model = open(scenario, f.repository());
            onView(withId(R.id.container_chat_welcome)).check(matches(isDisplayed()));
            screenshot("welcome");
            onView(withId(R.id.input_chat_message)).perform(replaceText("O que são NRs?"), closeSoftKeyboard());
            onView(withId(R.id.button_chat_send)).perform(click());
            onView(withId(R.id.container_chat_loading)).check(matches(isDisplayed()));
            onView(withId(R.id.button_chat_send)).check(matches(not(isEnabled())));
            onView(withId(R.id.input_chat_message)).perform(replaceText("Minha próxima pergunta"), closeSoftKeyboard());
            f.gate.countDown();
            awaitFinished(model);
            screenshot("markdown");
            onView(withText("Norma")).check(matches(isDisplayed()));
            scenario.onActivity(activity -> {
                ChatViewModel.State state = model.get().getState().getValue();
                assertEquals(2, state.messages.size());
                assertFalse(state.messages.get(1).isFromUser());
                assertEquals(MARKDOWN, state.messages.get(1).getText());
            });
            scenario.recreate();
            onView(withId(R.id.input_chat_message)).check(matches(withText("Minha próxima pergunta")));
            onView(withId(R.id.button_chat_send)).check(matches(isEnabled()));
        }
    }

    @Test public void retryKeepsOneOutgoingMessage() throws Exception {
        Fixture f = new Fixture(503);
        f.gate.countDown();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<ChatViewModel> model = open(scenario, f.repository());
            onView(withId(R.id.button_chat_suggestion_nrs)).perform(click());
            awaitFinished(model);
            onView(withId(R.id.text_chat_error)).check(matches(withText(R.string.chat_server_error)));
            screenshot("error");
            f.code = 200;
            onView(withId(R.id.button_chat_retry)).perform(click());
            onView(withText(R.string.ai_sessions_resend)).perform(click());
            awaitFinished(model);
            scenario.onActivity(activity -> {
                assertEquals(2, model.get().getState().getValue().messages.size());
                assertEquals(2, f.calls.get());
                assertNull(model.get().getState().getValue().failure);
            });
        }
    }

    @Test public void missingFirebaseSessionOffersSignIn() throws Exception {
        Fixture f = new Fixture(200);
        AiChatRepository repository = new AiChatRepository(f.api,
                (force, callback) -> callback.onFailure(com.example.astro_mobile.data.firebase.IdTokenProvider.Failure.SESSION));
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<ChatViewModel> model = open(scenario, repository);
            onView(withId(R.id.input_chat_message)).perform(replaceText("Olá"));
            onView(withId(R.id.button_chat_send)).perform(click());
            awaitFinished(model);
            onView(withId(R.id.button_chat_retry)).check(matches(withText(R.string.chat_sign_in)));
            scenario.onActivity(activity -> assertEquals(ChatFailure.SESSION, model.get().getState().getValue().failure));
            assertEquals(0, f.calls.get());
        }
    }

    @Test public void lengthFilterKeepsUnicodeCharactersWhole() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            EditText input = new EditText(InstrumentationRegistry.getInstrumentation().getTargetContext());
            input.setFilters(new android.text.InputFilter[]{new CodePointLengthFilter(4)});
            input.setText("😀😀😀😀😀");
            assertEquals("😀😀😀😀", input.getText().toString());
        });
    }

    @Test public void calendarMarkersBecomeEndpointLinksOutsideCode() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            io.noties.markwon.Markwon markdown = ChatMarkdown.create(context);
            org.commonmark.node.Node document = markdown.parse(
                    "[Conectar minha agenda](google-calendar-conectar)\n\n"
                            + "[google-calendar-conectar](Autorizar o Google Calendar)\n\n"
                            + "`[google-calendar-conectar](Exemplo em código)`\n\n"
                            + "[Site](https://example.com)");
            ChatMarkdown.normalizeCalendarLinks(document);
            java.util.List<String> destinations = new java.util.ArrayList<>();
            java.util.List<String> labels = new java.util.ArrayList<>();
            document.accept(new org.commonmark.node.AbstractVisitor() {
                @Override public void visit(org.commonmark.node.Link link) {
                    destinations.add(link.getDestination());
                    labels.add(((org.commonmark.node.Text) link.getFirstChild()).getLiteral());
                }
            });
            assertEquals(java.util.Arrays.asList(ChatMarkdown.CALENDAR_URL, ChatMarkdown.CALENDAR_URL,
                    "https://example.com"), destinations);
            assertEquals(java.util.Arrays.asList("Conectar minha agenda", "Autorizar o Google Calendar", "Site"), labels);
            assertTrue(markdown.render(document).toString().contains("[google-calendar-conectar](Exemplo em código)"));
        });
    }

    @Test public void tablesPreserveAlignmentFormattingAndHorizontalScrolling() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            android.widget.LinearLayout content = new android.widget.LinearLayout(context);
            content.setOrientation(android.widget.LinearLayout.VERTICAL);
            String source = "Antes\n\n| Norma | Tema | Status | Responsável |\n"
                    + "| :--- | :---: | ---: | --- |\n"
                    + "| **NR 6** | EPI | OK | Equipe de segurança do trabalho |\n\n"
                    + "Depois\n\n| Exemplo | Valor |\n| --- | --- |\n| A\\|B | `código` |\n\n"
                    + "```\n| literal | código |\n| --- | --- |\n```";
            ChatMarkdownContent.render(content, ChatMarkdown.create(context), source);
            assertEquals(5, content.getChildCount());
            assertTrue(content.getChildAt(0) instanceof android.widget.TextView);
            assertEquals("Antes", ((android.widget.TextView) content.getChildAt(0)).getText().toString());
            android.widget.HorizontalScrollView scroll = (android.widget.HorizontalScrollView) content.getChildAt(1);
            int viewport = Math.round(280 * context.getResources().getDisplayMetrics().density);
            content.measure(android.view.View.MeasureSpec.makeMeasureSpec(viewport, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED));
            content.layout(0, 0, viewport, content.getMeasuredHeight());
            android.widget.TableLayout table = (android.widget.TableLayout) scroll.getChildAt(0);
            assertTrue(table.getWidth() > scroll.getWidth());
            assertTrue(table.getHeight() > 0);
            android.widget.TableRow row = (android.widget.TableRow) table.getChildAt(1);
            assertEquals(4, row.getChildCount());
            android.widget.TextView bold = (android.widget.TextView) row.getChildAt(0);
            assertEquals("NR 6", bold.getText().toString());
            assertTrue(((android.text.Spanned) bold.getText()).getSpans(0, 4,
                    io.noties.markwon.core.spans.StrongEmphasisSpan.class).length > 0);
            assertEquals(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL,
                    ((android.widget.TextView) row.getChildAt(1)).getGravity());
            assertEquals(android.view.Gravity.TOP | android.view.Gravity.RIGHT,
                    ((android.widget.TextView) row.getChildAt(2)).getGravity());
            android.widget.TableLayout second = (android.widget.TableLayout)
                    ((android.widget.HorizontalScrollView) content.getChildAt(3)).getChildAt(0);
            assertEquals("A|B", ((android.widget.TextView) ((android.widget.TableRow) second.getChildAt(1))
                    .getChildAt(0)).getText().toString());
            ChatMarkdownContent.render(content, ChatMarkdown.create(context), "Resposta sem tabela");
            assertEquals(1, content.getChildCount());
        });
    }

    private AtomicReference<ChatViewModel> open(ActivityScenario<MainActivity> scenario, AiChatRepository repository) {
        AtomicReference<ChatViewModel> model = new AtomicReference<>();
        scenario.onActivity(activity -> {
            NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            NavController navigation = host.getNavController();
            androidx.navigation.NavGraph graph = navigation.getNavInflater().inflate(R.navigation.nav_graph);
            graph.setStartDestination(R.id.employeeHomeFragment);
            navigation.setGraph(graph);
            model.set(new ViewModelProvider(navigation.getBackStackEntry(R.id.employeeHomeFragment),
                    new ViewModelProvider.Factory() {
                        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> cls) {
                            return cls.cast(new ChatViewModel(repository));
                        }
                    }).get(ChatViewModel.class));
            navigation.navigate(R.id.action_employee_home_to_chat);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        return model;
    }

    private void awaitFinished(AtomicReference<ChatViewModel> model) {
        AtomicReference<Boolean> loading = new AtomicReference<>(true);
        long end = SystemClock.uptimeMillis() + 8_000;
        while (SystemClock.uptimeMillis() < end) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> loading.set(model.get().getState().getValue().loading));
            if (!loading.get()) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                return;
            }
            SystemClock.sleep(50);
        }
        fail("A resposta local não foi entregue");
    }

    private void screenshot(String name) throws Exception {
        // O ListAdapter aplica o diff depois da emissão do estado do ViewModel.
        SystemClock.sleep(300);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("chat-qa");
        assertNotNull(directory);
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } finally { bitmap.recycle(); }
    }

    private static final class Fixture {
        final CountDownLatch gate = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger();
        final AiChatApi api;
        volatile int code;

        Fixture(int code) {
            this.code = code;
            OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
                calls.incrementAndGet();
                try { if (!gate.await(8, TimeUnit.SECONDS)) throw new java.io.IOException("test timeout"); }
                catch (InterruptedException error) { throw new java.io.IOException(error); }
                java.util.Map<String, String> payload = new java.util.HashMap<>();
                payload.put("session_id", "c4dd8100-1122-3344-5566-77889900aabb");
                payload.put("resposta", MARKDOWN);
                String body = new com.google.gson.Gson().toJson(payload);
                return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                        .code(this.code).message("test")
                        .body(ResponseBody.create(MediaType.get("application/json"), body)).build();
            }).build();
            api = new Retrofit.Builder().baseUrl("https://example.com/").client(http)
                    .addConverterFactory(GsonConverterFactory.create()).build().create(AiChatApi.class);
        }

        AiChatRepository repository() {
            return new AiChatRepository(api, (force, callback) -> callback.onToken("local-test-token"));
        }
    }
}
