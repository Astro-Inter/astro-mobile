package com.example.astro_mobile.manager.events;

import androidx.lifecycle.ViewModel;
import com.example.astro_mobile.data.api.ManagerEventDetailsRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.ManagerEventDetailsData;

/** Guarda o detalhe carregado ao navegar para os participantes. */
public final class ManagerEventDetailsViewModel extends ViewModel {
    private final ManagerEventDetailsRepository repository = ManagerEventDetailsRepository.createDefault();
    private ManagerEventDetailsData details;
    private boolean loading;
    public interface Callback { void onSuccess(); void onFailure(FailureKind failure); }
    public ManagerEventDetailsData getDetails() { return details; }
    public void load(long id, boolean selfDeclaration, Callback callback) {
        if (loading) return;
        loading = true;
        repository.load(id, selfDeclaration, new ManagerEventDetailsRepository.ResultCallback() {
            @Override public void onSuccess(ManagerEventDetailsData data) { loading = false; details = data; callback.onSuccess(); }
            @Override public void onFailure(FailureKind failure) { loading = false; callback.onFailure(failure); }
        });
    }
    public void cancel() { repository.cancel(); loading = false; }
    @Override protected void onCleared() { cancel(); }
}
