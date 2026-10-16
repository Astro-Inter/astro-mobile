package com.example.astro_mobile.manager.forms;

import androidx.lifecycle.ViewModel;

import com.example.astro_mobile.manager.forms.mock.ManagerFormSamples;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ManagerFormsViewModel extends ViewModel {
    public enum Filter { ALL, IN_PROGRESS, FINISHED }

    private final List<ManagerForm> forms = ManagerFormSamples.all();
    private Filter filter = Filter.ALL;
    private String query = "";
    private String selectedNr = "";
    private boolean loaded;

    public Filter getFilter() { return filter; }
    public void setFilter(Filter filter) { this.filter = filter; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getSelectedNr() { return selectedNr; }
    public void setSelectedNr(String selectedNr) { this.selectedNr = selectedNr; }
    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }

    public List<ManagerForm> visibleForms() {
        // Pesquisa, NR e estado são aplicados apenas sobre os exemplos locais.
        List<ManagerForm> visible = new ArrayList<>();
        String search = normalize(query.trim());
        for (ManagerForm form : forms) {
            boolean finished = form.status == ManagerForm.Status.FINISHED;
            if (filter == Filter.IN_PROGRESS && finished) continue;
            if (filter == Filter.FINISHED && !finished) continue;
            if (!selectedNr.isEmpty() && !selectedNr.equals(form.nr)) continue;
            if (!normalize(form.title).contains(search) && !normalize(form.nr).contains(search)) continue;
            visible.add(form);
        }
        return visible;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
