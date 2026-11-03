package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ManagerEventData;

/** Consulta o status atual pelo contrato de eventos do gestor. */
public final class ManagerEventStatusRepository {
    public interface Callback {
        void onSuccess(String status);
        void onFailure(FailureKind failure);
    }

    private final ManagerEventsRepository events = ManagerEventsRepository.createDefault();

    public void load(long eventId, Callback callback) {
        events.load(new ManagerEventsRepository.ResultCallback() {
            @Override public void onSuccess(java.util.List<ManagerEventData> items) {
                for (ManagerEventData item : items) {
                    if (item != null && item.id != null && item.id == eventId) {
                        if ("ATIVO".equals(item.status) || "CANCELADO".equals(item.status)
                                || "CONCLUIDO".equals(item.status)) {
                            callback.onSuccess(item.status);
                        } else callback.onFailure(FailureKind.INTERNAL);
                        return;
                    }
                }
                callback.onFailure(FailureKind.INTERNAL);
            }
            @Override public void onFailure(FailureKind failure) { callback.onFailure(failure); }
        });
    }

    public void cancel() { events.cancel(); }
}
