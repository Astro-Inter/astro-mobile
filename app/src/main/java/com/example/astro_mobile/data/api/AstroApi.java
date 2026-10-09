package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ActivateRequest;
import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.VerifyEmailData;
import com.example.astro_mobile.data.api.dto.VerifyEmailRequest;
import com.example.astro_mobile.data.api.dto.VerifyKeyRequest;
import com.example.astro_mobile.data.api.dto.UserProfileData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface AstroApi {
    @GET("user/me")
    Call<ApiResponse<UserProfileData>> getProfile(@Header("Authorization") String authorization);

    @POST("verify-email")
    Call<ApiResponse<VerifyEmailData>> verifyEmail(@Body VerifyEmailRequest request);

    @POST("verify-key")
    Call<Void> verifyKey(@Body VerifyKeyRequest request);

    @POST("activate")
    Call<Void> activate(@Body ActivateRequest request);
}
