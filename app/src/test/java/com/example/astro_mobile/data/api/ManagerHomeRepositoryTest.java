package com.example.astro_mobile.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.astro_mobile.data.api.dto.ManagerHomeData;
import com.example.astro_mobile.data.firebase.IdTokenProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

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

public final class ManagerHomeRepositoryTest {
    private static final String DATA = "{\"username\":\"Ana Lima\",\"workspace_name\":\"Astro\","
            + "\"indicators\":{\"current_events\":3,\"pending_validations\":15,"
            + "\"collaborators_with_pending_events\":34},"
            + "\"pending_validations\":[{\"id\":101,\"collaborator_name\":\"Kevin\","
            + "\"event_title\":\"Treinamento de EPI\",\"viewed\":false}],"
            + "\"pending_forms\":[{\"id\":\"mongo-form-id\",\"title\":\"Inspeção\","
            + "\"days_left\":2,\"viewed\":true}]}";
    private static final String SUCCESS = "{\"success\":true,\"data\":" + DATA + "}";

    @Test
    public void loadsManagerContractWithBearerToken() throws Exception {
        Result result = load(repository(chain -> {
            assertEquals("GET", chain.request().method());
            assertEquals("/home/manager", chain.request().url().encodedPath());
            assertEquals("Bearer initial-token", chain.request().header("Authorization"));
            assertNull(chain.request().url().query());
            return response(chain, 200, SUCCESS);
        }, tokenProvider()));
        assertNull(result.failure);
        ManagerHomeData home = result.home;
        assertEquals("Ana Lima", home.getUsername());
        assertEquals("Astro", home.getWorkspaceName());
        assertEquals(3, home.getIndicators().getCurrentEvents());
        assertEquals(15, home.getIndicators().getPendingValidations());
        assertEquals(34, home.getIndicators().getCollaboratorsWithPendingEvents());
        ManagerHomeData.PendingValidation validation = home.getPendingValidations().get(0);
        assertEquals(Long.valueOf(101), validation.getId());
        assertEquals("Kevin", validation.getCollaboratorName());
        assertEquals("Treinamento de EPI", validation.getEventTitle());
        assertFalse(validation.isViewed());
        ManagerHomeData.PendingForm form = home.getPendingForms().get(0);
        assertEquals("mongo-form-id", form.getId());
        assertEquals("Inspeção", form.getTitle());
        assertEquals(Long.valueOf(2), form.getDaysLeft());
        assertTrue(form.isViewed());
    }

    @Test
    public void emptyAndNullFeedsPreserveIndependentIndicators() throws Exception {
        for (String feeds : new String[] {"[]", "null"}) {
            String json = "{\"success\":true,\"data\":{\"username\":\"Ana\",\"workspace_name\":\"Astro\","
                    + "\"indicators\":{\"current_events\":0,\"pending_validations\":15,"
                    + "\"collaborators_with_pending_events\":0},\"pending_validations\":" + feeds
                    + ",\"pending_forms\":" + feeds + "}}";
            Result result = load(repository(chain -> response(chain, 200, json), tokenProvider()));
            assertNull(result.failure);
            assertTrue(result.home.getPendingValidations().isEmpty());
            assertTrue(result.home.getPendingForms().isEmpty());
            assertEquals(15, result.home.getIndicators().getPendingValidations());
        }
    }

    @Test
    public void acceptsAbsentAndExpiredDeadlinesAndLongIndicators() throws Exception {
        for (String days : new String[] {"null", "-1", "0", "1", "2"}) {
            String json = SUCCESS.replace("\"days_left\":2", "\"days_left\":" + days)
                    .replace("\"current_events\":3", "\"current_events\":3000000000");
            Result result = load(repository(chain -> response(chain, 200, json), tokenProvider()));
            assertNull(result.failure);
            assertEquals(3000000000L, result.home.getIndicators().getCurrentEvents());
            assertEquals("null".equals(days) ? null : Long.valueOf(days),
                    result.home.getPendingForms().get(0).getDaysLeft());
        }
    }

    @Test
    public void unauthorizedRefreshesTokenOnlyOnce() throws Exception {
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
        assertEquals(Arrays.asList(false, true), refreshes);
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
    public void forbiddenAndIneligibleManagerDoNotRefreshToken() throws Exception {
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
    public void invalidEnvelopeAndServerFailureAreErrors() throws Exception {
        for (String json : new String[] {"", "{}", "{\"success\":false,\"data\":" + DATA + "}",
                "{\"success\":true,\"data\":null}", "<html>unavailable</html>"}) {
            assertEquals(FailureKind.INTERNAL,
                    load(repository(chain -> response(chain, 200, json), tokenProvider())).failure);
        }
        assertEquals(FailureKind.INTERNAL,
                load(repository(chain -> response(chain, 503, "{}"), tokenProvider())).failure);
    }

    @Test
    public void missingOrNegativeIndicatorsAndMissingIdentityAreErrors() throws Exception {
        Gson gson = new Gson();
        List<String> invalid = new ArrayList<>();
        for (String field : new String[] {"username", "workspace_name", "indicators"}) {
            JsonObject envelope = gson.fromJson(SUCCESS, JsonObject.class);
            envelope.getAsJsonObject("data").remove(field);
            invalid.add(envelope.toString());
        }
        for (String field : new String[] {"current_events", "pending_validations", "collaborators_with_pending_events"}) {
            JsonObject envelope = gson.fromJson(SUCCESS, JsonObject.class);
            envelope.getAsJsonObject("data").getAsJsonObject("indicators").remove(field);
            invalid.add(envelope.toString());
            envelope.getAsJsonObject("data").getAsJsonObject("indicators").addProperty(field, -1);
            invalid.add(envelope.toString());
        }
        invalid.add(SUCCESS.replace("Ana Lima", " "));
        invalid.add(SUCCESS.replace("\"workspace_name\":\"Astro\"", "\"workspace_name\":\"\""));
        for (String json : invalid) {
            assertEquals(FailureKind.INTERNAL,
                    load(repository(chain -> response(chain, 200, json), tokenProvider())).failure);
        }
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
        ManagerHomeRepository repository = repository(chain -> {
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
        ManagerHomeRepository repository = repository(chain -> {
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

    private IdTokenProvider tokenProvider() {
        return (refresh, callback) -> callback.onToken(refresh ? "refreshed-token" : "initial-token");
    }

    private ManagerHomeRepository repository(Interceptor interceptor, IdTokenProvider tokens) {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/").client(client)
                .callbackExecutor(Runnable::run).addConverterFactory(GsonConverterFactory.create())
                .build().create(AstroApi.class);
        return new ManagerHomeRepository(api, tokens);
    }

    private Response response(Interceptor.Chain chain, int code, String json) {
        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("test")
                .body(ResponseBody.create(MediaType.get("application/json"), json)).build();
    }

    private Result load(ManagerHomeRepository repository) throws Exception {
        Result result = new Result();
        repository.load(result);
        assertTrue("A consulta da home não terminou", result.completed.await(5, TimeUnit.SECONDS));
        return result;
    }

    private static final class Result implements ManagerHomeRepository.ResultCallback {
        final CountDownLatch completed = new CountDownLatch(1);
        ManagerHomeData home;
        FailureKind failure;

        @Override public void onSuccess(ManagerHomeData value) { home = value; completed.countDown(); }
        @Override public void onFailure(FailureKind value) { failure = value; completed.countDown(); }
    }
}
