package com.example.astro_mobile.manager.events.validation;

import androidx.lifecycle.ViewModel;
import com.example.astro_mobile.data.api.EventValidationRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.ManagerEventStatusRepository;
import com.example.astro_mobile.data.api.dto.EventValidationData;

/** Mantém os dados e a justificativa enquanto o gestor navega. */
public final class EventValidationViewModel extends ViewModel {
    public interface Callback {
        void onSuccess(); void onFailure(FailureKind failure);
        default void onCancelled() { }
    }
    private final EventValidationRepository repository = EventValidationRepository.createDefault();
    private final ManagerEventStatusRepository statuses = new ManagerEventStatusRepository();
    private EventValidationData details;
    private String eventStatus;
    private String reason = "";
    private boolean loading;
    public EventValidationData getDetails() { return details; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isLoading() { return loading; }
    public String getEventStatus() { return eventStatus; }
    public void load(long eventId, long participantId, Callback callback) {
        if (loading) return;
        loading = true;
        repository.load(eventId, participantId, new EventValidationRepository.Callback<EventValidationData>() {
            @Override public void onSuccess(EventValidationData data) {
                details = data;
                statuses.load(eventId, new ManagerEventStatusRepository.Callback() {
                    @Override public void onSuccess(String status) { eventStatus = status; loading = false; callback.onSuccess(); }
                    @Override public void onFailure(FailureKind failure) { loading = false; callback.onFailure(failure); }
                });
            }
            @Override public void onFailure(FailureKind failure) { loading = false; callback.onFailure(failure); }
        });
    }
    public void decide(long eventId, long participantId, boolean reject, Callback callback) {
        if (loading) return;
        loading = true;
        statuses.load(eventId, new ManagerEventStatusRepository.Callback() {
            @Override public void onSuccess(String status) {
                eventStatus = status;
                if ("CANCELADO".equals(status)) { loading = false; callback.onCancelled(); return; }
                repository.decide(eventId, participantId, reject ? reason.trim() : null, new EventValidationRepository.Callback<Object>() {
                    @Override public void onSuccess(Object data) { loading = false; callback.onSuccess(); }
                    @Override public void onFailure(FailureKind failure) { loading = false; callback.onFailure(failure); }
                });
            }
            @Override public void onFailure(FailureKind failure) { loading = false; callback.onFailure(failure); }
        });
    }
    public void cancel() { repository.cancel(); statuses.cancel(); loading = false; }
    @Override protected void onCleared() { cancel(); }
}
