package com.example.astro_mobile.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.example.astro_mobile.data.api.AstroApi;
import com.example.astro_mobile.data.api.NotificationsRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.NotificationData;
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

public final class NotificationsViewModelTest {
    private static final String SUCCESS = "{\"success\":true,\"data\":[{\"id\":\"id\",\"title\":\"Aviso\",\"viewed\":false,\"hours\":2}]}";
    private static final String SEARCH_DATA = "{\"success\":true,\"data\":["
            + "{\"id\":\"first\",\"title\":\"Validações pendentes\",\"viewed\":false,\"hours\":0},"
            + "{\"id\":\"second\",\"title\":\"Treinamento de EPI\",\"viewed\":true,\"hours\":2},"
            + "{\"id\":\"third\",\"title\":\"Novas validações\",\"viewed\":false,\"hours\":4}]}";

    @Test
    public void searchIgnoresAccentsCaseAndOuterSpacesAndKeepsServerOrder() throws Exception {
        Fixture fixture = new Fixture();
        fixture.json = SEARCH_DATA;
        Result result = new Result();
        fixture.model.load(result);
        fixture.tokens.get(0).onToken("token");
        result.await();

        fixture.model.setSearchQuery("  VALIDACOES  ");
        List<NotificationData> visible = fixture.model.visibleNotifications();
        assertEquals(2, visible.size());
        assertEquals("first", visible.get(0).getId());
        assertEquals("third", visible.get(1).getId());
        assertEquals(3, fixture.model.getNotifications().size());

        fixture.model.setSearchQuery("epi");
        assertEquals(1, fixture.model.visibleNotifications().size());
        assertEquals("second", fixture.model.visibleNotifications().get(0).getId());
        assertEquals(1, fixture.calls.get());
    }

    @Test
    public void clearingSearchRestoresFullListAfterNoMatches() throws Exception {
        Fixture fixture = new Fixture();
        fixture.json = SEARCH_DATA;
        Result result = new Result();
        fixture.model.load(result);
        fixture.tokens.get(0).onToken("token");
        result.await();

        fixture.model.setSearchQuery("inexistente");
        assertTrue(fixture.model.visibleNotifications().isEmpty());
        assertEquals(3, fixture.model.getNotifications().size());

        for (String query : new String[]{"", "   "}) {
            fixture.model.setSearchQuery(query);
            List<NotificationData> visible = fixture.model.visibleNotifications();
            assertEquals(3, visible.size());
            assertEquals("first", visible.get(0).getId());
            assertEquals("second", visible.get(1).getId());
            assertEquals("third", visible.get(2).getId());
        }
        assertEquals(1, fixture.calls.get());
    }

    @Test
    public void retainsSearchAcrossCancellationAndAppliesItToNewResponse() throws Exception {
        Fixture fixture = new Fixture();
        fixture.json = SEARCH_DATA;
        fixture.model.setSearchQuery("validacoes");
        Result first = new Result();
        fixture.model.load(first);
        fixture.tokens.get(0).onToken("token");
        first.await();
        assertEquals(2, fixture.model.visibleNotifications().size());

        fixture.model.cancel();
        assertEquals("validacoes", fixture.model.getSearchQuery());
        assertTrue(fixture.model.visibleNotifications().isEmpty());
        fixture.json = SUCCESS.replace("Aviso", "Validações atualizadas");
        Result reloaded = new Result();
        fixture.model.load(reloaded);
        assertTrue(fixture.model.visibleNotifications().isEmpty());
        fixture.tokens.get(1).onToken("token");
        reloaded.await();
        assertEquals(1, fixture.model.visibleNotifications().size());
        assertEquals("Validações atualizadas", fixture.model.visibleNotifications().get(0).getTitle());
        assertEquals(2, fixture.calls.get());
    }

    @Test
    public void ownsLoadingAndDataAndPreventsConcurrentLoads() throws Exception {
        Fixture fixture = new Fixture();
        Result result = new Result();
        Result duplicate = new Result();
        fixture.model.load(result);
        fixture.model.load(duplicate);
        assertTrue(fixture.model.isLoading());
        assertTrue(fixture.model.getNotifications().isEmpty());
        assertEquals(1, fixture.tokens.size());
        fixture.tokens.get(0).onToken("token");
        result.await();
        assertFalse(fixture.model.isLoading());
        assertNull(result.failure);
        assertNotNull(result.notifications);
        assertSame(result.notifications, fixture.model.getNotifications());
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
        assertTrue(fixture.model.getNotifications().isEmpty());
        fixture.code = 200;
        Result retried = new Result();
        fixture.model.load(retried);
        fixture.tokens.get(1).onToken("token");
        retried.await();
        assertNotNull(retried.notifications);
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
        assertNotNull(current.notifications);
        assertSame(current.notifications, fixture.model.getNotifications());
        assertEquals(1, fixture.calls.get());
    }

    @Test
    public void cancelAndReloadDoNotRetainPreviousAccountData() throws Exception {
        Fixture fixture = new Fixture();
        Result first = new Result();
        fixture.model.load(first);
        fixture.tokens.get(0).onToken("token");
        first.await();
        assertNotNull(fixture.model.getNotifications());
        fixture.model.load(new Result());
        assertTrue(fixture.model.getNotifications().isEmpty());
        assertTrue(fixture.model.isLoading());
        fixture.model.cancel();
        assertTrue(fixture.model.getNotifications().isEmpty());
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
        assertTrue(fixture.model.getNotifications().isEmpty());
        assertEquals(0, result.deliveries.get());
        assertEquals(0, fixture.calls.get());
    }

    @Test
    public void tokenFailuresFinishSynchronouslyWithoutLeavingLoadingActive() throws Exception {
        for (IdTokenProvider.Failure failure : IdTokenProvider.Failure.values()) {
            NotificationsRepository repository = new NotificationsRepository(null,
                    (refresh, callback) -> callback.onFailure(failure));
            NotificationsViewModel model = new NotificationsViewModel(repository);
            Result result = new Result();
            model.load(result);
            result.await();
            assertFalse(model.isLoading());
            assertTrue(model.getNotifications().isEmpty());
            assertEquals(FailureKind.valueOf(failure.name()), result.failure);
            assertEquals(1, result.deliveries.get());
        }
    }

    private static final class Fixture {
        final List<IdTokenProvider.Callback> tokens = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        final NotificationsViewModel model;
        volatile int code = 200;
        volatile String json = SUCCESS;

        Fixture() {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                calls.incrementAndGet();
                return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                        .code(code).message("test")
                        .body(ResponseBody.create(MediaType.get("application/json"), json)).build();
            }).build();
            AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/").client(client)
                    .callbackExecutor(Runnable::run).addConverterFactory(GsonConverterFactory.create())
                    .build().create(AstroApi.class);
            model = new NotificationsViewModel(new NotificationsRepository(api,
                    (refresh, callback) -> tokens.add(callback)));
        }
    }

    private static final class Result implements NotificationsViewModel.ResultCallback {
        final CountDownLatch completed = new CountDownLatch(1);
        final AtomicInteger deliveries = new AtomicInteger();
        List<NotificationData> notifications;
        FailureKind failure;

        @Override
        public void onSuccess(List<NotificationData> notifications) {
            this.notifications = notifications;
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
            assertTrue("A consulta das notificações não terminou", completed.await(5, TimeUnit.SECONDS));
        }
    }
}
