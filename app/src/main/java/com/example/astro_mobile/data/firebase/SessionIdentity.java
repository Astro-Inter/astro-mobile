package com.example.astro_mobile.data.firebase;

/** Identidade da sessão e aviso de troca de conta, sem expor o SDK à apresentação. */
public interface SessionIdentity {
    String currentUserId();
    Runnable observe(Runnable onChanged);
}
