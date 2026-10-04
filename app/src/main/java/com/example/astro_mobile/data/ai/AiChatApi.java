package com.example.astro_mobile.data.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AiChatApi {
    @POST("chat/messages")
    Call<ChatResponse> sendMessage(@Header("Authorization") String authorization,
                                   @Query("markdown") boolean markdown,
                                   @Body ChatRequest request);
}
