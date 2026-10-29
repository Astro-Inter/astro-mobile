package com.example.astro_mobile.manager.events.participants;

import androidx.lifecycle.ViewModel;

import com.example.astro_mobile.manager.events.ManagerEvent;
import com.example.astro_mobile.data.api.EventParticipantsRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.EventParticipantData;
import com.example.astro_mobile.manager.events.participants.mock.ParticipantSamples;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EventParticipantsViewModel extends ViewModel {
    public enum Filter { ALL, FINISHED, NOT_FINISHED }

    private List<EventParticipant> participants;
    private Filter filter = Filter.ALL;
    private String query = "";
    private boolean loaded;
    private boolean loading;
    private final EventParticipantsRepository repository = EventParticipantsRepository.createDefault();
    public interface Callback { void onSuccess(); void onFailure(FailureKind failure); }

    public void initialize(ManagerEvent.CompletionMode mode) {
        // Guarda os mocks uma vez, mantendo pesquisa e filtro ao recriar as Views.
        if (participants == null && mode == ManagerEvent.CompletionMode.MANAGER_ASSIGNMENT) participants = ParticipantSamples.forMode(mode);
    }

    public Filter getFilter() { return filter; }
    public void setFilter(Filter filter) { this.filter = filter; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }
    public void load(long eventId, Callback callback) {
        if (loading) return;
        loading = true;
        repository.load(eventId, new EventParticipantsRepository.ResultCallback() {
            @Override public void onSuccess(List<EventParticipantData> data) {
                loading = false;
                List<EventParticipant> result = new ArrayList<>();
                try { for (EventParticipantData item : data) result.add(new EventParticipant(item)); }
                catch (IllegalArgumentException error) { callback.onFailure(FailureKind.INTERNAL); return; }
                participants = result; loaded = true; callback.onSuccess();
            }
            @Override public void onFailure(FailureKind failure) { loading = false; callback.onFailure(failure); }
        });
    }
    public void cancel() { repository.cancel(); loading = false; }
    @Override protected void onCleared() { cancel(); }

    public List<EventParticipant> visibleParticipants() {
        // Combina os filtros somente nos exemplos locais; não define regras do backend.
        List<EventParticipant> result = new ArrayList<>();
        String search = normalize(query.trim());
        if (participants == null) return result;
        for (EventParticipant participant : participants) {
            boolean finished = participant.status == EventParticipant.Status.VALIDATED
                    || participant.status == EventParticipant.Status.ASSIGNED;
            if (filter == Filter.FINISHED && !finished) continue;
            if (filter == Filter.NOT_FINISHED && finished) continue;
            if (normalize(participant.name).contains(search)) result.add(participant);
        }
        return result;
    }

    private String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
