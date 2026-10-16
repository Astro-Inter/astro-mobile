package com.example.astro_mobile.manager.forms.answer;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.camera.FormEvidenceRepository;
import com.example.astro_mobile.data.location.FormLocationRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FormAnswerViewModel extends ViewModel {
    public static final int MAX_EVIDENCES = 3;
    public static final float ALLOWED_RADIUS_METERS = 100f;

    private final FormLocationRepository locations;
    private final FormEvidenceRepository photos;
    private final int[] choices = {-1, -1, -1};
    private final List<Location> readings = new ArrayList<>();
    private String risks = "";
    private String date = "";
    private final List<Uri> evidences = new ArrayList<>();
    private Uri pendingPhoto;
    private boolean loaded;
    private boolean busy;
    private boolean outside;
    private boolean completed;
    private Location reference;
    private FormLocationRepository.Failure failure;

    public FormAnswerViewModel(FormLocationRepository locations, FormEvidenceRepository photos) {
        this.locations = locations;
        this.photos = photos;
    }

    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }
    public int choice(int question) { return choices[question]; }
    public void setChoice(int question, int answer) { choices[question] = answer; }
    public String getRisks() { return risks; }
    public void setRisks(String risks) { this.risks = risks; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public int evidenceCount() { return evidences.size(); }
    public Uri evidenceAt(int index) { return evidences.get(index); }
    public Bitmap evidencePreview(Uri photo) throws IOException { return photos.preview(photo); }

    public Uri preparePhoto() throws IOException {
        if (evidences.size() == MAX_EVIDENCES || pendingPhoto != null) return null;
        pendingPhoto = photos.createCapture();
        return pendingPhoto;
    }

    public void onPhotoResult(boolean captured) {
        if (pendingPhoto == null) return;
        if (captured) evidences.add(pendingPhoto);
        else photos.remove(pendingPhoto);
        pendingPhoto = null;
    }

    public void removeEvidence(Uri photo) {
        if (evidences.remove(photo)) photos.remove(photo);
    }
    public boolean isBusy() { return busy; }
    public boolean isOutside() { return outside; }
    public boolean isCompleted() { return completed; }
    public FormLocationRepository.Failure getFailure() { return failure; }

    // As posições ficam somente na memória desta sessão, sem logs, upload ou persistência oculta.
    public List<Location> getReadings() {
        List<Location> copy = new ArrayList<>();
        for (Location reading : readings) copy.add(new Location(reading));
        return Collections.unmodifiableList(copy);
    }

    public boolean canSubmit() {
        return choices[0] != -1 && choices[1] != -1 && choices[2] != -1
                && !risks.trim().isEmpty() && !date.isEmpty() && !evidences.isEmpty()
                && canStart();
    }

    public boolean canStart() {
        return !readings.isEmpty() && failure == null && !busy && !outside && !completed;
    }

    public void requestLocation(boolean finish, boolean requiresUnitLocation, Runnable onChanged) {
        if (busy || completed) return;
        busy = true;
        locations.requestCurrent(new FormLocationRepository.Callback() {
            @Override public void onLocation(Location location) {
                busy = false;
                failure = null;
                // Mock autorizado: fixa a posição ao abrir os detalhes; a API fornecerá a unidade depois.
                if (reference == null) reference = new Location(location);
                readings.add(new Location(location));
                outside = requiresUnitLocation && reference.distanceTo(location) > ALLOWED_RADIUS_METERS;
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

    @Override protected void onCleared() {
        locations.cancel();
        // As fotos são temporárias: sair deste formulário descarta seus arquivos locais.
        for (Uri photo : evidences) photos.remove(photo);
        if (pendingPhoto != null) photos.remove(pendingPhoto);
    }

    public static ViewModelProvider.Factory factory(Context context) {
        FormLocationRepository repository = new FormLocationRepository(context);
        FormEvidenceRepository photos = new FormEvidenceRepository(context);
        return new ViewModelProvider.Factory() {
            @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return modelClass.cast(new FormAnswerViewModel(repository, photos));
            }
        };
    }
}
