package com.example.astro_mobile.manager.forms.answer;

import android.content.Context;
import android.location.Location;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.FormResponseRepository;
import com.example.astro_mobile.data.api.dto.FormResponseRequest;
import com.example.astro_mobile.data.api.dto.ReceivedFormDetails;
import com.example.astro_mobile.data.camera.FormEvidenceRepository;
import com.example.astro_mobile.data.location.FormLocationRepository;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;

/** Mantém as respostas da tela e aplica a validação de localização definida pelo formulário. */
public final class FormAnswerViewModel extends ViewModel {
    public static final float ALLOWED_RADIUS_METERS = 100f;

    private final FormLocationRepository locations;
    private final FormResponseRepository repository;
    private final FormEvidenceRepository evidence;
    private final Map<Long, FormResponseRequest.Answer> answers = new LinkedHashMap<>();
    private final Map<Long, List<Uri>> photos = new LinkedHashMap<>();
    private Long pendingPhotoQuestion;
    private Uri pendingPhoto;
    private ReceivedFormDetails details;
    private Location reference;
    private Location lastLocation;
    private boolean requiresUnitLocation;
    private boolean locationBusy;
    private boolean loading;
    private boolean uncertain;
    private FormLocationRepository.Failure failure;

    public FormAnswerViewModel(FormLocationRepository locations, FormResponseRepository repository,
                               FormEvidenceRepository evidence) {
        this.locations = locations;
        this.repository = repository;
        this.evidence = evidence;
    }

    public ReceivedFormDetails getDetails() { return details; }
    public boolean isBusy() { return locationBusy; }
    public boolean isLoading() { return loading; }
    public boolean needsConfirmation() { return uncertain; }
    public boolean isCompleted() { return details != null && "CONCLUIDO".equals(details.form.status); }
    public boolean requiresLocation() { return requiresUnitLocation; }
    public FormLocationRepository.Failure getFailure() { return failure; }
    public boolean isOutside() {
        return requiresUnitLocation && reference != null && lastLocation != null
                && reference.distanceTo(lastLocation) > ALLOWED_RADIUS_METERS;
    }

    public void initialize(ReceivedFormDetails data) {
        boolean sameForm = details != null && details.form.id.equals(data.form.id);
        details = data;
        ReceivedFormDetails.UnitLocation unit = data.form.unitLocation;
        setUnitReference(data.form.unitId != null, unit != null && unit.isValid() ? unit.latitude : null,
                unit != null && unit.isValid() ? unit.longitude : null);
        if (!sameForm || isCompleted()) {
            answers.clear();
            clearPhotos();
        }
        if (data.form.questions == null) return;

        for (ReceivedFormDetails.Question question : data.form.questions) {
            if (answers.containsKey(question.id)) continue;
            FormResponseRequest.Answer answer = new FormResponseRequest.Answer(question.id);
            answer.optionIds = new ArrayList<>();
            if (isCompleted() && question.answer != null) readSubmittedAnswer(question, answer);
            answers.put(question.id, answer);
        }
    }

