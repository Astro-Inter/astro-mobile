package com.example.astro_mobile.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.astro_mobile.data.api.dto.CollaboratorEventData;
import com.example.astro_mobile.employee.events.EventStatusClassifier;
import com.example.astro_mobile.employee.events.EmployeeEvent;
import java.time.ZoneId;
import java.time.Instant;
import com.example.astro_mobile.data.api.dto.CollaboratorHomeData;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import org.junit.Test;

import java.net.UnknownHostException;
import java.util.ArrayList;
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

public final class CollaboratorHomeRepositoryTest {
    private static final String DATA = "{\"username\":\"Ana Lima\",\"workspace_name\":\"Astro\","
            + "\"events_metrics\":{\"pending\":3,\"under_analysis\":2,\"done\":21},"
            + "\"status_message\":\"URGENT\","
            + "\"pending_events\":[{\"id\":101,\"title\":\"Treinamento de EPI\","
            + "\"limit_date\":\"2026-10-19\",\"viewed\":false}],"
            + "\"last_notifications\":[{\"id\":\"mongo-id\",\"title\":\"Lembrete\","
            + "\"sent_at_in_hours\":2,\"viewed\":true}]}";
    private static final String SUCCESS = "{\"success\":true,\"data\":" + DATA + "}";

    @Test
    public void loadsRealContractWithBearerToken() throws Exception {
        Result result = load(repository(chain -> {
            assertEquals("GET", chain.request().method());
            assertEquals("/home/collaborator", chain.request().url().encodedPath());
            assertEquals("Bearer initial-token", chain.request().header("Authorization"));
            assertNull(chain.request().url().query());
            return response(chain, 200, SUCCESS);
        }, tokenProvider()));
        assertNull(result.failure);
        CollaboratorHomeData home = result.home;
        assertEquals("Ana Lima", home.getUsername());
        assertEquals("Astro", home.getWorkspaceName());
        assertEquals(3, home.getEventsMetrics().getPending());
        assertEquals(2, home.getEventsMetrics().getUnderAnalysis());
        assertEquals(21, home.getEventsMetrics().getDone());
        assertEquals("URGENT", home.getStatusMessage());
        assertEquals(Long.valueOf(101), home.getPendingEvents().get(0).getId());
        assertEquals("Treinamento de EPI", home.getPendingEvents().get(0).getTitle());
        assertEquals("2026-10-19", home.getPendingEvents().get(0).getLimitDate());
        assertFalse(home.getPendingEvents().get(0).isViewed());
        assertEquals("mongo-id", home.getLastNotifications().get(0).getId());
        assertEquals("Lembrete", home.getLastNotifications().get(0).getTitle());
        assertEquals(2, home.getLastNotifications().get(0).getSentAtInHours());
        assertTrue(home.getLastNotifications().get(0).isViewed());
    }

    @Test
    public void acceptsAllFourStatusCodesAndEmptyFeeds() throws Exception {
        for (String status : new String[] {"URGENT", "PENDENT", "ANALYSIS", "OK"}) {
            String data = "{\"username\":\"Ana\",\"workspace_name\":\"Astro\","
                    + "\"events_metrics\":{\"pending\":0,\"under_analysis\":0,\"done\":0},"
                    + "\"status_message\":\"" + status + "\",\"pending_events\":[],\"last_notifications\":[]}";
            Result result = load(repository(chain -> response(chain, 200,
                    "{\"success\":true,\"data\":" + data + "}"), tokenProvider()));
            assertNull(result.failure);
            assertEquals(status, result.home.getStatusMessage());
            assertTrue(result.home.getPendingEvents().isEmpty());
            assertTrue(result.home.getLastNotifications().isEmpty());
        }
    }

    @Test
    public void nullFeedsAreEmptyWithoutLosingMetrics() throws Exception {
        String json = SUCCESS.replaceFirst("\\\"pending_events\\\":\\[.*?\\]", "\"pending_events\":null")
                .replaceFirst("\\\"last_notifications\\\":\\[.*?\\]", "\"last_notifications\":null");
        Result result = load(repository(chain -> response(chain, 200, json), tokenProvider()));
        assertNull(result.failure);
        assertTrue(result.home.getPendingEvents().isEmpty());
        assertTrue(result.home.getLastNotifications().isEmpty());
        assertEquals(21, result.home.getEventsMetrics().getDone());
    }

