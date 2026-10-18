package com.example.astro_mobile.data.api;

import static org.junit.Assert.*;

import com.example.astro_mobile.data.api.dto.NotificationData;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import org.junit.Test;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class NotificationsRepositoryTest {
    private static final String SUCCESS = "{\"success\":true,\"data\":["
            + "{\"id\":\"new-id\",\"titulo\":\"Validações pendentes\",\"viewed\":false,\"horas\":0},"
            + "{\"id\":\"old-id\",\"titulo\":\"Treinamento de EPI\",\"viewed\":true,\"horas\":48}]}";

    @Test
    public void loadsAuthenticatedContractAndPreservesOrder() throws Exception {
        Result result = load(repository(chain -> {
            assertEquals("GET", chain.request().method());
            assertEquals("/notifications", chain.request().url().encodedPath());
            assertEquals("Bearer token", chain.request().header("Authorization"));
            assertNull(chain.request().url().query());
            return response(chain, 200, SUCCESS);
        }, tokens()));
        assertNull(result.failure);
        assertEquals(2, result.items.size());
        assertEquals("new-id", result.items.get(0).getId());
        assertEquals("Validações pendentes", result.items.get(0).getTitle());
        assertFalse(result.items.get(0).isViewed());
        assertEquals(0, result.items.get(0).getHours());
        assertEquals("old-id", result.items.get(1).getId());
        assertTrue(result.items.get(1).isViewed());
        assertEquals(48, result.items.get(1).getHours());
    }

    @Test
    public void emptyListIsSuccessButInvalidResponsesAreErrors() throws Exception {
        Result empty = load(repository(chain -> response(chain, 200,
                "{\"success\":true,\"data\":[]}"), tokens()));
        assertNull(empty.failure);
        assertTrue(empty.items.isEmpty());
        for (String json : new String[]{"{}", "", "<html>unavailable</html>",
                "{\"success\":true,\"data\":null}", "{\"success\":true,\"data\":[null]}",
                "{\"success\":true,\"data\":[{}]}", SUCCESS.replace("true,\"data", "false,\"data"),
                SUCCESS.replace("\"horas\":0", "\"horas\":-1"),
                SUCCESS.replace("\"viewed\":false", "\"viewed\":null")}) {
            Result invalid = load(repository(chain -> response(chain, 200, json), tokens()));
            assertEquals(FailureKind.INTERNAL, invalid.failure);
            assertNull(invalid.items);
        }
    }

    @Test
    public void refreshesUnauthorizedTokenOnlyOnce() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        List<Boolean> refreshes = new ArrayList<>();
        Result result = load(repository(chain -> {
            int index = calls.getAndIncrement();
            assertEquals(index == 0 ? "Bearer token" : "Bearer refreshed",
                    chain.request().header("Authorization"));
            return response(chain, index == 0 ? 401 : 200, SUCCESS);
        }, (refresh, callback) -> {
            refreshes.add(refresh);
            callback.onToken(refresh ? "refreshed" : "token");
        }));
        assertNull(result.failure);
        assertEquals(Arrays.asList(false, true), refreshes);
        calls.set(0);
        Result denied = load(repository(chain -> {
            calls.incrementAndGet();
            return response(chain, 401, "{}");
        }, tokens()));
        assertEquals(FailureKind.SESSION, denied.failure);
        assertEquals(2, calls.get());
    }

    @Test
    public void httpAndTransportFailuresNeverBecomeAnEmptyList() throws Exception {
        for (int code : new int[]{403, 404, 500, 503}) {
            Result result = load(repository(chain -> response(chain, code, "{}"), tokens()));
            assertEquals(code == 403 || code == 404 ? FailureKind.BUSINESS : FailureKind.INTERNAL,
                    result.failure);
            assertNull(result.items);
        }
        assertEquals(FailureKind.CONNECTION, load(repository(chain -> {
            throw new UnknownHostException();
        }, tokens())).failure);
        assertEquals(FailureKind.INTERNAL, load(repository(chain -> {
            throw new java.net.SocketTimeoutException();
        }, tokens())).failure);
    }

    @Test
    public void tokenFailuresDoNotSendHttp() throws Exception {
        for (IdTokenProvider.Failure failure : IdTokenProvider.Failure.values()) {
            Result result = load(repository(chain -> {
                fail("Não pode consultar notificações sem token");
                return response(chain, 200, SUCCESS);
            }, (refresh, callback) -> callback.onFailure(failure)));
            assertEquals(FailureKind.valueOf(failure.name()), result.failure);
        }
    }

    @Test
    public void cancellationDiscardsLateTokenAndAllowsNewRequest() throws Exception {
        List<IdTokenProvider.Callback> pending = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        NotificationsRepository repository = repository(chain -> {
            calls.incrementAndGet();
            return response(chain, 200, SUCCESS);
        }, (refresh, callback) -> pending.add(callback));
        Result previous = new Result();
        repository.load(previous);
        repository.cancel();
        Result current = new Result();
        repository.load(current);
        pending.get(0).onToken("old-token");
        pending.get(0).onFailure(IdTokenProvider.Failure.SESSION);
        pending.get(1).onToken("new-token");
        assertTrue(current.completed.await(5, TimeUnit.SECONDS));
        assertNotNull(current.items);
        assertEquals(1, previous.completed.getCount());
        assertEquals(1, calls.get());
    }

    @Test
    public void cancellationDiscardsInFlightHttpResult() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Runnable> delivery = new AtomicReference<>();
        CountDownLatch queued = new CountDownLatch(1);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            started.countDown();
            try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
            catch (InterruptedException error) { throw new java.io.IOException(error); }
            return response(chain, 200, SUCCESS);
        }).build();
        AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/").client(client)
                .callbackExecutor(task -> { delivery.set(task); queued.countDown(); })
                .addConverterFactory(GsonConverterFactory.create()).build().create(AstroApi.class);
        NotificationsRepository repository = new NotificationsRepository(api, tokens());
        Result result = new Result();
        repository.load(result);
        assertTrue(started.await(5, TimeUnit.SECONDS));
        repository.cancel();
        release.countDown();
        assertTrue(queued.await(5, TimeUnit.SECONDS));
        delivery.get().run();
        assertEquals(1, result.completed.getCount());
    }

    private IdTokenProvider tokens() {
        return (refresh, callback) -> callback.onToken(refresh ? "refreshed" : "token");
    }

    private NotificationsRepository repository(Interceptor interceptor, IdTokenProvider tokens) {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/").client(client)
                .callbackExecutor(Runnable::run).addConverterFactory(GsonConverterFactory.create())
                .build().create(AstroApi.class);
        return new NotificationsRepository(api, tokens);
    }

    private Response response(Interceptor.Chain chain, int code, String json) {
        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("test")
                .body(ResponseBody.create(MediaType.get("application/json"), json)).build();
    }

    private Result load(NotificationsRepository repository) throws Exception {
        Result result = new Result();
        repository.load(result);
        assertTrue("A consulta não terminou", result.completed.await(5, TimeUnit.SECONDS));
        return result;
    }

    private static final class Result implements NotificationsRepository.ResultCallback {
        final CountDownLatch completed = new CountDownLatch(1);
        List<NotificationData> items;
        FailureKind failure;
        @Override public void onSuccess(List<NotificationData> items) { this.items = items; completed.countDown(); }
        @Override public void onFailure(FailureKind failure) { this.failure = failure; completed.countDown(); }
    }
}
