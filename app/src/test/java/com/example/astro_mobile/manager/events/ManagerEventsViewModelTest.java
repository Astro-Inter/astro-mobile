package com.example.astro_mobile.manager.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.astro_mobile.data.api.AstroApi;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.ManagerEventsRepository;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import org.junit.Test;

import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ManagerEventsViewModelTest {
    private static final String EVENTS = "{\"success\":true,\"data\":["
            + "{\"id\":1,\"title\":\"Validação\",\"status\":\"ATIVO\",\"completionMode\":\"COLABORADOR\",\"pendingCount\":2},"
            + "{\"id\":2,\"title\":\"Atribuição\",\"status\":\"ATIVO\",\"completionMode\":\"GESTOR\",\"pendingCount\":1},"
            + "{\"id\":3,\"title\":\"Sem ação\",\"status\":\"ATIVO\",\"completionMode\":\"GESTOR\",\"pendingCount\":0},"
            + "{\"id\":4,\"title\":\"Finalizado\",\"status\":\"CONCLUIDO\",\"completionMode\":\"COLABORADOR\",\"pendingCount\":1},"
            + "{\"id\":5,\"title\":\"Cancelado\",\"status\":\"CANCELADO\",\"completionMode\":\"GESTOR\",\"pendingCount\":2}]}";

    @Test public void mapsStatesAndFiltersWithoutShowingCancelledAsInProgress() throws Exception {
        ManagerEventsViewModel model = model(200, EVENTS);
        Result result = new Result();
        model.load(result);
        result.await();
        assertNull(result.failure);
        assertEquals(5, model.visibleEvents().size());
        assertEquals(ManagerEvent.Status.VALIDATION_PENDING, model.visibleEvents().get(0).status);
        assertEquals(2L, model.visibleEvents().get(0).pendingValidations);
        assertEquals(ManagerEvent.Status.ASSIGNMENT_PENDING, model.visibleEvents().get(1).status);
        assertEquals(ManagerEvent.Status.NO_ACTION, model.visibleEvents().get(2).status);
        assertEquals(ManagerEvent.Status.FINISHED, model.visibleEvents().get(3).status);
        assertEquals(ManagerEvent.Status.CANCELLED, model.visibleEvents().get(4).status);
        model.setFilter(ManagerEventsViewModel.Filter.IN_PROGRESS);
        assertEquals(3, model.visibleEvents().size());
        model.setFilter(ManagerEventsViewModel.Filter.FINISHED);
        assertEquals(4L, model.visibleEvents().get(0).id);
        assertEquals(1, model.visibleEvents().size());
        model.setFilter(ManagerEventsViewModel.Filter.ALL);
        model.setQuery("validacao");
        assertEquals(1L, model.visibleEvents().get(0).id);
        assertEquals(1, model.visibleEvents().size());
    }

    @Test public void serverFailureDoesNotPresentAnEmptyListAsSuccess() throws Exception {
        ManagerEventsViewModel model = model(503, "{}");
        Result result = new Result();
        model.load(result);
        result.await();
        assertEquals(FailureKind.INTERNAL, result.failure);
        assertTrue(!model.isLoaded());
    }

    @Test public void connectionFailureIsDistinguishedFromServerFailure() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            throw new UnknownHostException("unavailable");
        }).build();
        ManagerEventsViewModel model = model(client, (refresh, callback) -> callback.onToken("token"));
        Result result = new Result();
        model.load(result);
        result.await();
        assertEquals(FailureKind.CONNECTION, result.failure);
    }

    @Test public void expiredTokenIsRefreshedBeforeRetry() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            int attempt = requests.incrementAndGet();
            assertEquals(attempt == 1 ? "Bearer old" : "Bearer renewed", chain.request().header("Authorization"));
            return new okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(attempt == 1 ? 401 : 200).message("response")
                    .body(ResponseBody.create(attempt == 1 ? "{}" : EVENTS,
                            MediaType.get("application/json"))).build();
        }).build();
        ManagerEventsViewModel model = model(client,
                (refresh, callback) -> callback.onToken(refresh ? "renewed" : "old"));
        Result result = new Result();
        model.load(result);
        result.await();
        assertNull(result.failure);
        assertEquals(2, requests.get());
        assertEquals(5, model.visibleEvents().size());
    }

    private ManagerEventsViewModel model(int code, String json) {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            assertEquals("/events/my-events", chain.request().url().encodedPath());
            assertEquals("Bearer token", chain.request().header("Authorization"));
            return new okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(code).message("response")
                    .body(ResponseBody.create(json, MediaType.get("application/json"))).build();
        }).build();
        return model(client, (refresh, callback) -> callback.onToken("token"));
    }

    private ManagerEventsViewModel model(OkHttpClient client, IdTokenProvider tokens) {
        AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/")
                .client(client).addConverterFactory(GsonConverterFactory.create())
                .callbackExecutor(Runnable::run).build().create(AstroApi.class);
        return new ManagerEventsViewModel(new ManagerEventsRepository(api, tokens));
    }

    private static final class Result implements ManagerEventsViewModel.ResultCallback {
        private final CountDownLatch finished = new CountDownLatch(1);
        private FailureKind failure;
        @Override public void onSuccess() { finished.countDown(); }
        @Override public void onFailure(FailureKind failure) { this.failure = failure; finished.countDown(); }
        void await() throws InterruptedException { assertTrue(finished.await(5, TimeUnit.SECONDS)); }
    }
}
