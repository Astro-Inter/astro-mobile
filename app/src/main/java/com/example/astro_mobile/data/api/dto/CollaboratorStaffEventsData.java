package com.example.astro_mobile.data.api.dto;

import java.util.List;

public final class CollaboratorStaffEventsData {
    public Long collaboratorId;
    public String collaboratorName;
    public List<CollaboratorStaffEventData> events;

    public boolean isValidFor(long requestedCollaboratorId) {
        return collaboratorId != null && collaboratorId == requestedCollaboratorId && events != null;
    }
}
