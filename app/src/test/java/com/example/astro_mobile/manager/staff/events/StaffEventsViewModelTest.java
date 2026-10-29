package com.example.astro_mobile.manager.staff.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.astro_mobile.data.api.AstroApi;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.StaffEventsRepository;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import org.junit.Test;

import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class StaffEventsViewModelTest {
    private static final String EMPTY = "{\"success\":true,\"data\":{\"collaboratorId\":2,"
            + "\"collaboratorName\":\"Ana Silva\",\"events\":[]}}";
    private static final String EVENTS = EMPTY.replace("[]", "["
            + event(10, "PENDENTE", "2099-01-01T10:00:00") + ","
            + event(11, "EM_ANALISE", "2000-01-01T10:00:00") + ","
            + event(12, "CONCLUIDO", "2000-01-01T10:00:00") + ","
            + event(13, "PENDENTE", "2000-01-01T10:00:00") + "]");

    @Test public void loadsTheSelectedCollaboratorAndFiltersTheContractStates() throws Exception {
        StaffEventsViewModel model = model(200, EVENTS);
        Result result = load(model);
        assertNull(result.failure);
        assertEquals("Ana Silva", model.getCollaboratorName());
        assertEquals(4, model.visibleEvents().size());
        model.setFilter(StaffEventsViewModel.Filter.PENDING);
        assertEquals(1, model.visibleEvents().size());
        assertEquals(10L, model.visibleEvents().get(0).id);
        model.setFilter(StaffEventsViewModel.Filter.DONE);
        assertEquals(12L, model.visibleEvents().get(0).id);
        model.setFilter(StaffEventsViewModel.Filter.EXPIRED);
        assertEquals(1, model.visibleEvents().size());
        assertEquals(13L, model.visibleEvents().get(0).id);
    }

    @Test public void emptyResponseIsSuccessButServerFailureIsNotAnEmptyList() throws Exception {
        StaffEventsViewModel empty = model(200, EMPTY);
        assertNull(load(empty).failure);
        assertTrue(empty.isLoaded());
        assertTrue(empty.visibleEvents().isEmpty());
        StaffEventsViewModel unavailable = model(503, "{}");
        assertEquals(FailureKind.INTERNAL, load(unavailable).failure);
        assertFalse(unavailable.isLoaded());
    }

    @Test public void responseForAnotherCollaboratorIsRejected() throws Exception {
        StaffEventsViewModel model = model(200, EMPTY.replace("\"collaboratorId\":2", "\"collaboratorId\":9"));
        assertEquals(FailureKind.INTERNAL, load(model).failure);
        assertFalse(model.isLoaded());
    }

    @Test public void missingModeIsAContractErrorAndDoesNotExposePartialData() throws Exception {
        StaffEventsViewModel model = model(200, EVENTS.replace(",\"completionMode\":\"GESTOR\"", ""));
        assertEquals(FailureKind.INTERNAL, load(model).failure);
        assertFalse(model.isLoaded());
        assertTrue(model.visibleEvents().isEmpty());
    }

    @Test public void networkFailureOpensConnectionError() throws Exception {
        StaffEventsViewModel model = model(chain -> { throw new UnknownHostException("offline"); },
                (refresh, callback) -> callback.onToken("token"));
        assertEquals(FailureKind.CONNECTION, load(model).failure);
    }

    @Test public void expiredTokenIsRenewedOnce() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        StaffEventsViewModel model = model(chain -> {
            int attempt = calls.incrementAndGet();
            assertEquals(attempt == 1 ? "Bearer old" : "Bearer renewed",
                    chain.request().header("Authorization"));
            return response(chain, attempt == 1 ? 401 : 200, attempt == 1 ? "{}" : EMPTY);
        }, (refresh, callback) -> callback.onToken(refresh ? "renewed" : "old"));
        assertNull(load(model).failure);
        assertEquals(2, calls.get());
    }

    @Test public void repeatedUnauthorizedStopsWithSessionFailure() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        StaffEventsViewModel model = model(chain -> {
            calls.incrementAndGet();
            return response(chain, 401, "{}");
        }, (refresh, callback) -> callback.onToken("invalid"));
        assertEquals(FailureKind.SESSION, load(model).failure);
        assertEquals(2, calls.get());
    }

    private static String event(int id, String status, String deadline) {
        return "{\"eventId\":" + id + ",\"eventTitle\":\"Treinamento\",\"classLimitDate\":\""
                + deadline + "\",\"completionStatus\":\"" + status
                + "\",\"eventStatus\":\"ATIVO\",\"completionMode\":\"GESTOR\"}";
    }

    private StaffEventsViewModel model(int code, String json) {
        return model(chain -> {
            assertEquals("/collaborators/2/events", chain.request().url().encodedPath());
            assertEquals("GET", chain.request().method());
            assertEquals("Bearer token", chain.request().header("Authorization"));
            return response(chain, code, json);
        }, (refresh, callback) -> callback.onToken("token"));
    }

    private StaffEventsViewModel model(Interceptor interceptor, IdTokenProvider tokens) {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/").client(client)
                .addConverterFactory(GsonConverterFactory.create()).callbackExecutor(Runnable::run)
                .build().create(AstroApi.class);
        return new StaffEventsViewModel(new StaffEventsRepository(api, tokens));
    }

    private okhttp3.Response response(Interceptor.Chain chain, int code, String json) {
        return new okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("response").body(ResponseBody.create(json,
                        MediaType.get("application/json"))).build();
    }

    private Result load(StaffEventsViewModel model) throws InterruptedException {
        Result result = new Result();
        model.load(2L, result);
        assertTrue(result.finished.await(5, TimeUnit.SECONDS));
        return result;
    }

    private static final class Result implements StaffEventsViewModel.ResultCallback {
        private final CountDownLatch finished = new CountDownLatch(1);
        private FailureKind failure;
        @Override public void onSuccess() { finished.countDown(); }
        @Override public void onFailure(FailureKind failure) {
            this.failure = failure;
            finished.countDown();
        }
    }
}
