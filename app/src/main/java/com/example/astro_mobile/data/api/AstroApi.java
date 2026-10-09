package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ActivateRequest;
import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.VerifyEmailData;
import com.example.astro_mobile.data.api.dto.VerifyEmailRequest;
import com.example.astro_mobile.data.api.dto.VerifyKeyRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AstroApi {
    @POST("verify-email")
    Call<ApiResponse<VerifyEmailData>> verifyEmail(@Body VerifyEmailRequest request);

    @POST("verify-key")
    Call<Void> verifyKey(@Body VerifyKeyRequest request);

    @POST("activate")
    Call<Void> activate(@Body ActivateRequest request);
}
