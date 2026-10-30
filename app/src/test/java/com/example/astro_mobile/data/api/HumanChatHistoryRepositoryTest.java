package com.example.astro_mobile.data.api;

import static org.junit.Assert.*;
import com.example.astro_mobile.data.api.dto.ChatMessagePageData;
import com.example.astro_mobile.data.firebase.IdTokenProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class HumanChatHistoryRepositoryTest {
    private static final String CONVERSATION = "11111111-2222-4333-8444-555555555555";
    private static final String PAGE = "{\"success\":true,\"data\":{\"messages\":[],\"nextCursor\":\"cursor-opaco\",\"hasMore\":true}}";

    @Test public void confirmsReadBeforeFetchingThirtyMessagesAndUsesOpaqueCursor() throws Exception {
        List<String> requests = new ArrayList<>();
        HumanChatHistoryRepository repository = repository(chain -> {
            assertEquals("Bearer token", chain.request().header("Authorization"));
            requests.add(chain.request().method());
            if ("PATCH".equals(chain.request().method())) {
                assertEquals("/chats/" + CONVERSATION + "/read", chain.request().url().encodedPath());
                return response(chain, 204, "");
            }
            assertEquals("30", chain.request().url().queryParameter("limit"));
            assertEquals(requests.size() == 2 ? null : "cursor-opaco", chain.request().url().queryParameter("before"));
            return response(chain, 200, PAGE);
        }, (refresh, cb) -> cb.onToken("token"));
        Result first = new Result(); repository.loadConversation(CONVERSATION, first); first.await();
        assertEquals(List.of("PATCH", "GET"), requests);
        assertTrue(first.markedRead);
        assertEquals("cursor-opaco", first.page.nextCursor);
        Result older = new Result(); repository.loadOlder(CONVERSATION, first.page.nextCursor, older); older.await();
        assertFalse(older.markedRead);
        assertEquals(List.of("PATCH", "GET", "GET"), requests);
    }

    @Test public void failedReadNeverClearsUnreadOrFetchesHistory() throws Exception {
        for (int code : new int[]{400, 401, 403, 404, 503}) {
            HumanChatHistoryRepository repository = repository(chain -> {
                assertEquals("PATCH", chain.request().method());
                return response(chain, code, "{}");
            }, (refresh, cb) -> cb.onToken("token"));
            Result result = new Result(); repository.loadConversation(CONVERSATION, result); result.await();
            assertFalse(result.markedRead);
            assertNull(result.page);
            assertEquals(code == 401 ? FailureKind.SESSION : FailureKind.INTERNAL, result.failure);
        }
    }

    @Test public void cancelDiscardsTokenFromPreviousSession() {
        List<IdTokenProvider.Callback> callbacks = new ArrayList<>();
        HumanChatHistoryRepository repository = repository(chain -> { fail("Não deve acessar rede"); return null; },
                (refresh, cb) -> callbacks.add(cb));
        Result result = new Result(); repository.loadConversation(CONVERSATION, result); repository.cancel();
        callbacks.get(0).onToken("old-session");
        assertEquals(1, result.done.getCount());
    }

    private static HumanChatHistoryRepository repository(Interceptor interceptor, IdTokenProvider tokens) {
        AstroApi api = new Retrofit.Builder().baseUrl("https://example.test/")
                .client(new OkHttpClient.Builder().addInterceptor(interceptor).build())
                .addConverterFactory(GsonConverterFactory.create()).build().create(AstroApi.class);
        return new HumanChatHistoryRepository(api, tokens);
    }
    private static Response response(Interceptor.Chain chain, int code, String body) {
        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("test")
                .body(ResponseBody.create(MediaType.get("application/json"), body)).build();
    }
    private static final class Result implements HumanChatHistoryRepository.ResultCallback {
        final CountDownLatch done = new CountDownLatch(1);
        ChatMessagePageData page;
        FailureKind failure;
        boolean markedRead;
        @Override public void onSuccess(ChatMessagePageData page) { this.page = page; done.countDown(); }
        @Override public void onFailure(FailureKind failure) { this.failure = failure; done.countDown(); }
        @Override public void onMarkedRead() { markedRead = true; }
        void await() throws Exception { assertTrue(done.await(5, TimeUnit.SECONDS)); }
    }
}
