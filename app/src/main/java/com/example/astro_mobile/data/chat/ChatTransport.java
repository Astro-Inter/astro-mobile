package com.example.astro_mobile.data.chat;

/** Todos os callbacks são entregues na mesma fila da conexão. */
public interface ChatTransport {
    interface Listener {
        void onOpen();
        void onText(String text);
        void onClosed();
        default void onTlsFailure() { onClosed(); }
    }
    interface Socket {
        boolean send(String text);
        void close();
    }
    Socket open(Listener listener);
}
