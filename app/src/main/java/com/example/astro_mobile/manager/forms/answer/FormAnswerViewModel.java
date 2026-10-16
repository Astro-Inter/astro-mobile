package com.example.astro_mobile.manager.forms.answer;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.location.FormLocationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FormAnswerViewModel extends ViewModel {
    public static final int MAX_EVIDENCES = 3;
    public static final float ALLOWED_RADIUS_METERS = 100f;

    private final FormLocationRepository locations;
    private final int[] choices = {-1, -1, -1};
    private final List<Location> readings = new ArrayList<>();
    private String risks = "";
    private String date = "";
    private int evidences;
    private boolean loaded;
    private boolean busy;
    private boolean cancelled;
    private boolean completed;
    private float distance;
    private Location reference;
    private FormLocationRepository.Failure failure;

    public FormAnswerViewModel(FormLocationRepository locations) { this.locations = locations; }

    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }
    public int choice(int question) { return choices[question]; }
    public void setChoice(int question, int answer) { choices[question] = answer; }
    public String getRisks() { return risks; }
    public void setRisks(String risks) { this.risks = risks; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public int evidenceCount() { return evidences; }
    public void addEvidence() { if (evidences < MAX_EVIDENCES) evidences++; }
    public void removeEvidence() { if (evidences > 0) evidences--; }
    public boolean isBusy() { return busy; }
    public boolean isCancelled() { return cancelled; }
    public boolean isCompleted() { return completed; }
    public float getDistance() { return distance; }
    public FormLocationRepository.Failure getFailure() { return failure; }
    public int readingCount() { return readings.size(); }

    // As posições ficam somente na memória desta sessão, sem logs, upload ou persistência oculta.
    public List<Location> getReadings() {
        List<Location> copy = new ArrayList<>();
        for (Location reading : readings) copy.add(new Location(reading));
        return Collections.unmodifiableList(copy);
    }

    public boolean canSubmit() {
        return choices[0] != -1 && choices[1] != -1 && choices[2] != -1
                && !risks.trim().isEmpty() && !date.isEmpty() && evidences > 0
                && !readings.isEmpty() && failure == null && !busy && !cancelled && !completed;
    }

    public void requestLocation(boolean finish, boolean requiresUnitLocation, Runnable onChanged) {
        if (busy || cancelled || completed) return;
        busy = true;
        locations.requestCurrent(new FormLocationRepository.Callback() {
            @Override public void onLocation(Location location) {
                busy = false;
                failure = null;
                // Até a API, a primeira posição é somente uma referência de teste, não a unidade.
                if (reference == null) reference = new Location(location);
                readings.add(new Location(location));
                distance = reference.distanceTo(location);
                cancelled = requiresUnitLocation && distance > ALLOWED_RADIUS_METERS;
                if (finish && canSubmit()) completed = true;
                onChanged.run();
            }

            @Override public void onFailure(FormLocationRepository.Failure reason) {
                busy = false;
                failure = reason;
                onChanged.run();
            }
        });
    }

    public void pauseLocation() {
        // Mantém as respostas/referência, mas interrompe a leitura ao ir para segundo plano.
        locations.cancel();
        busy = false;
    }

    @Override protected void onCleared() { locations.cancel(); }

    public static ViewModelProvider.Factory factory(Context context) {
        FormLocationRepository repository = new FormLocationRepository(context);
        return new ViewModelProvider.Factory() {
            @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return modelClass.cast(new FormAnswerViewModel(repository));
            }
        };
    }
}
