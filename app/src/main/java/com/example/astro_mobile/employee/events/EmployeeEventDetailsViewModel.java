package com.example.astro_mobile.employee.events;

import android.content.ContentResolver;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.CollaboratorEventDetailsRepository;
import com.example.astro_mobile.data.api.EventEvidenceFile;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.CollaboratorEventData;
import com.example.astro_mobile.data.api.dto.CollaboratorEventDetailsData;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EmployeeEventDetailsViewModel extends ViewModel {
    public interface ResultCallback {
        void onSuccess();
        void onFailure(FailureKind failure, String message);
    }
    private final CollaboratorEventDetailsRepository repository;
    private CollaboratorEventDetailsData details;
    private final List<EventEvidenceFile> files = new ArrayList<>();
    private boolean loading;

    public EmployeeEventDetailsViewModel(CollaboratorEventDetailsRepository repository) { this.repository = repository; }
    public CollaboratorEventDetailsData getDetails() { return details; }
    public List<EventEvidenceFile> getFiles() { return Collections.unmodifiableList(files); }
    public boolean isLoading() { return loading; }
    public void removeFile(EventEvidenceFile file) { files.remove(file); }
    public void addFile(EventEvidenceFile file) {
        if (files.size() < 3 && files.stream().noneMatch(item -> item.getUri().equals(file.getUri()))) files.add(file);
    }

    public void load(long eventId, ResultCallback callback) {
        loading = true;
        repository.load(eventId, new CollaboratorEventDetailsRepository.ResultCallback<CollaboratorEventDetailsData>() {
            @Override public void onSuccess(CollaboratorEventDetailsData data) {
                details = data;
                loading = false;
                callback.onSuccess();
            }
            @Override public void onFailure(FailureKind failure, String message) {
                loading = false;
                callback.onFailure(failure, message);
            }
        });
    }

    public EventStatusClassifier.DisplayStatus getDisplayStatus() {
        EventStatusClassifier.Participation participation;
        switch (details.getParticipationStatus()) {
            case EM_ANALISE: participation = EventStatusClassifier.Participation.AWAITING_APPROVAL; break;
            case CONCLUIDO: participation = EventStatusClassifier.Participation.APPROVED; break;
            case REJEITADO: participation = EventStatusClassifier.Participation.REJECTED; break;
            default: participation = EventStatusClassifier.Participation.NOT_SUBMITTED;
        }
        // A tela aberta usa a situação atual da participação, sem a etiqueta de não visualizado.
        return EventStatusClassifier.classify(details.getEventStatus() == CollaboratorEventData.EventStatus.CANCELADO,
                participation, true, details.getDeadline().atZone(ZoneId.systemDefault()).toInstant(), Instant.now());
    }

    public boolean canComplete() {
        if (details == null || details.getCompletionMode() != CollaboratorEventDetailsData.CompletionMode.COLABORADOR
                || details.getEventStatus() != CollaboratorEventData.EventStatus.ATIVO
                || !details.getDeadline().isAfter(java.time.LocalDateTime.now())) return false;
        return details.getParticipationStatus() == CollaboratorEventData.ParticipationStatus.PENDENTE
                || details.getParticipationStatus() == CollaboratorEventData.ParticipationStatus.REJEITADO;
    }

    public void complete(ContentResolver resolver, ResultCallback callback) {
        if (loading || !canComplete()) return;
        loading = true;
        repository.complete(details.getEventId(), new ArrayList<>(files), resolver,
                new CollaboratorEventDetailsRepository.ResultCallback<Object>() {
                    @Override public void onSuccess(Object result) {
                        loading = false;
                        files.clear();
                        callback.onSuccess();
                    }
                    @Override public void onFailure(FailureKind failure, String message) {
                        loading = false;
                        callback.onFailure(failure, message);
                    }
                });
    }

    public void cancel() { repository.cancel(); loading = false; }
    @Override protected void onCleared() { cancel(); }
    public static final class Factory implements ViewModelProvider.Factory {
        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return modelClass.cast(new EmployeeEventDetailsViewModel(CollaboratorEventDetailsRepository.createDefault()));
        }
    }
}
