package com.example.astro_mobile.navigation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.graphics.Bitmap;
import android.os.SystemClock;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.astro_mobile.MainActivity;
import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.EmailVerificationViewModel;
import com.example.astro_mobile.data.api.AstroApi;
import com.example.astro_mobile.data.api.EmailVerificationRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Confere as telas reais com falhas locais, sem acessar a API ou alterar a sessão. */
@RunWith(AndroidJUnit4.class)
public class ErrorScreensTest {
    private static final String EMAIL = "ui-error@example.com";

    @Test public void connectionErrorRetriesAndReturnsToEmail() throws Exception {
        checkError(true, R.id.connectionErrorFragment,
                R.string.email_connection_error_title, "erro-conexao");
    }

    @Test public void genericErrorRetriesAndReturnsToEmail() throws Exception {
        checkError(false, R.id.genericErrorFragment,
                R.string.email_generic_error_title, "erro-generico");
    }

    private void checkError(boolean offline, int destination, int title, String screenshot)
            throws Exception {
        AtomicInteger calls = new AtomicInteger();
        EmailVerificationRepository repository = localRepository(offline, calls);
        AtomicReference<NavController> navigation = new AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Abre a identificação com o repositório local, sem passar pela sessão da splash.
            scenario.onActivity(activity -> {
                NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(host);
                NavController controller = host.getNavController();
                activity.getViewModelStore().clear();
                new ViewModelProvider(activity, new EmailVerificationViewModel.Factory(repository))
                        .get(EmailVerificationViewModel.class);
                NavGraph graph = controller.getNavInflater().inflate(R.navigation.nav_graph);
                graph.setStartDestination(R.id.emailIdentificationFragment);
                controller.setGraph(graph);
                navigation.set(controller);
            });
            onView(withId(R.id.input_email_identification))
                    .perform(replaceText(EMAIL), closeSoftKeyboard());
            onView(withId(R.id.button_email_identification_continue)).perform(click());
            awaitDestination(navigation, destination);
            onView(withId(R.id.text_email_error_title)).check(matches(withText(title)));
            onView(withId(R.id.button_email_error_start)).check(matches(isDisplayed()));
            takeScreenshot(screenshot);

            // A nova tentativa repete a consulta e libera o botão após a mesma falha.
            onView(withId(R.id.button_email_error_retry)).perform(click());
            awaitCalls(calls, 2);
            awaitIdleRequest(scenario);
            onView(withId(R.id.text_email_error_title)).check(matches(withText(title)));
            onView(withId(R.id.button_email_error_retry)).check(matches(isEnabled()));
            assertEquals(2, calls.get());

            onView(withId(R.id.button_email_error_start)).perform(click());
            awaitDestination(navigation, R.id.emailIdentificationFragment);
            onView(withId(R.id.input_email_identification)).check(matches(withText(EMAIL)));
        }
    }

    private EmailVerificationRepository localRepository(boolean offline, AtomicInteger calls) {
        // Intercepta antes da rede: uma falha de conexão ou uma resposta HTTP 500.
        OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
            calls.incrementAndGet();
            if (offline) {
                throw new UnknownHostException("Falha local para validar a tela");
            }
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(500).message("Local UI test")
                    .body(ResponseBody.create(MediaType.get("application/json"), "{}"))
                    .build();
        }).build();
        AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/").client(http)
                .addConverterFactory(GsonConverterFactory.create()).build().create(AstroApi.class);
        return new EmailVerificationRepository(api);
    }

    private void awaitDestination(AtomicReference<NavController> navigation, int expected) {
        long deadline = SystemClock.uptimeMillis() + 5_000;
        AtomicInteger current = new AtomicInteger();
        while (SystemClock.uptimeMillis() < deadline) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    current.set(navigation.get().getCurrentDestination().getId()));
            if (current.get() == expected) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                return;
            }
            SystemClock.sleep(50);
        }
        fail("A navegação não chegou à tela esperada");
    }

    private void awaitCalls(AtomicInteger calls, int expected) {
        long deadline = SystemClock.uptimeMillis() + 5_000;
        while (calls.get() < expected && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(50);
        }
        assertEquals(expected, calls.get());
    }

    private void awaitIdleRequest(ActivityScenario<MainActivity> scenario) {
        long deadline = SystemClock.uptimeMillis() + 5_000;
        AtomicReference<Boolean> loading = new AtomicReference<>(true);
        while (SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> loading.set(new ViewModelProvider(activity)
                    .get(EmailVerificationViewModel.class).isLoading()));
            if (!loading.get()) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                return;
            }
            SystemClock.sleep(50);
        }
        fail("A tentativa não terminou");
    }

    private void takeScreenshot(String name) throws Exception {
        // Aguarda o término da transição antes de registrar a tela do aparelho.
        SystemClock.sleep(500);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .takeScreenshot();
        assertNotNull(bitmap);
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getExternalFilesDir("error-ui-qa");
        assertNotNull(directory);
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } finally {
            bitmap.recycle();
        }
    }
}
