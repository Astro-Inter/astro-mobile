package com.example.astro_mobile.chat.human;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.astro_mobile.R;

/** Rodapé efêmero da conversa, sem entrar no histórico de mensagens. */
public final class HumanTypingAdapter extends RecyclerView.Adapter<HumanTypingAdapter.Holder> {
    private boolean typing;

    public void setTyping(boolean value) {
        if (typing == value) return;
        typing = value;
        if (typing) notifyItemInserted(0); else notifyItemRemoved(0);
    }
    @Override public int getItemCount() { return typing ? 1 : 0; }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_human_typing, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) { holder.indicator.show(true); }
    @Override public void onViewAttachedToWindow(@NonNull Holder holder) { holder.indicator.show(true); }
    @Override public void onViewDetachedFromWindow(@NonNull Holder holder) { holder.indicator.show(false); }
    @Override public void onViewRecycled(@NonNull Holder holder) { holder.indicator.show(false); }
    static final class Holder extends RecyclerView.ViewHolder {
        final HumanTypingIndicator indicator;
        Holder(View view) {
            super(view);
            indicator = new HumanTypingIndicator(view.findViewById(R.id.indicator_human_chat_typing));
        }
    }
}
