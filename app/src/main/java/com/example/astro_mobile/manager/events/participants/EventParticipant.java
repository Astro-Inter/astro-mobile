package com.example.astro_mobile.manager.events.participants;

public final class EventParticipant {
    public enum Status { VALIDATION_PENDING, AWAITING_COMPLETION, VALIDATED, REJECTED }

    public final String name;
    public final Status status;
    public final int avatarIndex;
    public final Long id;
    public final String photoUrl;

    public EventParticipant(com.example.astro_mobile.data.api.dto.EventParticipantData data) {
        id = data.participantId;
        name = data.participantName;
        photoUrl = data.profilePhotoUrl;
        avatarIndex = 0;
        switch (data.participationStatus) {
            case "PENDENTE": status = Status.AWAITING_COMPLETION; break;
            case "EM_ANALISE": status = Status.VALIDATION_PENDING; break;
            case "CONCLUIDO": status = Status.VALIDATED; break;
            case "REJEITADO": status = Status.REJECTED; break;
            default: throw new IllegalArgumentException("Estado de participação desconhecido");
        }
    }
}
