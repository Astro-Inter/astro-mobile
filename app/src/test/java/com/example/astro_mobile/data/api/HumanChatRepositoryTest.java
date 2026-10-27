package com.example.astro_mobile.data.api;

import static org.junit.Assert.*;

import com.example.astro_mobile.data.api.dto.ChatConversationData;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import org.junit.Test;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class HumanChatRepositoryTest {
    private static final String SUCCESS = "[{\"id\":\"conversation-2\",\"participant\":{"
            + "\"id\":42,\"name\":\"João\",\"role\":\"GESTOR\","
            + "\"profilePhotoUrl\":\"https://example.com/photo.jpg\",\"avatarInitials\":\"JO\"},"
            + "\"lastMessage\":{\"senderId\":42,\"content\":\"Olá\"},\"unreadCount\":2},"
            + "{\"id\":\"conversation-1\",\"participant\":{\"id\":23,\"name\":\"Ana\","
            + "\"role\":\"COLABORADOR\",\"avatarInitials\":\"AN\"},"
            + "\"lastMessage\":{\"senderId\":1,\"content\":\"Tudo bem?\"},\"unreadCount\":0}]";

    @Test public void consumesDirectArrayWithBearerAndPreservesOrderAndMetadata() throws Exception {
        Result result = load(repository(chain -> {
            assertEquals("GET", chain.request().method());
            assertEquals("/chats", chain.request().url().encodedPath());
            assertEquals("Bearer token", chain.request().header("Authorization"));
            assertNull(chain.request().url().query());
            return response(chain, 200, SUCCESS);
        }, tokens()));
        assertNull(result.failure);
        assertEquals(2, result.items.size());
        ChatConversationData first = result.items.get(0);
        assertEquals("conversation-2", first.id);
        assertEquals("João", first.participant.name);
        assertEquals("https://example.com/photo.jpg", first.participant.profilePhotoUrl);
        assertEquals("JO", first.participant.avatarInitials);
        assertEquals("Olá", first.lastMessage.content);
        assertEquals(2, first.unreadCount);
        assertEquals("conversation-1", result.items.get(1).id);
        assertNull(result.items.get(1).participant.profilePhotoUrl);
    }

    @Test public void acceptsEmptyArrayAndRejectsUnexpectedEnvelope() throws Exception {
        Result empty = load(repository(chain -> response(chain, 200, "[]"), tokens()));
        assertNotNull(empty.items);
        assertTrue(empty.items.isEmpty());
        for (String body : new String[]{"{\"success\":true,\"data\":[]}", "null", "[{}]"}) {
            Result invalid = load(repository(chain -> response(chain, 200, body), tokens()));
            assertEquals(FailureKind.INTERNAL, invalid.failure);
        }
    }

    @Test public void refreshesOnceAndUsesNewTokenOnRetry() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        List<Boolean> refreshes = new ArrayList<>();
        Result result = load(repository(chain -> {
            int call = calls.getAndIncrement();
            assertEquals(call == 0 ? "Bearer token" : "Bearer renewed",
                    chain.request().header("Authorization"));
            return response(chain, call == 0 ? 401 : 200, SUCCESS);
        }, (refresh, callback) -> {
            refreshes.add(refresh);
            callback.onToken(refresh ? "renewed" : "token");
        }));
        assertNull(result.failure);
        assertEquals(Arrays.asList(false, true), refreshes);
        assertEquals(2, calls.get());
    }

    @Test public void repeatedUnauthorizedStopsWithSessionFailure() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Result result = load(repository(chain -> {
            calls.incrementAndGet();
            return response(chain, 401, "{}");
        }, tokens()));
        assertEquals(FailureKind.SESSION, result.failure);
        assertEquals(2, calls.get());
    }

    @Test public void distinguishesConnectivityFromServerErrorsAndTimeouts() throws Exception {
        Result offline = load(repository(chain -> { throw new UnknownHostException(); }, tokens()));
        assertEquals(FailureKind.CONNECTION, offline.failure);
        Result timeout = load(repository(chain -> { throw new SocketTimeoutException(); }, tokens()));
        assertEquals(FailureKind.INTERNAL, timeout.failure);
        for (int code : new int[]{403, 500, 503}) {
            Result server = load(repository(chain -> response(chain, code, "{}"), tokens()));
            assertEquals(FailureKind.INTERNAL, server.failure);
        }
    }

    @Test public void tokenFailuresDoNotCallApi() throws Exception {
        for (IdTokenProvider.Failure failure : IdTokenProvider.Failure.values()) {
            Result result = load(repository(chain -> {
                fail("A falha do token deve impedir a requisição");
                return response(chain, 200, SUCCESS);
            }, (refresh, callback) -> callback.onFailure(failure)));
            assertEquals(FailureKind.valueOf(failure.name()), result.failure);
        }
    }

    @Test public void cancellationDiscardsOldSessionResult() throws Exception {
        List<IdTokenProvider.Callback> pending = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        HumanChatRepository repository = repository(chain -> {
            calls.incrementAndGet();
            return response(chain, 200, SUCCESS);
        }, (refresh, callback) -> pending.add(callback));
        Result old = new Result();
        repository.load(old);
        repository.cancel();
        Result current = new Result();
        repository.load(current);
        pending.get(0).onToken("old");
        pending.get(1).onToken("current");
        assertTrue(current.completed.await(5, TimeUnit.SECONDS));
        assertEquals(1, old.completed.getCount());
        assertEquals(1, calls.get());
    }

    private IdTokenProvider tokens() {
        return (refresh, callback) -> callback.onToken("token");
    }

    private HumanChatRepository repository(Interceptor interceptor, IdTokenProvider tokens) {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/").client(client)
                .callbackExecutor(Runnable::run).addConverterFactory(GsonConverterFactory.create())
                .build().create(AstroApi.class);
        return new HumanChatRepository(api, tokens);
    }

    private Response response(Interceptor.Chain chain, int code, String json) {
        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("test")
                .body(ResponseBody.create(MediaType.get("application/json"), json)).build();
    }

    private Result load(HumanChatRepository repository) throws Exception {
        Result result = new Result();
        repository.load(result);
        assertTrue("A consulta não terminou", result.completed.await(5, TimeUnit.SECONDS));
        return result;
    }

    private static final class Result implements HumanChatRepository.ResultCallback {
        final CountDownLatch completed = new CountDownLatch(1);
        List<ChatConversationData> items;
        FailureKind failure;
        @Override public void onSuccess(List<ChatConversationData> items) {
            this.items = items;
            completed.countDown();
        }
        @Override public void onFailure(FailureKind failure) {
            this.failure = failure;
            completed.countDown();
        }
    }
}
