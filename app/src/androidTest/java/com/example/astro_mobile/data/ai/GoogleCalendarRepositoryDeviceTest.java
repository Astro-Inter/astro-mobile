package com.example.astro_mobile.data.ai;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.astro_mobile.data.firebase.IdTokenProvider;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import static org.junit.Assert.*;

/** Intercepta HTTP localmente; não acessa Firebase nem abre o navegador. */
@RunWith(AndroidJUnit4.class)
public class GoogleCalendarRepositoryDeviceTest {
    @Test public void refreshesTokenOnceAndDeliversAuthorization() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        AtomicInteger refreshes = new AtomicInteger();
        AiChatApi api = api(requests, true);
        IdTokenProvider tokens = (refresh, callback) -> {
            if (refresh) refreshes.incrementAndGet();
            callback.onToken("fixture-token");
        };
        AtomicReference<GoogleCalendarRepository> repository = new AtomicReference<>();
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        onMain(() -> {
            repository.set(new GoogleCalendarRepository(api, tokens));
            assertTrue(repository.get().connect((url, failure) -> {
                assertNull(failure);
                result.set(url);
                completed.countDown();
            }));
        });
        try {
            assertTrue("Calendar callback must finish", completed.await(5, TimeUnit.SECONDS));
            assertEquals("https://accounts.example.com/authorize", result.get());
            assertEquals(2, requests.get());
            assertEquals(1, refreshes.get());
        } finally { onMain(() -> repository.get().close()); }
    }

    @Test public void blocksDuplicateClickAndDiscardsTokenAfterClosing() {
        AtomicInteger requests = new AtomicInteger();
        AtomicInteger results = new AtomicInteger();
        AtomicReference<IdTokenProvider.Callback> pending = new AtomicReference<>();
        onMain(() -> {
            GoogleCalendarRepository repository = new GoogleCalendarRepository(api(requests, false),
                    (refresh, callback) -> pending.set(callback));
            assertTrue(repository.connect((url, failure) -> results.incrementAndGet()));
            assertFalse(repository.connect((url, failure) -> results.incrementAndGet()));
            repository.close();
            pending.get().onToken("late-fixture-token");
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(0, requests.get());
        assertEquals(0, results.get());
    }

    @Test public void rejectsSessionWithoutMakingHttpRequest() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<GoogleCalendarRepository.Failure> failure = new AtomicReference<>();
        AtomicReference<GoogleCalendarRepository> repository = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        onMain(() -> {
            repository.set(new GoogleCalendarRepository(api(requests, false),
                    (refresh, callback) -> callback.onFailure(IdTokenProvider.Failure.SESSION)));
            repository.get().connect((url, error) -> {
                assertNull(url);
                failure.set(error);
                completed.countDown();
            });
        });
        try {
            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertEquals(GoogleCalendarRepository.Failure.SESSION, failure.get());
            assertEquals(0, requests.get());
        } finally { onMain(() -> repository.get().close()); }
    }

    private AiChatApi api(AtomicInteger requests, boolean firstUnauthorized) {
        OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
            int attempt = requests.incrementAndGet();
            int status = firstUnauthorized && attempt == 1 ? 401 : 200;
            return new okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(status).message("Local fixture")
                    .body(ResponseBody.create(MediaType.get("application/json"), status == 200
                            ? "{\"authorization_url\":\"https://accounts.example.com/authorize\"}" : "{}"))
                    .build();
        }).build();
        return new Retrofit.Builder().baseUrl("https://example.com/").client(http)
                .addConverterFactory(GsonConverterFactory.create()).build().create(AiChatApi.class);
    }

    private void onMain(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }
}
