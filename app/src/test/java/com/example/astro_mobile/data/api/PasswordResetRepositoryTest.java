package com.example.astro_mobile.data.api;

import static org.junit.Assert.*;

import com.example.astro_mobile.data.firebase.AuthFailureKind;
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
    }

    @Test public void rejectsInvalidEmailAndUnexpectedResponses() throws Exception {
        assertEquals(AuthFailureKind.INVALID_CREDENTIALS, new Fixture(400,
                "{\"success\":false,\"error\":{\"code\":\"INVALID_EMAIL\"}}").request());
        assertEquals(AuthFailureKind.INTERNAL, new Fixture(200, "{\"success\":false}").request());
        assertEquals(AuthFailureKind.INTERNAL, new Fixture(503, "{}").request());
    }

    @Test public void detectsConnectionFailure() throws Exception {
        Fixture fixture = new Fixture(200, "{}");
        fixture.offline = true;
        assertEquals(AuthFailureKind.CONNECTION, fixture.request());
    }

    @Test public void honorsRetryAfterBeforeSendingAgain() throws Exception {
        Fixture fixture = new Fixture(429, "{}");
        fixture.retryAfter = "1";
        try {
            assertEquals(AuthFailureKind.RATE_LIMIT, fixture.request());
            assertEquals(AuthFailureKind.RATE_LIMIT, fixture.request());
            assertEquals(1, fixture.calls.get());
        } finally {
            Thread.sleep(1200);
        }
    }

    private static final class Fixture {
        final AtomicInteger calls = new AtomicInteger();
        final PasswordResetRepository repository;
        String path, body, authorization, retryAfter;
        boolean offline;

        Fixture(int status, String json) {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                calls.incrementAndGet();
                path = chain.request().url().encodedPath();
                authorization = chain.request().header("Authorization");
                Buffer buffer = new Buffer();
                chain.request().body().writeTo(buffer);
                body = buffer.readUtf8();
                if (offline) throw new UnknownHostException("offline");
                Response.Builder response = new Response.Builder().request(chain.request())
                        .protocol(Protocol.HTTP_1_1).code(status).message("test")
                        .body(ResponseBody.create(MediaType.get("application/json"), json));
                if (retryAfter != null) response.header("Retry-After", retryAfter);
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
            });
            assertTrue("Resposta não recebida", latch.await(5, TimeUnit.SECONDS));
            return failure[0];
        }
    }
}
