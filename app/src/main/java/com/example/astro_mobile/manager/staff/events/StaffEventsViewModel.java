package com.example.astro_mobile.manager.staff.events;

import androidx.lifecycle.ViewModel;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.StaffEventsRepository;
import com.example.astro_mobile.data.api.dto.CollaboratorStaffEventData;
import com.example.astro_mobile.data.api.dto.CollaboratorStaffEventsData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.Instant;
import java.time.ZoneId;

public final class StaffEventsViewModel extends ViewModel {
    public enum Filter {
        ALL(null), PENDING(StaffEvent.Status.PENDING), DONE(StaffEvent.Status.DONE), EXPIRED(StaffEvent.Status.EXPIRED);

        final StaffEvent.Status status;
        Filter(StaffEvent.Status status) { this.status = status; }
    }

    public interface ResultCallback {
        void onSuccess();
        void onFailure(FailureKind failure);
    }

    private final StaffEventsRepository repository;
    private List<StaffEvent> events = Collections.emptyList();
    private Filter filter = Filter.ALL;
    private boolean loaded;
    private boolean loading;
    private String collaboratorName;

    public StaffEventsViewModel(StaffEventsRepository repository) { this.repository = repository; }
    public Filter getFilter() { return filter; }
    public void setFilter(Filter filter) { this.filter = filter; }
    public boolean isLoaded() { return loaded; }
    public String getCollaboratorName() { return collaboratorName; }

    public void load(long collaboratorId, ResultCallback callback) {
        if (loading) return;
        loading = true;
        repository.load(collaboratorId, new StaffEventsRepository.ResultCallback() {
            @Override public void onSuccess(CollaboratorStaffEventsData data) {
                List<StaffEvent> result = new ArrayList<>();
                ZoneId zone = ZoneId.systemDefault();
                Instant now = Instant.now();
                for (CollaboratorStaffEventData item : data.events) {
                    StaffEvent event = StaffEventMapper.toStaffEvent(item, zone, now);
                    if (event == null) {
                        loading = false;
                        callback.onFailure(FailureKind.INTERNAL);
                        return;
                    }
                    result.add(event);
                }
                // Só troca a lista após validar o retorno completo.
                events = result;
                collaboratorName = data.collaboratorName;
                loaded = true;
                loading = false;
                callback.onSuccess();
            }

            @Override public void onFailure(FailureKind failure) {
                loading = false;
                callback.onFailure(failure);
            }
        });
    }

    public List<StaffEvent> visibleEvents() {
        // Aplica o filtro selecionado aos eventos carregados.
        List<StaffEvent> result = new ArrayList<>();
        for (StaffEvent event : events) {
            if (filter == Filter.ALL || event.status == filter.status) result.add(event);
        }
        return result;
    }

    public void cancel() {
        repository.cancel();
        loading = false;
    }

    @Override protected void onCleared() { cancel(); }

    public static final class Factory implements ViewModelProvider.Factory {
        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(StaffEventsViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new StaffEventsViewModel(StaffEventsRepository.createDefault()));
        }
    }
}
