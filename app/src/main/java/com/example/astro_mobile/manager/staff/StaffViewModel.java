package com.example.astro_mobile.manager.staff;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.CollaboratorsRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.CollaboratorSummaryData;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class StaffViewModel extends ViewModel {
    public interface ResultCallback {
        void onSuccess();
        void onFailure(FailureKind failure);
    }

    private final CollaboratorsRepository repository;
    private List<StaffMember> members = Collections.emptyList();
    private String query = "";
    private int filter;
    private boolean loaded;
    private boolean loading;
    private int requestId;

    public StaffViewModel(CollaboratorsRepository repository) {
        this.repository = repository;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query == null ? "" : query; }
    public boolean isLoaded() { return loaded; }
    public boolean isLoading() { return loading; }
    public boolean hasMembers() { return !members.isEmpty(); }
    public int getFilter() { return filter; }
    public void setFilter(int filter) { this.filter = filter; }

    public void load(ResultCallback callback) {
        if (loading) return;
        loading = true;
        int id = ++requestId;
        repository.load(new CollaboratorsRepository.ResultCallback() {
            @Override public void onSuccess(List<CollaboratorSummaryData> collaborators) {
                if (id != requestId) return;
                loading = false;
                ArrayList<StaffMember> result = new ArrayList<>(collaborators.size());
                for (CollaboratorSummaryData collaborator : collaborators) {
                    result.add(new StaffMember(collaborator));
                }
                members = result;
                loaded = true;
                callback.onSuccess();
            }

            @Override public void onFailure(FailureKind failure) {
                if (id != requestId) return;
                loading = false;
                callback.onFailure(failure);
            }
        });
    }

    public List<StaffMember> visibleMembers() {
        ArrayList<StaffMember> result = new ArrayList<>();
        String search = normalize(query.trim());
        for (StaffMember member : members) {
            String searchableText = member.name + " " + member.role;
            boolean matchesFilter = filter == 0 || (filter == 1 && member.pendingCount > 0)
                    || (filter == 2 && member.pendingCount == 0);
            if (matchesFilter && normalize(searchableText).contains(search)) result.add(member);
        }
        return result;
    }

    private String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public void cancel() {
        ++requestId;
        loading = false;
        repository.cancel();
    }

    @Override protected void onCleared() { cancel(); }

    public static final class Factory implements ViewModelProvider.Factory {
        private final CollaboratorsRepository repository;

        public Factory(CollaboratorsRepository repository) { this.repository = repository; }

        public static Factory createDefault() {
            return new Factory(CollaboratorsRepository.createDefault());
        }

        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(StaffViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
            }
            return modelClass.cast(new StaffViewModel(repository));
        }
    }
}
