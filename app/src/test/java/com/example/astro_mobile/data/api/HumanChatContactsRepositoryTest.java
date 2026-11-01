package com.example.astro_mobile.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.astro_mobile.data.api.dto.ChatContactData;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class HumanChatContactsRepositoryTest {
    @Test public void parsesProfilePhotoUrlFromContactsResponse() throws Exception {
        String json = "{\"success\":true,\"data\":[{\"id\":42,\"name\":\"João\","
                + "\"role\":\"GESTOR\",\"profilePhotoUrl\":\"https://cdn.example/photo.jpg?sig=x\"},"
                + "{\"id\":23,\"name\":\"Ana\",\"role\":\"COLABORADOR\",\"profilePhotoUrl\":null}]}";
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            assertEquals("GET", chain.request().method());
            assertEquals("/chats/contacts", chain.request().url().encodedPath());
            assertEquals("Bearer token", chain.request().header("Authorization"));
            return new okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK")
                    .body(ResponseBody.create(MediaType.get("application/json"), json)).build();
        }).build();
        AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/").client(client)
                .callbackExecutor(Runnable::run).addConverterFactory(GsonConverterFactory.create())
                .build().create(AstroApi.class);
        HumanChatContactsRepository repository = new HumanChatContactsRepository(api,
                (refresh, callback) -> callback.onToken("token"));
        Result result = new Result();

        repository.load(result);

        assertTrue("A consulta não terminou", result.completed.await(5, TimeUnit.SECONDS));
        assertNull(result.failure);
        assertEquals(2, result.contacts.size());
        assertEquals("https://cdn.example/photo.jpg?sig=x", result.contacts.get(0).profilePhotoUrl);
        assertNull(result.contacts.get(1).profilePhotoUrl);
    }

    private static final class Result implements HumanChatContactsRepository.ResultCallback {
        final CountDownLatch completed = new CountDownLatch(1);
        List<ChatContactData> contacts;
        FailureKind failure;

        @Override public void onSuccess(List<ChatContactData> contacts) {
            this.contacts = contacts;
            completed.countDown();
        }

        @Override public void onFailure(FailureKind failure) {
            this.failure = failure;
            completed.countDown();
        }
    }
}
