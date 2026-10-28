package com.example.astro_mobile.manager.events;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.ManagerEventsRepository;
import com.example.astro_mobile.data.api.dto.ManagerEventData;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ManagerEventsViewModel extends ViewModel {
    public enum Filter { ALL, IN_PROGRESS, FINISHED }
    public interface ResultCallback {
        void onSuccess();
        void onFailure(FailureKind failure);
    }

    private final ManagerEventsRepository repository;
    private final List<ManagerEvent> events = new ArrayList<>();
    private Filter filter = Filter.ALL;
    private String query = "";
    private boolean loaded;
    private boolean loading;

    public ManagerEventsViewModel(ManagerEventsRepository repository) { this.repository = repository; }
    public Filter getFilter() { return filter; }
    public void setFilter(Filter filter) { this.filter = filter; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public boolean isLoaded() { return loaded; }
    public boolean isLoading() { return loading; }

    // Converte os dados reais em estados apresentados na lista.
    public void load(ResultCallback callback) {
        loading = true;
        repository.load(new ManagerEventsRepository.ResultCallback() {
            @Override public void onSuccess(List<ManagerEventData> data) {
                List<ManagerEvent> result = new ArrayList<>();
                for (ManagerEventData item : data) {
                    ManagerEvent event = toEvent(item);
                    if (event == null) {
                        loading = false;
                        callback.onFailure(FailureKind.INTERNAL);
                        return;
                    }
                    result.add(event);
                }
                events.clear();
                events.addAll(result);
                loading = false;
                loaded = true;
                callback.onSuccess();
            }

            @Override public void onFailure(FailureKind failure) {
                loading = false;
                callback.onFailure(failure);
            }
        });
    }

    private ManagerEvent toEvent(ManagerEventData item) {
        if (item == null || item.id == null || item.title == null || item.pendingCount == null
                || item.pendingCount < 0) return null;
        ManagerEvent.CompletionMode mode;
        if ("COLABORADOR".equals(item.completionMode)) mode = ManagerEvent.CompletionMode.SELF_DECLARATION;
        else if ("GESTOR".equals(item.completionMode)) mode = ManagerEvent.CompletionMode.MANAGER_ASSIGNMENT;
        else return null;

        ManagerEvent.Status status;
        if ("CANCELADO".equals(item.status)) status = ManagerEvent.Status.CANCELLED;
        else if ("CONCLUIDO".equals(item.status)) status = ManagerEvent.Status.FINISHED;
        else if (!"ATIVO".equals(item.status)) return null;
        else if (item.pendingCount == 0) status = ManagerEvent.Status.NO_ACTION;
        else status = mode == ManagerEvent.CompletionMode.SELF_DECLARATION
                    ? ManagerEvent.Status.VALIDATION_PENDING : ManagerEvent.Status.ASSIGNMENT_PENDING;
        return new ManagerEvent(item.id, item.title, status, item.pendingCount, mode);
    }

    // Pesquisa e filtros são aplicados aos dados já recebidos.
    public List<ManagerEvent> visibleEvents() {
        List<ManagerEvent> result = new ArrayList<>();
        String search = normalize(query.trim());
        for (ManagerEvent event : events) {
            if (filter == Filter.IN_PROGRESS && (event.status == ManagerEvent.Status.FINISHED
                    || event.status == ManagerEvent.Status.CANCELLED)) continue;
            if (filter == Filter.FINISHED && event.status != ManagerEvent.Status.FINISHED) continue;
            if (normalize(event.title).contains(search)) result.add(event);
        }
        return result;
    }

    private String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public void cancel() { repository.cancel(); loading = false; }
    @Override protected void onCleared() { cancel(); }

    public static final class Factory implements ViewModelProvider.Factory {
        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return modelClass.cast(new ManagerEventsViewModel(ManagerEventsRepository.createDefault()));
        }
    }
}
