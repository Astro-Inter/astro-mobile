package com.example.astro_mobile.manager.events.assignment;

import androidx.lifecycle.ViewModel;
import com.example.astro_mobile.data.api.EventAssignmentRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.EventAssignmentData;

/** Mantém os dados da tela e impede duas operações simultâneas. */
public final class EventAssignmentViewModel extends ViewModel {
    public interface Callback { void onSuccess(); void onFailure(FailureKind failure); }
    private final EventAssignmentRepository repository = EventAssignmentRepository.createDefault();
    private EventAssignmentData details;
    private boolean loading;
    public EventAssignmentData getDetails() { return details; }
    public boolean isLoading() { return loading; }
    public void load(long eventId, long participantId, Callback callback) { execute(eventId, participantId, false, callback); }
    public void submit(long eventId, long participantId, Callback callback) { execute(eventId, participantId, true, callback); }
    private void execute(long eventId, long participantId, boolean submit, Callback callback) {
        if (loading) return;
        loading = true;
        EventAssignmentRepository.Callback<EventAssignmentData> result = new EventAssignmentRepository.Callback<EventAssignmentData>() {
            @Override public void onSuccess(EventAssignmentData data) { loading = false; if (!submit) details = data; callback.onSuccess(); }
            @Override public void onFailure(FailureKind failure) { loading = false; callback.onFailure(failure); }
        };
        if (submit) repository.submit(eventId, participantId, result);
        else repository.load(eventId, participantId, result);
    }
    public void cancel() { repository.cancel(); loading = false; }
    @Override protected void onCleared() { cancel(); }
}
