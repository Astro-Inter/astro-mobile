package com.example.astro_mobile.manager.staff;

import androidx.lifecycle.ViewModel;
import com.example.astro_mobile.data.api.CollaboratorProfileRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.CollaboratorProfileData;

/** Mantém o perfil carregado enquanto o gestor navega nas telas internas. */
public final class StaffDetailsViewModel extends ViewModel {
    private final CollaboratorProfileRepository repository = CollaboratorProfileRepository.createDefault();
    private CollaboratorProfileData profile;
    private boolean loading;
    public interface Callback { void onSuccess(); void onFailure(FailureKind failure); }
    public CollaboratorProfileData getProfile() { return profile; }
    public void load(long id, Callback callback) {
        if (loading) return;
        loading = true;
        repository.load(id, new CollaboratorProfileRepository.ResultCallback() {
            @Override public void onSuccess(CollaboratorProfileData data) { loading = false; profile = data; callback.onSuccess(); }
            @Override public void onFailure(FailureKind failure) { loading = false; callback.onFailure(failure); }
        });
    }
    public void cancel() { repository.cancel(); loading = false; }
    @Override protected void onCleared() { cancel(); }
}
