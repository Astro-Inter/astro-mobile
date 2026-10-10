package com.example.astro_mobile.data.api;

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ProfilePhotoUploadTest {
    @Test
    public void sendsAuthenticatedMultipartAndRefreshesTokenOnce() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger refreshes = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            int attempt = attempts.incrementAndGet();
            assertEquals("PUT", chain.request().method());
            assertEquals("/user/me/profile-photo", chain.request().url().encodedPath());
            assertEquals(attempt == 1 ? "Bearer initial" : "Bearer renewed", chain.request().header("Authorization"));
            assertTrue(chain.request().body() instanceof MultipartBody);
            Buffer buffer = new Buffer();
            chain.request().body().writeTo(buffer);
            String multipart = buffer.readUtf8();
            assertTrue(multipart.contains("name=\"file\"; filename=\"profile.jpg\""));
            assertTrue(multipart.contains("Content-Type: image/jpeg"));
            return response(chain.request(), attempt == 1 ? 401 : 204);
        }).build();
        UserProfileRepository repository = new UserProfileRepository(api(client), (refresh, callback) -> {
            if (refresh) refreshes.incrementAndGet();
            callback.onToken(refresh ? "renewed" : "initial");
        });
        assertNull(upload(repository));
        assertEquals(2, attempts.get());
        assertEquals(1, refreshes.get());
    }

    @Test
    public void invalidFileAndRequestSizeResponsesAreRecoverable() throws Exception {
        for (int code : new int[] { 400, 413 }) {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> response(chain.request(), code)).build();
            assertEquals(FailureKind.BUSINESS, upload(new UserProfileRepository(api(client),
                    (refresh, callback) -> callback.onToken("test"))));
        }
    }

    @Test
    public void repeatedUnauthorizedUploadExpiresSession() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            attempts.incrementAndGet();
            return response(chain.request(), 401);
        }).build();
        assertEquals(FailureKind.SESSION, upload(new UserProfileRepository(api(client),
                (refresh, callback) -> callback.onToken("test"))));
        assertEquals(2, attempts.get());
    }

    private AstroApi api(OkHttpClient client) {
        return new Retrofit.Builder().baseUrl("https://example.com/").client(client)
                .callbackExecutor(Runnable::run).addConverterFactory(GsonConverterFactory.create())
                .build().create(AstroApi.class);
    }

    private Response response(Request request, int code) {
        return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code)
                .message("test").body(ResponseBody.create(MediaType.get("application/json"), "")).build();
    }

    private FailureKind upload(UserProfileRepository repository) throws Exception {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<FailureKind> failure = new AtomicReference<>();
        ProfilePhotoFile file = ProfilePhotoFile.read(new ByteArrayInputStream(new byte[] { (byte)255, (byte)216, (byte)255 }));
        repository.uploadPhoto(file, new UserProfileRepository.PhotoCallback() {
            public void onSuccess() { completed.countDown(); }
            public void onFailure(FailureKind kind) { failure.set(kind); completed.countDown(); }
        });
        assertTrue(completed.await(5, TimeUnit.SECONDS));
        return failure.get();
    }
}
