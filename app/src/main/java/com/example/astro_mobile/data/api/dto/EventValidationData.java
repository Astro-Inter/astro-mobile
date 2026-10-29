package com.example.astro_mobile.data.api.dto;

import java.util.List;

/** Dados e evidências da conclusão enviada pelo colaborador. */
public final class EventValidationData {
    public String profilePhotoUrl, participantName, roleName, unitName, status, completionDate, endDate, eventTitle;
    public Boolean evidenceMandatory;
    public List<Evidence> evidences;
    public static final class Evidence {
        public Long evidenceId, sizeBytes;
        public String originalName, url, mimeType;
    }
}