    private void readSubmittedAnswer(ReceivedFormDetails.Question question, FormResponseRequest.Answer answer) {
        for (JsonElement value : question.answer) {
            if (value == null || value.isJsonNull()) continue;
            if ("alternativa".equals(question.type)) {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    answer.optionIds.add(value.getAsLong());
                } else if (value.isJsonObject() && value.getAsJsonObject().has("otherText")) {
                    answer.otherText = value.getAsJsonObject().get("otherText").getAsString();
                }
            } else if (value.isJsonPrimitive()) {
                if ("texto".equals(question.type)) answer.text = value.getAsString();
                if ("data".equals(question.type)) answer.date = value.getAsString();
            }
        }
    }

    public FormResponseRequest.Answer answer(Long id) { return answers.get(id); }

    public void select(ReceivedFormDetails.Question question, Long option, boolean selected) {
        List<Long> selectedIds = answer(question.id).optionIds;
        if (!Boolean.TRUE.equals(question.multiple)) selectedIds.clear();
        if (selected && !selectedIds.contains(option)) selectedIds.add(option);
        else if (!selected) selectedIds.remove(option);
    }

    public boolean selectedOther(ReceivedFormDetails.Question question) {
        if (question.options == null) return false;
        for (ReceivedFormDetails.Option option : question.options) {
            if (option.other && answer(question.id).optionIds.contains(option.id)) return true;
        }
        return false;
    }

    public List<Uri> photos(Long questionId) {
        List<Uri> values = photos.get(questionId);
        return values == null ? java.util.Collections.emptyList() : new ArrayList<>(values);
    }

    private int photoCount(Long questionId) {
        List<Uri> values = photos.get(questionId);
        return values == null ? 0 : values.size();
    }

    public boolean canAddPhoto(ReceivedFormDetails.Question question) {
        int limit = question.maxFiles == null ? 1 : Math.min(10, Math.max(1, question.maxFiles));
        return !isCompleted() && photoCount(question.id) < limit;
    }

    public void beginPhotoCapture(Long questionId, Uri photo) {
        pendingPhotoQuestion = questionId;
        pendingPhoto = photo;
    }

    public Uri pendingPhoto() { return pendingPhoto; }

    public boolean finishPhotoCapture(boolean captured) {
        Uri photo = pendingPhoto;
        Long questionId = pendingPhotoQuestion;
        pendingPhoto = null;
        pendingPhotoQuestion = null;
        if (photo == null) return false;
        ReceivedFormDetails.Question question = findQuestion(questionId);
        if (!captured || question == null || !canAddPhoto(question)
                || !evidence.isWithinImageLimit(photo,
                        question.maxFileSizeMb == null ? 10 : question.maxFileSizeMb)
                || exceedsRequestLimit(photo)) {
            evidence.remove(photo);
            return false;
        }
        photos.computeIfAbsent(questionId, ignored -> new ArrayList<>()).add(photo);
        return true;
    }

    private boolean exceedsRequestLimit(Uri additional) {
        try {
            long total = evidence.sizeBytes(additional);
            for (List<Uri> values : photos.values()) {
                for (Uri existing : values) total += evidence.sizeBytes(existing);
            }
            // Deixa margem para o JSON e os cabeçalhos do multipart dentro do limite do servidor.
            return total > 150L * 1024L * 1024L;
        } catch (IOException error) {
            return true;
        }
    }

    public void removePhoto(Long questionId, Uri photo) {
        List<Uri> values = photos.get(questionId);
        if (values != null && values.remove(photo)) evidence.remove(photo);
        if (values != null && values.isEmpty()) photos.remove(questionId);
    }

    private ReceivedFormDetails.Question findQuestion(Long id) {
        if (id == null || details == null || details.form.questions == null) return null;
        for (ReceivedFormDetails.Question question : details.form.questions) {
            if (question.id.equals(id)) return question;
        }
        return null;
    }

    private List<File> orderedPhotoFiles() throws IOException {
        List<File> files = new ArrayList<>();
        for (ReceivedFormDetails.Question question : details.form.questions) {
            for (Uri photo : photos(question.id)) files.add(evidence.file(photo));
        }
        return files;
    }

    private void clearPhotos() {
        for (List<Uri> values : photos.values()) {
            for (Uri photo : values) evidence.remove(photo);
        }
        photos.clear();
        if (pendingPhoto != null) evidence.remove(pendingPhoto);
        pendingPhoto = null;
        pendingPhotoQuestion = null;
    }

    public boolean canSubmit() {
        if (details == null || details.form.questions == null || details.form.questions.isEmpty()
                || isCompleted() || loading || uncertain || !canStart()) return false;
        for (ReceivedFormDetails.Question question : details.form.questions) {
            FormResponseRequest.Answer answer = answer(question.id);
            switch (question.type) {
                case "texto":
                    if (question.required && blank(answer.text)) return false;
                    break;
                case "data":
                    if (question.required && blank(answer.date)) return false;
                    break;
                case "alternativa":
                    if (question.required && answer.optionIds.isEmpty()) return false;
                    if (selectedOther(question) && blank(answer.otherText)) return false;
                    break;
                // Upload de fotos pertence à SCRUM-276.
                case "foto":
                    if (question.required && photoCount(question.id) == 0) return false;
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }

    public void setUnitReference(boolean required, Double latitude, Double longitude) {
        requiresUnitLocation = required;
        boolean same = reference != null && latitude != null && longitude != null
                && reference.getLatitude() == latitude && reference.getLongitude() == longitude;
        if (same) return;
        reference = null;
        lastLocation = null;
        failure = null;
        if (required && latitude != null && longitude != null) {
            reference = new Location("unit");
            reference.setLatitude(latitude);
            reference.setLongitude(longitude);
        }
    }

    public boolean canStart() {
        if (isCompleted() || locationBusy) return false;
        return !requiresUnitLocation || reference != null && lastLocation != null && failure == null && !isOutside();
    }

    public void requestLocation(Runnable onChanged) {
        if (locationBusy || isCompleted()) return;
        if (!requiresUnitLocation) { onChanged.run(); return; }
        if (reference == null) {
            failure = FormLocationRepository.Failure.UNAVAILABLE;
            onChanged.run();
            return;
        }
        locationBusy = true;
        locations.requestCurrent(new FormLocationRepository.Callback() {
            @Override public void onLocation(Location location) {
                locationBusy = false;
                failure = null;
                lastLocation = new Location(location);
                onChanged.run();
            }
            @Override public void onFailure(FormLocationRepository.Failure reason) {
                locationBusy = false;
                failure = reason;
                onChanged.run();
            }
        });
    }

    public void refresh(FormResponseRepository.ResultCallback callback) {
        loading = true;
        FormResponseRepository.ResultCallback result = new FormResponseRepository.ResultCallback() {
            @Override public void onSuccess(ReceivedFormDetails data) {
                loading = false;
                uncertain = false;
                initialize(data);
                callback.onSuccess(data);
            }
            @Override public void onFailure(FailureKind kind, String message, boolean isUncertain) {
                loading = false;
                uncertain = isUncertain;
                callback.onFailure(kind, message, isUncertain);
            }
            @Override public void onNotSubmitted() {
                loading = false;
                uncertain = false;
                callback.onNotSubmitted();
            }
        };
        if (uncertain) repository.confirmSubmission(details.form.id, result);
        else repository.load(details.form.id, isCompleted(), result);
    }

    public void submit(FormResponseRepository.ResultCallback callback) {
        if (!canSubmit()) return;
        List<FormResponseRequest.Answer> payload = new ArrayList<>();
        for (ReceivedFormDetails.Question question : details.form.questions) {
            FormResponseRequest.Answer value = answer(question.id);
            FormResponseRequest.Answer item = new FormResponseRequest.Answer(question.id);
            boolean answered = false;
            if ("texto".equals(question.type) && !blank(value.text)) {
                item.text = value.text.trim();
                answered = true;
            } else if ("data".equals(question.type) && !blank(value.date)) {
                item.date = value.date;
                answered = true;
            } else if ("alternativa".equals(question.type) && !value.optionIds.isEmpty()) {
                item.optionIds = new ArrayList<>(value.optionIds);
                if (selectedOther(question)) item.otherText = value.otherText.trim();
                answered = true;
            } else if ("foto".equals(question.type) && photoCount(question.id) > 0) {
                item.fileIndexes = new ArrayList<>();
                int startIndex = 0;
                for (ReceivedFormDetails.Question prior : details.form.questions) {
                    if (prior.id.equals(question.id)) break;
                    startIndex += photoCount(prior.id);
                }
                for (int index = 0; index < photoCount(question.id); index++) {
                    item.fileIndexes.add(startIndex + index);
                }
                answered = true;
            }
            if (answered) payload.add(item);
        }

        List<File> files;
        try {
            files = orderedPhotoFiles();
        } catch (IOException error) {
            callback.onFailure(FailureKind.INTERNAL, null, false);
            return;
        }

        loading = true;
        uncertain = true;
        repository.submit(details.form.id, new FormResponseRequest(payload), files,
                new FormResponseRepository.ResultCallback() {
            @Override public void onSuccess(ReceivedFormDetails data) {
                loading = false;
                uncertain = false;
                initialize(data);
                locations.cancel();
                callback.onSuccess(data);
            }
            @Override public void onFailure(FailureKind kind, String message, boolean isUncertain) {
                loading = false;
                uncertain = isUncertain;
                callback.onFailure(kind, message, isUncertain);
            }
        });
    }

    public void getImageUrl(String imageId, FormResponseRepository.ImageUrlCallback callback) {
        repository.getImageUrl(details.form.id, imageId, callback);
    }

    public void pauseLocation() { locations.cancel(); locationBusy = false; }
    public void cancelRequest() { repository.cancel(); loading = false; }
    @Override protected void onCleared() { pauseLocation(); cancelRequest(); clearPhotos(); }

    public static ViewModelProvider.Factory factory(Context context) {
        FormLocationRepository locations = new FormLocationRepository(context);
        return new ViewModelProvider.Factory() {
            @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return modelClass.cast(new FormAnswerViewModel(locations, FormResponseRepository.createDefault(),
                        new FormEvidenceRepository(context)));
            }
        };
    }
}