    @Test
    public void unauthorizedRefreshesTokenOnceAndRetries() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        List<Boolean> refreshes = new ArrayList<>();
        Result result = load(repository(chain -> {
            int index = calls.getAndIncrement();
            assertEquals(index == 0 ? "Bearer initial-token" : "Bearer refreshed-token",
                    chain.request().header("Authorization"));
            return response(chain, index == 0 ? 401 : 200, SUCCESS);
        }, (refresh, callback) -> {
            refreshes.add(refresh);
            callback.onToken(refresh ? "refreshed-token" : "initial-token");
        }));
        assertNull(result.failure);
        assertEquals(2, calls.get());
        assertEquals(java.util.Arrays.asList(false, true), refreshes);
    }

    @Test
    public void repeatedUnauthorizedEndsSessionWithoutRetryLoop() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Result result = load(repository(chain -> {
            calls.incrementAndGet();
            return response(chain, 401, "{}");
        }, tokenProvider()));
        assertEquals(FailureKind.SESSION, result.failure);
        assertEquals(2, calls.get());
    }

    @Test
    public void forbiddenAndMissingHomeDoNotRefreshOrReturnEmptySuccess() throws Exception {
        for (int code : new int[] {403, 404}) {
            AtomicInteger tokens = new AtomicInteger();
            Result result = load(repository(chain -> response(chain, code, "{}"), (refresh, callback) -> {
                tokens.incrementAndGet();
                assertFalse(refresh);
                callback.onToken("initial-token");
            }));
            assertEquals(FailureKind.BUSINESS, result.failure);
            assertNull(result.home);
            assertEquals(1, tokens.get());
        }
    }

    @Test
    public void invalidResponsesAndServerFailureAreErrors() throws Exception {
        for (String json : new String[] {"", "{}", "{\"success\":false,\"data\":" + DATA + "}",
                "{\"success\":true,\"data\":null}",
                SUCCESS.replace("URGENT", "UNKNOWN"),
                SUCCESS.replace("\"pending\":3", "\"pending\":-1"),
                SUCCESS.replace("\"username\":\"Ana Lima\"", "\"username\":null"),
                SUCCESS.replace("\"events_metrics\":{\"pending\":3,\"under_analysis\":2,\"done\":21}",
                        "\"events_metrics\":null"),
                SUCCESS.replace("\"events_metrics\":{\"pending\":3,\"under_analysis\":2,\"done\":21}",
                        "\"events_metrics\":{}"), "<html>unavailable</html>"}) {
            assertEquals(FailureKind.INTERNAL,
                    load(repository(chain -> response(chain, 200, json), tokenProvider())).failure);
        }
        assertEquals(FailureKind.INTERNAL,
                load(repository(chain -> response(chain, 503, "{}"), tokenProvider())).failure);
    }

    @Test
    public void connectionFailureAndTimeoutAreDistinguished() throws Exception {
        assertEquals(FailureKind.CONNECTION, load(repository(chain -> {
            throw new UnknownHostException();
        }, tokenProvider())).failure);
        assertEquals(FailureKind.INTERNAL, load(repository(chain -> {
            throw new java.net.SocketTimeoutException();
        }, tokenProvider())).failure);
    }

    @Test
    public void tokenFailureDoesNotSendHttpRequest() throws Exception {
        for (IdTokenProvider.Failure failure : IdTokenProvider.Failure.values()) {
            Result result = load(repository(chain -> {
                fail("Não deve consultar a home sem token");
                return response(chain, 200, SUCCESS);
            }, (refresh, callback) -> callback.onFailure(failure)));
            assertEquals(FailureKind.valueOf(failure.name()), result.failure);
        }
    }

    @Test
    public void cancelledTokenRequestCannotStartHttpOrDeliverResult() {
        AtomicReference<IdTokenProvider.Callback> token = new AtomicReference<>();
        CollaboratorHomeRepository repository = repository(chain -> {
            fail("Uma consulta cancelada não pode enviar HTTP");
            return response(chain, 200, SUCCESS);
        }, (refresh, callback) -> token.set(callback));
        Result result = new Result();
        repository.load(result);
        repository.cancel();
        token.get().onToken("late-token");
        token.get().onFailure(IdTokenProvider.Failure.SESSION);
        assertEquals(1, result.completed.getCount());
    }

    @Test
    public void newLoadDiscardsPreviousTokenCallback() throws Exception {
        List<IdTokenProvider.Callback> tokens = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        CollaboratorHomeRepository repository = repository(chain -> {
            calls.incrementAndGet();
            return response(chain, 200, SUCCESS);
        }, (refresh, callback) -> tokens.add(callback));
        Result previous = new Result();
        Result current = new Result();
        repository.load(previous);
        repository.load(current);
        tokens.get(0).onToken("old-token");
        tokens.get(1).onToken("current-token");
        assertTrue(current.completed.await(5, TimeUnit.SECONDS));
        assertNotNull(current.home);
        assertEquals(1, previous.completed.getCount());
        assertEquals(1, calls.get());
    }

    @Test
    public void loadsParticipationForHomeInsteadOfGuessingFromViewed() throws Exception {
        AtomicInteger eventCalls = new AtomicInteger();
        Result result = load(repository(chain -> response(chain, 200, SUCCESS), chain -> {
            eventCalls.incrementAndGet();
            assertEquals("Bearer initial-token", chain.request().header("Authorization"));
            return response(chain, 200, EVENTS);
        }, tokenProvider()));
        assertNull(result.failure);
        assertEquals(1, eventCalls.get());
        assertEquals(CollaboratorEventData.ParticipationStatus.REJEITADO,
                result.home.getPendingEvents().get(0).getEventData().getParticipationStatus());
        assertEquals(EventStatusClassifier.DisplayStatus.REJECTED,
                EmployeeEvent.fromApi(
                        result.home.getPendingEvents().get(0).getEventData(), ZoneId.of("America/Sao_Paulo"),
                        Instant.parse("2026-10-25T12:00:00Z")).displayStatus);
    }

    @Test
    public void failedParticipationLookupDoesNotPublishGuessedHomeStatus() throws Exception {
        Result result = load(repository(chain -> response(chain, 200, SUCCESS),
                chain -> response(chain, 503, "{}"), tokenProvider()));
        assertEquals(FailureKind.INTERNAL, result.failure);
        assertNull(result.home);
    }

    private IdTokenProvider tokenProvider() {
        return (refresh, callback) -> callback.onToken(refresh ? "refreshed-token" : "initial-token");
    }

    private static final String EVENTS = "{\"success\":true,\"data\":[{\"id\":101,"
            + "\"eventTitle\":\"Treinamento de EPI\",\"classLimitDate\":\"2026-10-19T12:00:00\","
            + "\"viewed\":false,\"participationStatus\":\"REJEITADO\",\"eventStatus\":\"ATIVO\"}]}";

    private CollaboratorHomeRepository repository(Interceptor interceptor, IdTokenProvider tokens) {
        return repository(interceptor, chain -> response(chain, 200, EVENTS), tokens);
    }

    private CollaboratorHomeRepository repository(Interceptor home, Interceptor events, IdTokenProvider tokens) {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain ->
                chain.request().url().encodedPath().equals("/events/collaborator")
                        ? events.intercept(chain) : home.intercept(chain)).build();
        AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/").client(client)
                .callbackExecutor(Runnable::run).addConverterFactory(GsonConverterFactory.create())
                .build().create(AstroApi.class);
        return new CollaboratorHomeRepository(api, tokens);
    }

    private Response response(Interceptor.Chain chain, int code, String json) {
        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("test")
                .body(ResponseBody.create(MediaType.get("application/json"), json)).build();
    }

    private Result load(CollaboratorHomeRepository repository) throws Exception {
        Result result = new Result();
        repository.load(result);
        assertTrue("A consulta da home não terminou", result.completed.await(5, TimeUnit.SECONDS));
        return result;
    }

    private static final class Result implements CollaboratorHomeRepository.ResultCallback {
        final CountDownLatch completed = new CountDownLatch(1);
        CollaboratorHomeData home;
        FailureKind failure;

        @Override public void onSuccess(CollaboratorHomeData value) { home = value; completed.countDown(); }
        @Override public void onFailure(FailureKind value) { failure = value; completed.countDown(); }
    }
}
