package com.example.astro_mobile.manager.forms;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.ReceivedFormsRepository;
import com.example.astro_mobile.data.api.dto.ReceivedFormsPage;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ManagerFormsViewModel extends ViewModel {
    public enum Filter { ALL, IN_PROGRESS, FINISHED }
    public interface ResultCallback {
        void onSuccess();
        void onFailure(FailureKind failure);
    }
    private final ReceivedFormsRepository repository;
    private final List<ManagerForm> forms = new ArrayList<>();
    private Filter filter = Filter.ALL;
    private String query = "";
    private boolean loading;

    public ManagerFormsViewModel(ReceivedFormsRepository repository) { this.repository = repository; }
    public Filter getFilter() { return filter; }
    public void setFilter(Filter value) { filter = value; }
    public String getQuery() { return query; }
    public void setQuery(String value) { query = value; }
    public boolean isLoading() { return loading; }

    public void load(ResultCallback callback) {
        loading = true;
        repository.load(new ReceivedFormsRepository.ResultCallback() {
            @Override public void onSuccess(List<ReceivedFormsPage.Item> items) {
                List<ManagerForm> result = new ArrayList<>();
                try {
                    for (ReceivedFormsPage.Item item : items) result.add(toForm(item));
                } catch (IllegalArgumentException | java.time.DateTimeException | NullPointerException error) {
                    loading = false;
                    callback.onFailure(FailureKind.INTERNAL);
                    return;
                }
                forms.clear();
                forms.addAll(result);
                loading = false;
                callback.onSuccess();
            }
            @Override public void onFailure(FailureKind failure) {
                loading = false;
                callback.onFailure(failure);
            }
        });
    }
    private ManagerForm toForm(ReceivedFormsPage.Item item) {
        if (item.id == null || item.id.isEmpty() || item.title == null) throw new IllegalArgumentException();
        boolean finished = "CONCLUIDO".equals(item.status);
        if (!finished && !"PENDENTE".equals(item.status)) throw new IllegalArgumentException();
        ManagerForm.Status status = item.viewed ? ManagerForm.Status.PENDING : ManagerForm.Status.NEW;
        // Responder e aprovar sÃ£o estados diferentes; a reprovaÃ§Ã£o nÃ£o permite novo envio.
        if (finished) {
            if ("approved".equals(item.reviewStatus)) status = ManagerForm.Status.APPROVED;
            else if ("rejected".equals(item.reviewStatus)) status = ManagerForm.Status.REJECTED;
            else if ("awaiting_review".equals(item.reviewStatus)) status = ManagerForm.Status.ANALYSIS;
            else throw new IllegalArgumentException();
        }
        String deadline = item.dueDate == null ? "" : LocalDate.parse(item.dueDate)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return new ManagerForm(item.id, item.title, item.description == null ? "" : item.description,
                deadline, item.nrCode == null ? "" : "NR - " + item.nrCode,
                status, finished, item.unitId != null);
    }
    public List<ManagerForm> visibleForms() {
        List<ManagerForm> visible = new ArrayList<>();
        String search = normalize(query.trim());
        for (ManagerForm form : forms) {
            if (filter == Filter.IN_PROGRESS && form.finished) continue;
            if (filter == Filter.FINISHED && !form.finished) continue;
            if (normalize(form.title + " " + form.description + " " + form.nr).contains(search)) visible.add(form);
        }
        return visible;
    }
    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
    public void cancel() { repository.cancel(); loading = false; }
    @Override protected void onCleared() { cancel(); }
    public static final class Factory implements ViewModelProvider.Factory {
        @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return modelClass.cast(new ManagerFormsViewModel(ReceivedFormsRepository.createDefault()));
        }
    }
}
