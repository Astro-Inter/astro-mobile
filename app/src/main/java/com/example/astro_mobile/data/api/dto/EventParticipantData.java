package com.example.astro_mobile.data.api.dto;

/** Participante e situação devolvidos pela API para o gestor. */
public final class EventParticipantData {
    public Long participantId;
    public String participantName, eventTitle, participationStatus, profilePhotoUrl;
    public boolean isValid() {
        return participantId != null && participantName != null && participationStatus != null;
    }
}
