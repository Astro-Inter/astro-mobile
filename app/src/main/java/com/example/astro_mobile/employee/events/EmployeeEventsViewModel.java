package com.example.astro_mobile.employee.events;

import androidx.lifecycle.ViewModel;

import com.example.astro_mobile.employee.events.mock.EmployeeEventSamples;

import java.time.LocalDate;
import java.time.YearMonth;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EmployeeEventsViewModel extends ViewModel {
    private YearMonth month = YearMonth.now();
    // A fonte mock pode ser trocada pelo repositório da API quando o contrato chegar.
    private List<EmployeeEvent> events = EmployeeEventSamples.forMonth(month);
    private EmployeeEvent.Status filter;
    private String query = "";
    private boolean loaded;

    public YearMonth getMonth() { return month; }
    public void setMonth(YearMonth month) {
        this.month = month;
        events = EmployeeEventSamples.forMonth(month);
    }
    public EmployeeEvent.Status getFilter() { return filter; }
    public void setFilter(EmployeeEvent.Status filter) { this.filter = filter; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query.trim(); }
    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }

    public List<EmployeeEvent> visibleEvents() {
        // Aplica a busca sem diferenciar acentos, o estado escolhido e o mês atual.
        List<EmployeeEvent> result = new ArrayList<>();
        for (EmployeeEvent event : events) {
            if (YearMonth.from(event.date).equals(month)
                    && (filter == null || event.status == filter)
                    && matches(event.title, query)) result.add(event);
        }
        return result;
    }

    private boolean matches(String title, String query) {
        String normalizedTitle = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        String normalizedQuery = Normalizer.normalize(query, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return normalizedTitle.contains(normalizedQuery);
    }

    public List<EmployeeEvent> eventsOn(LocalDate day) {
        List<EmployeeEvent> result = new ArrayList<>();
        for (EmployeeEvent event : visibleEvents()) {
            if (event.date.equals(day)) result.add(event);
        }
        return result;
    }
}
