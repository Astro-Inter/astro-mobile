package com.example.astro_mobile.chat.human;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.astro_mobile.R;
import com.bumptech.glide.Glide;

/** Rodapé efêmero da conversa, sem entrar no histórico de mensagens. */
public final class HumanTypingAdapter extends RecyclerView.Adapter<HumanTypingAdapter.Holder> {
    private final HumanConversation conversation;
    private boolean typing;

    public HumanTypingAdapter(HumanConversation conversation) { this.conversation = conversation; }

    public void setTyping(boolean value) {
        if (typing == value) return;
        typing = value;
        if (typing) notifyItemInserted(0); else notifyItemRemoved(0);
    }
    @Override public int getItemCount() { return typing ? 1 : 0; }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_human_typing, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Glide.with(holder.avatar).load(conversation.profilePhotoUrl).circleCrop()
                .placeholder(R.drawable.profile_avatar_placeholder)
                .fallback(R.drawable.profile_avatar_placeholder)
                .error(R.drawable.profile_avatar_placeholder).into(holder.avatar);
        holder.indicator.show(true);
    }
    @Override public void onViewAttachedToWindow(@NonNull Holder holder) { holder.indicator.show(true); }
    @Override public void onViewDetachedFromWindow(@NonNull Holder holder) { holder.indicator.show(false); }
    @Override public void onViewRecycled(@NonNull Holder holder) {
        holder.indicator.show(false);
        Glide.with(holder.avatar).clear(holder.avatar);
    }
    static final class Holder extends RecyclerView.ViewHolder {
        final HumanTypingIndicator indicator;
        final ImageView avatar;
        Holder(View view) {
            super(view);
            indicator = new HumanTypingIndicator(view.findViewById(R.id.indicator_human_chat_typing));
            avatar = view.findViewById(R.id.image_human_typing_avatar);
        }
    }
}
