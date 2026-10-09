package com.example.astro_mobile.data.firebase;

/** Obtém o ID token da sessão Firebase para as APIs que exigem autenticação. */
public interface IdTokenProvider {
    enum Failure { SESSION, CONNECTION, INTERNAL }

    interface Callback {
        void onToken(String token);
        void onFailure(Failure failure);
    }

    void getToken(boolean forceRefresh, Callback callback);
}
