package com.example.astro_mobile.manager.events.assignment;

import androidx.lifecycle.ViewModel;
import com.example.astro_mobile.data.api.EventAssignmentRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.ManagerEventStatusRepository;
import com.example.astro_mobile.data.api.dto.EventAssignmentData;
import com.example.astro_mobile.manager.events.CompletionDeadline;
import java.time.LocalDateTime;

/** Mantém os dados da tela e impede duas operações simultâneas. */
public final class EventAssignmentViewModel extends ViewModel {
    public interface Callback {
        void onSuccess(); void onFailure(FailureKind failure);
        default void onCancelled() { }
        default void onDeadlineChanged() { }
    }
    private final EventAssignmentRepository repository = EventAssignmentRepository.createDefault();
    private final ManagerEventStatusRepository statuses = new ManagerEventStatusRepository();
    private EventAssignmentData details;
    private String eventStatus;
    private boolean loading;
    public EventAssignmentData getDetails() { return details; }
    public boolean isLoading() { return loading; }
    public String getEventStatus() { return eventStatus; }
    public void load(long eventId, long participantId, Callback callback) { execute(eventId, participantId, false, callback); }
    public void submit(long eventId, long participantId, Callback callback) { execute(eventId, participantId, true, callback); }
    private void execute(long eventId, long participantId, boolean submit, Callback callback) {
        if (loading) return;
        loading = true;
        EventAssignmentRepository.Callback<EventAssignmentData> result = new EventAssignmentRepository.Callback<EventAssignmentData>() {
            @Override public void onSuccess(EventAssignmentData data) {
                if (submit) { loading = false; callback.onSuccess(); return; }
                details = data;
                statuses.load(eventId, new ManagerEventStatusRepository.Callback() {
                    @Override public void onSuccess(String status) { eventStatus = status; loading = false; callback.onSuccess(); }
                    @Override public void onFailure(FailureKind failure) { loading = false; callback.onFailure(failure); }
                });
            }
            @Override public void onFailure(FailureKind failure) { loading = false; callback.onFailure(failure); }
        };
        if (submit) statuses.load(eventId, new ManagerEventStatusRepository.Callback() {
            @Override public void onSuccess(String status) {
                eventStatus = status;
                if ("CANCELADO".equals(status)) { loading = false; callback.onCancelled(); }
                else if (details == null || CompletionDeadline.assignment(details.endDate, LocalDateTime.now())
                        != CompletionDeadline.Result.ALLOWED) { loading = false; callback.onDeadlineChanged(); }
                else repository.submit(eventId, participantId, result);
            }
            @Override public void onFailure(FailureKind failure) { loading = false; callback.onFailure(failure); }
        });
        else repository.load(eventId, participantId, result);
    }
    public void cancel() { repository.cancel(); statuses.cancel(); loading = false; }
    @Override protected void onCleared() { cancel(); }
}
