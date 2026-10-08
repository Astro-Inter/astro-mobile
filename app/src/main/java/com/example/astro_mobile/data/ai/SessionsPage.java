package com.example.astro_mobile.data.ai;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public final class SessionsPage {
    public List<SessionSummary> sessions;
    @SerializedName("next_cursor") public String nextCursor;

    public boolean isValid() {
        if (sessions == null || (nextCursor != null && (nextCursor.isEmpty() || nextCursor.length() > 1024))) return false;
        for (SessionSummary session : sessions) if (session == null || !session.isValid()) return false;
        return true;
    }
}
