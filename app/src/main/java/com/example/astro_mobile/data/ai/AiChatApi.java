package com.example.astro_mobile.data.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Path;

public interface AiChatApi {
    @GET("sessions")
    Call<SessionsPage> listSessions(@Header("Authorization") String authorization,
                                   @Query("limit") int limit, @Query("cursor") String cursor);

    @GET("sessions/{session_id}/messages")
    Call<SessionHistory> sessionHistory(@Header("Authorization") String authorization, @Path("session_id") String sessionId);

    @POST("sessions/{session_id}/iniciar")
    Call<SessionResult> resumeSession(@Header("Authorization") String authorization, @Path("session_id") String sessionId);

    @POST("sessions/{session_id}/encerrar")
    Call<SessionResult> endSession(@Header("Authorization") String authorization, @Path("session_id") String sessionId);

    @GET("integracoes/google-calendar/conectar")
    Call<GoogleCalendarConnectResponse> connectGoogleCalendar(@Header("Authorization") String authorization);

    @POST("chat/messages")
    Call<ChatResponse> sendMessage(@Header("Authorization") String authorization,
                                   @Query("markdown") boolean markdown,
                                   @Body ChatRequest request);
}
