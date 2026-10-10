package com.example.astro_mobile.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.astro_mobile.data.api.dto.UserProfileData;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class UserProfileRepositoryTest {
    @Test
    public void authenticatedProfileIncludesPhotoAndUserInformation() throws InterruptedException {
        UserProfileData profile = request("\"profilePhotoUrl\":\"https://example.com/photo.png?signature=test\",");
        assertEquals("https://example.com/photo.png?signature=test", profile.getProfilePhotoUrl());
        assertEquals("Ana", profile.getNome());
        assertEquals("Eletricista", profile.getCargo());
        assertEquals("Osasco", profile.getUnidade());
        assertEquals("Presencial", profile.getModalidade());
        assertEquals("ana@example.com", profile.getEmail());
        assertEquals(10, profile.getNrs().get(0).getCode());
        assertEquals("2027-10-10", profile.getNrs().get(0).getValidade());
    }

    @Test
    public void profileWithoutPhotoStillLoadsUserInformation() throws InterruptedException {
        for (String field : new String[] { "", "\"profilePhotoUrl\":null,", "\"profilePhotoUrl\":\"  \"," }) {
            UserProfileData profile = request(field);
            assertNull(profile.getProfilePhotoUrl());
            assertEquals("Ana", profile.getNome());
            assertEquals(1, profile.getNrs().size());
        }
    }

    private UserProfileData request(String photoField) throws InterruptedException {
        String json = "{\"success\":true,\"data\":{" + photoField
                + "\"nome\":\"Ana\",\"cargo\":\"Eletricista\",\"unidade\":\"Osasco\","
                + "\"modalidade\":\"Presencial\",\"email\":\"ana@example.com\","
                + "\"nrs\":[{\"code\":10,\"validade\":\"2027-10-10\"}]}}";
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            assertEquals("GET", chain.request().method());
            assertEquals("/user/me", chain.request().url().encodedPath());
            assertEquals("Bearer test-token", chain.request().header("Authorization"));
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("test")
                    .body(ResponseBody.create(MediaType.get("application/json"), json)).build();
        }).build();
        AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/").client(client)
                .callbackExecutor(Runnable::run).addConverterFactory(GsonConverterFactory.create())
                .build().create(AstroApi.class);
        AtomicReference<UserProfileData> result = new AtomicReference<>();
        AtomicReference<FailureKind> failure = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        new UserProfileRepository(api, (refresh, callback) -> callback.onToken("test-token"))
                .load(new UserProfileRepository.ResultCallback() {
                    @Override
                    public void onSuccess(UserProfileData profile) {
                        result.set(profile);
                        completed.countDown();
                    }

                    @Override
                    public void onFailure(FailureKind kind) {
                        failure.set(kind);
                        completed.countDown();
                    }
                });
        assertTrue("A consulta do perfil não terminou", completed.await(5, TimeUnit.SECONDS));
        assertNull(failure.get());
        return result.get();
    }
}
