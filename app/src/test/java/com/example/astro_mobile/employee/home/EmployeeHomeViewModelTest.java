package com.example.astro_mobile.employee.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.example.astro_mobile.data.api.AstroApi;
import com.example.astro_mobile.data.api.CollaboratorHomeRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.CollaboratorHomeData;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class EmployeeHomeViewModelTest {
    private static final String SUCCESS = "{\"success\":true,\"data\":{\"username\":\"Ana\","
            + "\"workspace_name\":\"Astro\",\"events_metrics\":{\"pending\":0,"
            + "\"under_analysis\":0,\"done\":1},\"status_message\":\"OK\"}}";

    @Test
    public void ownsLoadingAndDataAndPreventsConcurrentLoads() throws Exception {
        Fixture fixture = new Fixture();
        Result result = new Result();
        Result duplicate = new Result();
        fixture.model.load(result);
        fixture.model.load(duplicate);
        assertTrue(fixture.model.isLoading());
        assertNull(fixture.model.getHome());
        assertEquals(1, fixture.tokens.size());
        fixture.tokens.get(0).onToken("token");
        result.await();
        assertFalse(fixture.model.isLoading());
        assertNull(result.failure);
        assertNotNull(result.home);
        assertSame(result.home, fixture.model.getHome());
        assertEquals(1, duplicate.completed.getCount());
        assertEquals(1, fixture.calls.get());
    }

    @Test
    public void failureReleasesLoadingAndAllowsRetry() throws Exception {
        Fixture fixture = new Fixture();
        fixture.code = 503;
        Result failed = new Result();
        fixture.model.load(failed);
        fixture.tokens.get(0).onToken("token");
        failed.await();
        assertEquals(FailureKind.INTERNAL, failed.failure);
        assertFalse(fixture.model.isLoading());
        assertNull(fixture.model.getHome());
        fixture.code = 200;
        Result retried = new Result();
        fixture.model.load(retried);
        fixture.tokens.get(1).onToken("token");
        retried.await();
        assertNotNull(retried.home);
        assertEquals(1, failed.deliveries.get());
        assertEquals(2, fixture.calls.get());
    }

    @Test
    public void cancelledResultCannotReachOldViewOrReplaceNewLoad() throws Exception {
        Fixture fixture = new Fixture();
        Result previous = new Result();
        fixture.model.load(previous);
        fixture.model.cancel();
        assertFalse(fixture.model.isLoading());
        Result current = new Result();
        fixture.model.load(current);
        fixture.tokens.get(0).onFailure(IdTokenProvider.Failure.SESSION);
        fixture.tokens.get(0).onToken("old-token");
        assertTrue(fixture.model.isLoading());
        fixture.tokens.get(1).onToken("new-token");
        current.await();
        assertEquals(0, previous.deliveries.get());
        assertNotNull(current.home);
        assertSame(current.home, fixture.model.getHome());
        assertEquals(1, fixture.calls.get());
    }

    @Test
    public void cancelAndReloadDoNotRetainPreviousAccountData() throws Exception {
        Fixture fixture = new Fixture();
        Result first = new Result();
        fixture.model.load(first);
        fixture.tokens.get(0).onToken("token");
        first.await();
        assertNotNull(fixture.model.getHome());
        fixture.model.load(new Result());
        assertNull(fixture.model.getHome());
        assertTrue(fixture.model.isLoading());
        fixture.model.cancel();
        assertNull(fixture.model.getHome());
        assertFalse(fixture.model.isLoading());
    }

    @Test
    public void clearingViewModelCancelsPendingRequest() {
        Fixture fixture = new Fixture();
        Result result = new Result();
        fixture.model.load(result);
        fixture.model.onCleared();
        fixture.tokens.get(0).onToken("late-token");
        fixture.tokens.get(0).onFailure(IdTokenProvider.Failure.SESSION);
        assertFalse(fixture.model.isLoading());
        assertNull(fixture.model.getHome());
        assertEquals(0, result.deliveries.get());
        assertEquals(0, fixture.calls.get());
    }

    @Test
    public void tokenFailuresFinishSynchronouslyWithoutLeavingLoadingActive() throws Exception {
        for (IdTokenProvider.Failure failure : IdTokenProvider.Failure.values()) {
            CollaboratorHomeRepository repository = new CollaboratorHomeRepository(null,
                    (refresh, callback) -> callback.onFailure(failure));
            EmployeeHomeViewModel model = new EmployeeHomeViewModel(repository);
            Result result = new Result();
            model.load(result);
            result.await();
            assertFalse(model.isLoading());
            assertNull(model.getHome());
            assertEquals(FailureKind.valueOf(failure.name()), result.failure);
            assertEquals(1, result.deliveries.get());
        }
    }

    private static final class Fixture {
        final List<IdTokenProvider.Callback> tokens = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        final EmployeeHomeViewModel model;
        volatile int code = 200;

        Fixture() {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                calls.incrementAndGet();
                return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                        .code(code).message("test")
                        .body(ResponseBody.create(MediaType.get("application/json"), SUCCESS)).build();
            }).build();
            AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/").client(client)
                    .callbackExecutor(Runnable::run).addConverterFactory(GsonConverterFactory.create())
                    .build().create(AstroApi.class);
            model = new EmployeeHomeViewModel(new CollaboratorHomeRepository(api,
                    (refresh, callback) -> tokens.add(callback)));
        }
    }

    private static final class Result implements EmployeeHomeViewModel.ResultCallback {
        final CountDownLatch completed = new CountDownLatch(1);
        final AtomicInteger deliveries = new AtomicInteger();
        CollaboratorHomeData home;
        FailureKind failure;

        @Override
        public void onSuccess(CollaboratorHomeData home) {
            this.home = home;
            deliveries.incrementAndGet();
            completed.countDown();
        }

        @Override
        public void onFailure(FailureKind failure) {
            this.failure = failure;
            deliveries.incrementAndGet();
            completed.countDown();
        }

        void await() throws InterruptedException {
            assertTrue("A consulta da Home não terminou", completed.await(5, TimeUnit.SECONDS));
        }
    }
}
