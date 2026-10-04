package com.example.astro_mobile.data.ai;

import com.google.gson.annotations.SerializedName;

public final class GoogleCalendarConnectResponse {
    @SerializedName("authorization_url") private String authorizationUrl;

    public String getAuthorizationUrl() { return authorizationUrl; }
}
