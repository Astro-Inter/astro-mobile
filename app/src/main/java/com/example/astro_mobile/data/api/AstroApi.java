package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ActivateRequest;
import com.example.astro_mobile.data.api.dto.ReceivedFormDetails;
import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.VerifyEmailData;
import com.example.astro_mobile.data.api.dto.VerifyEmailRequest;
import com.example.astro_mobile.data.api.dto.VerifyKeyRequest;
import com.example.astro_mobile.data.api.dto.UserProfileData;
import com.example.astro_mobile.data.api.dto.CollaboratorHomeData;
import com.example.astro_mobile.data.api.dto.ManagerHomeData;
import com.example.astro_mobile.data.api.dto.NotificationData;
import com.example.astro_mobile.data.api.dto.NotificationDetailsData;
import com.example.astro_mobile.data.api.dto.CollaboratorEventData;
import com.example.astro_mobile.data.api.dto.CollaboratorEventDetailsData;
import com.example.astro_mobile.data.api.dto.ChatConversationData;
import com.example.astro_mobile.data.api.dto.ManagerEventData;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.PUT;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;
import com.example.astro_mobile.data.api.dto.ReceivedFormsPage;
import com.example.astro_mobile.data.api.dto.FormResponseRequest;
import com.example.astro_mobile.data.api.dto.FormImageUrl;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public interface AstroApi {
    @GET("events/collaborator/{eventId}")
    Call<ApiResponse<CollaboratorEventDetailsData>> getCollaboratorEvent(
            @Header("Authorization") String authorization, @Path("eventId") long eventId);

    @POST("events/{eventId}/completion")
    Call<ApiResponse<Object>> completeCollaboratorEvent(@Header("Authorization") String authorization,
            @Path("eventId") long eventId, @Body RequestBody body);

    @GET("chats")
    Call<List<ChatConversationData>> getChats(@Header("Authorization") String authorization);
    @GET("forms/received")
    Call<ApiResponse<ReceivedFormsPage>> getReceivedForms(
            @Header("Authorization") String authorization, @Query("page") int page,
            @Query("size") int size);

    @GET("notifications/{notificationId}")
    Call<ApiResponse<NotificationDetailsData>> getNotification(
            @Header("Authorization") String authorization, @Path("notificationId") String id);

    @PATCH("notifications/{notificationId}/viewed")
    Call<ApiResponse<Void>> markNotificationViewed(
            @Header("Authorization") String authorization, @Path("notificationId") String id);

    @GET("events/collaborator")
    Call<ApiResponse<List<CollaboratorEventData>>> getCollaboratorEvents(
            @Header("Authorization") String authorization);

    @GET("notifications")
    Call<ApiResponse<List<NotificationData>>> getNotifications(
            @Header("Authorization") String authorization);

    @GET("home/manager")
    Call<ApiResponse<ManagerHomeData>> getManagerHome(
            @Header("Authorization") String authorization);

    @GET("events/my-events")
    Call<ApiResponse<List<ManagerEventData>>> getManagerEvents(@Header("Authorization") String authorization);

    @GET("home/collaborator")
    Call<ApiResponse<CollaboratorHomeData>> getCollaboratorHome(
            @Header("Authorization") String authorization);

    @GET("forms/received/{id}")
    Call<ApiResponse<ReceivedFormDetails>> getReceivedForm(
            @Header("Authorization") String authorization, @Path("id") String id);

    @POST("forms/{id}/responses")
    Call<ApiResponse<ReceivedFormDetails>> submitFormResponse(
            @Header("Authorization") String authorization, @Path("id") String id,
            @Body FormResponseRequest request);

    @Multipart
    @POST("forms/{id}/responses")
    Call<ApiResponse<ReceivedFormDetails>> submitFormResponseWithFiles(
            @Header("Authorization") String authorization, @Path("id") String id,
            @Part("response") RequestBody response, @Part List<MultipartBody.Part> files);

    @GET("forms/{id}/responses")
    Call<ApiResponse<ReceivedFormDetails>> getFormResponses(
            @Header("Authorization") String authorization, @Path("id") String id);

    @GET("forms/{id}/images/{imageId}")
    Call<ApiResponse<FormImageUrl>> getFormImageUrl(
            @Header("Authorization") String authorization, @Path("id") String formId,
            @Path("imageId") String imageId);

    @GET("user/me")
    Call<ApiResponse<UserProfileData>> getProfile(@Header("Authorization") String authorization);

    @Multipart
    @PUT("user/me/profile-photo")
    Call<Void> updateProfilePhoto(@Header("Authorization") String authorization,
                                 @Part MultipartBody.Part file);

    @POST("verify-email")
    Call<ApiResponse<VerifyEmailData>> verifyEmail(@Body VerifyEmailRequest request);

    @POST("verify-key")
    Call<Void> verifyKey(@Body VerifyKeyRequest request);

    @POST("activate")
    Call<Void> activate(@Body ActivateRequest request);
}
