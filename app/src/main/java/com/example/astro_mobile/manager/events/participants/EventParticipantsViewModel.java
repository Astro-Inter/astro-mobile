package com.example.astro_mobile.manager.events.participants;

import androidx.lifecycle.ViewModel;

import com.example.astro_mobile.manager.events.ManagerEvent;
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

    public void initialize(ManagerEvent.CompletionMode mode) {
        // Guarda os mocks uma vez, mantendo pesquisa e filtro ao recriar as Views.
        if (participants == null) participants = ParticipantSamples.forMode(mode);
    }

    public Filter getFilter() { return filter; }
    public void setFilter(Filter filter) { this.filter = filter; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }

    public List<EventParticipant> visibleParticipants() {
        // Combina os filtros somente nos exemplos locais; não define regras do backend.
        List<EventParticipant> result = new ArrayList<>();
        String search = normalize(query.trim());
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
