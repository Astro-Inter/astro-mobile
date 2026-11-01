package com.example.astro_mobile.chat;

import androidx.recyclerview.widget.RecyclerView;

/** Mantém o fim visível durante mudanças de tamanho sem interromper a leitura do histórico. */
final class ChatScrollController {
    private final RecyclerView list;
    private boolean follow = true;
    private final Runnable scroll = this::scrollToEnd;
    private void scrollToEnd() {
        if (!follow || list.getAdapter() == null || list.getAdapter().getItemCount() == 0) return;
        list.scrollToPosition(list.getAdapter().getItemCount() - 1);
        list.post(() -> { if (follow) list.scrollBy(0, Integer.MAX_VALUE); });
    }
    ChatScrollController(RecyclerView list) {
        this.list = list;
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(RecyclerView view, int state) {
                if (state == RecyclerView.SCROLL_STATE_IDLE) follow = !view.canScrollVertically(1);
            }
            @Override public void onScrolled(RecyclerView view, int dx, int dy) {
                if (view.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING)
                    follow = !view.canScrollVertically(1);
            }
        });
        list.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            if (b - t != ob - ot) changed();
        });
    }
    void outgoing() { follow = true; }
    void changed() { list.removeCallbacks(scroll); if (follow) list.post(scroll); }
}
