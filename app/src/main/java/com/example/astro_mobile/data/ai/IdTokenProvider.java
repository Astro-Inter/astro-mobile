package com.example.astro_mobile.data.ai;

/** Obtém o ID token da sessão existente, sem chamar o login da API de IA. */
public interface IdTokenProvider {
    enum Failure { SESSION, CONNECTION, INTERNAL }

    interface Callback {
        void onToken(String token);
        void onFailure(Failure failure);
    }

    void getToken(boolean forceRefresh, Callback callback);
}
