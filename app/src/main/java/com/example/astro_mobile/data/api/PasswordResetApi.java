package com.example.astro_mobile.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PasswordResetApi {
    @POST("auth/forgot-password")
    Call<ResetResponse> requestReset(@Body ResetRequest request);

    final class ResetRequest {
        public final String email;
        public ResetRequest(String email) { this.email = email; }
    }

    final class ResetResponse {
        public boolean success;
        public ResetError error;
    }

    final class ResetError {
        public String code;
    }
}
