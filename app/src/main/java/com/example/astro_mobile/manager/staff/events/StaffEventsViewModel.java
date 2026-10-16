package com.example.astro_mobile.manager.staff.events;

import androidx.lifecycle.ViewModel;

import com.example.astro_mobile.manager.staff.events.mock.StaffEventSamples;

import java.util.ArrayList;
import java.util.List;

public final class StaffEventsViewModel extends ViewModel {
    public enum Filter {
        ALL(null), PENDING(StaffEvent.Status.PENDING), DONE(StaffEvent.Status.DONE), EXPIRED(StaffEvent.Status.EXPIRED);

        final StaffEvent.Status status;
        Filter(StaffEvent.Status status) { this.status = status; }
    }

    private final List<StaffEvent> events = StaffEventSamples.events();
    private Filter filter = Filter.ALL;
    private boolean loaded;

    public Filter getFilter() { return filter; }
    public void setFilter(Filter filter) { this.filter = filter; }
    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }

    public List<StaffEvent> visibleEvents() {
        // O filtro atua somente nos exemplos locais; Todos não restringe o estado.
        List<StaffEvent> result = new ArrayList<>();
        for (StaffEvent event : events) {
            if (filter == Filter.ALL || event.status == filter.status) result.add(event);
        }
        return result;
    }
}
