package com.example.astro_mobile.manager.events;

import androidx.lifecycle.ViewModel;

import com.example.astro_mobile.manager.events.mock.ManagerEventSamples;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ManagerEventsViewModel extends ViewModel {
    public enum Filter { ALL, IN_PROGRESS, FINISHED }

    private final List<ManagerEvent> events = ManagerEventSamples.events();
    private Filter filter = Filter.ALL;
    private String query = "";
    private boolean loaded;

    public Filter getFilter() { return filter; }
    public void setFilter(Filter filter) { this.filter = filter; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }

    public List<ManagerEvent> visibleEvents() {
        // Combina o texto pesquisado com o filtro, somente sobre os mocks locais.
        List<ManagerEvent> result = new ArrayList<>();
        String search = normalize(query.trim());
        for (ManagerEvent event : events) {
            boolean finished = event.status == ManagerEvent.Status.FINISHED;
            if (filter == Filter.IN_PROGRESS && finished) continue;
            if (filter == Filter.FINISHED && !finished) continue;
            if (normalize(event.title).contains(search)) result.add(event);
        }
        return result;
    }

    private String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
