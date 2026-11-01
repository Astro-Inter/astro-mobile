package com.example.astro_mobile.data.api;

import static org.junit.Assert.*;

import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.example.astro_mobile.data.firebase.FirebaseAuthRepository;
import com.example.astro_mobile.auth.PasswordResetViewModel;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PasswordResetRepositoryTest {
    @Test public void sendsOnlyEmailWithoutAuthorizationAndAcceptsNeutralSuccess() throws Exception {
        Fixture fixture = new Fixture(200, "{\"success\":true,\"message\":\"Se estiver cadastrado\"}");
        assertNull(fixture.request());
        assertEquals("/auth/forgot-password", fixture.path);
        assertEquals("{\"email\":\"user@example.com\"}", fixture.body);
        assertNull(fixture.authorization);
        assertEquals(1, fixture.calls.get());
    }

    @Test public void rejectsInvalidEmailAndUnexpectedResponses() throws Exception {
        assertEquals(AuthFailureKind.INVALID_CREDENTIALS, new Fixture(400,
                "{\"success\":false,\"error\":{\"code\":\"INVALID_EMAIL\"}}").request());
        assertEquals(AuthFailureKind.RESET_UNAVAILABLE, new Fixture(200, "{\"success\":false}").request());
        assertEquals(AuthFailureKind.RESET_UNAVAILABLE, new Fixture(503, "{}").request());
    }

    @Test public void classifiesServiceFailuresAndRetainsSafeRequestId() throws Exception {
        String[] support = {"FIREBASE_CONFIGURATION_ERROR", "FIREBASE_AUTH_UNAVAILABLE", "EMAIL_CONFIGURATION_ERROR"};
        String[] unavailable = {"FIREBASE_UNAVAILABLE", "PROVIDER_UNAVAILABLE", "SERVICE_UNAVAILABLE", "UNKNOWN"};
        String[] uncertain = {"EMAIL_DELIVERY_UNAVAILABLE", "PROVIDER_TIMEOUT"};
        for (String code : support) assertFailure(code, AuthFailureKind.RESET_SUPPORT);
        for (String code : unavailable) assertFailure(code, AuthFailureKind.RESET_UNAVAILABLE);
        for (String code : uncertain) assertFailure(code, AuthFailureKind.RESET_UNCERTAIN);
        Fixture malformed = new Fixture(503, "not json");
        assertEquals(AuthFailureKind.RESET_UNAVAILABLE, malformed.request());
        Fixture timeout = new Fixture(200, "{}");
        timeout.timeout = true;
        assertEquals(AuthFailureKind.RESET_UNCERTAIN, timeout.request());
        assertEquals(1, timeout.calls.get());
    }

    private static void assertFailure(String code, AuthFailureKind expected) throws Exception {
        Fixture fixture = new Fixture(503, "{\"success\":false,\"error\":{\"code\":\"" + code + "\"}}");
        fixture.requestId = "abc-123";
        assertEquals(expected, fixture.request());
        assertEquals("abc-123", fixture.returnedRequestId);
        assertEquals(1, fixture.calls.get());
    }

    @Test public void detectsConnectionFailure() throws Exception {
        Fixture fixture = new Fixture(200, "{}");
        fixture.offline = true;
        assertEquals(AuthFailureKind.CONNECTION, fixture.request());
    }

    @Test public void loadingPreventsRepeatedRequestsAndEndsAfterResponse() throws Exception {
        Fixture fixture = new Fixture(200, "{\"success\":true}");
        fixture.block = true;
        PasswordResetViewModel model = new PasswordResetViewModel(new FirebaseAuthRepository(null), fixture.repository);
        CountDownLatch done = new CountDownLatch(1);
        PasswordResetViewModel.ResultCallback callback = new PasswordResetViewModel.ResultCallback() {
            @Override public void onSuccess() { done.countDown(); }
            @Override public void onFailure(AuthFailureKind kind) { done.countDown(); }
        };
        model.sendPasswordResetEmail("user@example.com", callback);
        assertTrue(fixture.entered.await(5, TimeUnit.SECONDS));
        assertTrue(model.isLoading());
        model.clearRequest();
        model.attachCallback(callback);
        model.sendPasswordResetEmail("user@example.com", callback);
        fixture.release.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertFalse(model.isLoading());
        assertEquals(1, fixture.calls.get());
    }

    @Test public void loadingEndsOnFailures() throws Exception {
        Fixture invalid = new Fixture(400, "{\"success\":false,\"error\":{\"code\":\"INVALID_EMAIL\"}}");
        Fixture unavailable = new Fixture(503, "{\"success\":false,\"error\":{\"code\":\"SERVICE_UNAVAILABLE\"}}");
        Fixture offline = new Fixture(200, "{}"); offline.offline = true;
        Fixture timeout = new Fixture(200, "{}"); timeout.timeout = true;
        for (Fixture fixture : new Fixture[]{invalid, unavailable, offline, timeout}) {
            PasswordResetViewModel model = new PasswordResetViewModel(new FirebaseAuthRepository(null), fixture.repository);
            CountDownLatch done = new CountDownLatch(1);
            AuthFailureKind[] result = new AuthFailureKind[1];
            model.sendPasswordResetEmail("user@example.com", new PasswordResetViewModel.ResultCallback() {
                @Override public void onSuccess() { done.countDown(); }
                @Override public void onFailure(AuthFailureKind kind) { result[0] = kind; done.countDown(); }
            });
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertNotNull("Falha não pode virar sucesso", result[0]);
            assertFalse(model.isLoading());
            assertEquals(1, fixture.calls.get());
        }
    }

    @Test public void honorsRetryAfterBeforeSendingAgain() throws Exception {
        Fixture fixture = new Fixture(429, "{}");
        fixture.retryAfter = "1";
        try {
            assertEquals(AuthFailureKind.RATE_LIMIT, fixture.request());
            assertEquals(AuthFailureKind.RATE_LIMIT, fixture.request());
            PasswordResetViewModel model = new PasswordResetViewModel(new FirebaseAuthRepository(null), fixture.repository);
            AuthFailureKind[] result = new AuthFailureKind[1];
            model.sendPasswordResetEmail("user@example.com", new PasswordResetViewModel.ResultCallback() {
                @Override public void onSuccess() { fail("Limite não pode virar sucesso"); }
                @Override public void onFailure(AuthFailureKind kind) { result[0] = kind; }
            });
            assertEquals(AuthFailureKind.RATE_LIMIT, result[0]);
            assertFalse(model.isLoading());
            assertEquals(1, fixture.calls.get());
        } finally {
            Thread.sleep(1200);
        }
    }

    private static final class Fixture {
        final AtomicInteger calls = new AtomicInteger();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final PasswordResetRepository repository;
        String path, body, authorization, retryAfter, requestId, returnedRequestId;
        boolean offline, timeout, block;

        Fixture(int status, String json) {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                calls.incrementAndGet();
                if (block) {
                    entered.countDown();
                    try { release.await(5, TimeUnit.SECONDS); }
                    catch (InterruptedException error) { throw new java.io.IOException(error); }
                }
                path = chain.request().url().encodedPath();
                authorization = chain.request().header("Authorization");
                Buffer buffer = new Buffer();
                chain.request().body().writeTo(buffer);
                body = buffer.readUtf8();
                if (offline) throw new UnknownHostException("offline");
                if (timeout) throw new java.net.SocketTimeoutException("timeout");
                Response.Builder response = new Response.Builder().request(chain.request())
                        .protocol(Protocol.HTTP_1_1).code(status).message("test")
                        .body(ResponseBody.create(MediaType.get("application/json"), json));
                if (retryAfter != null) response.header("Retry-After", retryAfter);
                if (requestId != null) response.header("X-Request-ID", requestId);
                return response.build();
            }).build();
            PasswordResetApi api = new Retrofit.Builder().baseUrl("https://example.com/")
                    .client(client).callbackExecutor(Runnable::run)
                    .addConverterFactory(GsonConverterFactory.create()).build().create(PasswordResetApi.class);
            repository = new PasswordResetRepository(api);
        }

        AuthFailureKind request() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AuthFailureKind[] failure = new AuthFailureKind[1];
            repository.request("user@example.com", new PasswordResetRepository.ResultCallback() {
                @Override public void onSuccess() { latch.countDown(); }
                @Override public void onFailure(AuthFailureKind value) { failure[0] = value; latch.countDown(); }
                @Override public void onFailure(AuthFailureKind value, String id) {
                    returnedRequestId = id;
                    onFailure(value);
                }
            });
            assertTrue("Resposta não recebida", latch.await(5, TimeUnit.SECONDS));
            return failure[0];
        }
    }
}
