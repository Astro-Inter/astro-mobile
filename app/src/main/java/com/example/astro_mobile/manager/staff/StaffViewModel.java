package com.example.astro_mobile.manager.staff;

import androidx.lifecycle.ViewModel;

import com.example.astro_mobile.manager.staff.mock.StaffSamples;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StaffViewModel extends ViewModel {
    public enum Filter { ALL, PENDING, NO_PENDING }

    private final List<StaffMember> members = StaffSamples.members();
    private Filter filter = Filter.ALL;
    private String query = "";
    private boolean loaded;

    public Filter getFilter() { return filter; }
    public void setFilter(Filter filter) { this.filter = filter; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }

    public List<StaffMember> visibleMembers() {
        // Combina a pesquisa de nome/cargo com o filtro de pendências apenas nos mocks.
        List<StaffMember> result = new ArrayList<>();
        String search = normalize(query.trim());
        for (StaffMember member : members) {
            if (filter == Filter.PENDING && member.pendingCount == 0) continue;
            if (filter == Filter.NO_PENDING && member.pendingCount > 0) continue;
            if (normalize(member.name + " " + member.role).contains(search)) result.add(member);
        }
        return result;
    }

    private String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
