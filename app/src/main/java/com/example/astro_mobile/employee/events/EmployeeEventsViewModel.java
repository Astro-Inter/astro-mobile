package com.example.astro_mobile.employee.events;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.CollaboratorEventsRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.CollaboratorEventData;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class EmployeeEventsViewModel extends ViewModel {
    public interface ResultCallback {
        void onSuccess();
        void onFailure(FailureKind failure);
    }

    private final CollaboratorEventsRepository repository;
    private List<CollaboratorEventData> events = Collections.emptyList();
    private YearMonth month = YearMonth.now();
    private EmployeeEvent.Status filter = EmployeeEvent.Status.PENDING;
    private String query = "";
    private boolean loading;
    private ResultCallback pendingCallback;

    public EmployeeEventsViewModel(CollaboratorEventsRepository repository) { this.repository = repository; }

    public YearMonth getMonth() { return month; }
    public void setMonth(YearMonth month) { this.month = month; }
    public EmployeeEvent.Status getFilter() { return filter; }
    public void setFilter(EmployeeEvent.Status filter) { this.filter = filter; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query.trim(); }
    public boolean isLoading() { return loading; }

    public void load(ResultCallback callback) {
        if (loading) return;
        loading = true;
        pendingCallback = callback;
        repository.load(new CollaboratorEventsRepository.ResultCallback() {
            @Override public void onSuccess(List<CollaboratorEventData> result) {
                events = result;
                ResultCallback current = finishRequest();
                if (current != null) current.onSuccess();
            }
            @Override public void onFailure(FailureKind failure) {
                ResultCallback current = finishRequest();
                if (current != null) current.onFailure(failure);
            }
        });
    }

    private ResultCallback finishRequest() {
        loading = false;
        ResultCallback current = pendingCallback;
        pendingCallback = null;
        return current;
    }

    public List<EmployeeEvent> visibleEvents() {
        // Recalcula os prazos ao renderizar; trocar o mês nunca cria dados fictícios.
        List<EmployeeEvent> result = new ArrayList<>();
        Instant now = Instant.now();
        ZoneId zone = ZoneId.systemDefault();
        String search = normalize(query);
        for (CollaboratorEventData item : events) {
            LocalDate date = item.getDeadline().toLocalDate();
            if (!YearMonth.from(date).equals(month) || !normalize(item.getEventTitle()).contains(search)) continue;
            EmployeeEvent event = EmployeeEvent.fromApi(item, zone, now);
            if (filter == null || event.status == filter) result.add(event);
        }
        return result;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public List<EmployeeEvent> eventsOn(LocalDate day) {
        List<EmployeeEvent> result = new ArrayList<>();
        for (EmployeeEvent event : visibleEvents()) if (event.date.equals(day)) result.add(event);
        return result;
    }

    public void cancel() {
        // Solta o callback da View e os dados da sessão anterior, preservando só filtros.
        pendingCallback = null;
        loading = false;
        repository.cancel();
        events = Collections.emptyList();
    }

    @Override protected void onCleared() { cancel(); }

    public static final class Factory implements ViewModelProvider.Factory {
        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(EmployeeEventsViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new EmployeeEventsViewModel(CollaboratorEventsRepository.createDefault()));
        }
    }
}
