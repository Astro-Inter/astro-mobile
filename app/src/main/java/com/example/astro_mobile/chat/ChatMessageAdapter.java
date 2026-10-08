package com.example.astro_mobile.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.format.DateFormat;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;

import io.noties.markwon.Markwon;

public final class ChatMessageAdapter extends ListAdapter<ChatMessage, ChatMessageAdapter.Holder> {
    private final Markwon markdown;

    public ChatMessageAdapter(Markwon markdown) {
        super(new DiffUtil.ItemCallback<ChatMessage>() {
            @Override public boolean areItemsTheSame(@NonNull ChatMessage old, @NonNull ChatMessage next) {
                return old == next;
            }
            @Override public boolean areContentsTheSame(@NonNull ChatMessage old, @NonNull ChatMessage next) {
                return old.isFromUser() == next.isFromUser() && old.hasFailed() == next.hasFailed()
                        && old.getText().equals(next.getText());
            }
        });
        this.markdown = markdown;
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        ChatMessage message = getItem(position);
        boolean user = message.isFromUser();
        holder.avatar.setVisibility(user ? View.GONE : View.VISIBLE);
        holder.bubble.setBackgroundResource(user ? R.drawable.bg_chat_user : R.drawable.bg_chat_assistant);
        // O autor é identificado pela posição e pelo avatar; só falhas recebem legenda.
        holder.sender.setVisibility(message.hasFailed() ? View.VISIBLE : View.GONE);
        holder.sender.setText(R.string.chat_user_failed_label);
        int textColor = holder.itemView.getContext().getColor(user ? R.color.astro_light_text : R.color.chat_text);
        holder.sender.setTextColor(holder.itemView.getContext().getColor(user
                ? R.color.astro_light_text : R.color.chat_muted));
        holder.text.setTextColor(textColor);
        ConstraintLayout.LayoutParams params =
                (ConstraintLayout.LayoutParams) holder.bubble.getLayoutParams();
        float density = holder.itemView.getResources().getDisplayMetrics().density;
        params.width = user ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
        // A resposta ocupa a largura disponível; só a mensagem do usuário acompanha o texto.
        params.constrainedWidth = user;
        params.matchConstraintDefaultWidth = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD;
        params.horizontalBias = user ? 1f : 0f;
        params.setMarginStart(Math.round((user ? 52 : 53) * density));
        params.setMarginEnd(user ? 0 : Math.round(12 * density));
        holder.bubble.setLayoutParams(params);
        // Horário local da mensagem, preservado ao tentar enviar novamente.
        holder.time.setText(DateFormat.format("HH:mm", message.getCreatedAt()));
        holder.time.setVisibility(message.getCreatedAt() == 0 ? View.GONE : View.VISIBLE);
        ConstraintLayout.LayoutParams timeParams =
                (ConstraintLayout.LayoutParams) holder.time.getLayoutParams();
        timeParams.topToBottom = user ? ConstraintLayout.LayoutParams.UNSET : R.id.container_chat_markdown;
        timeParams.topMargin = user ? 0 : Math.round(4 * density);
        holder.time.setLayoutParams(timeParams);
        // O texto enviado permanece literal; apenas respostas da IA são Markdown.
        holder.text.setVisibility(user ? View.VISIBLE : View.GONE);
        holder.content.setVisibility(user ? View.GONE : View.VISIBLE);
        if (user) {
            holder.content.removeAllViews();
            holder.text.setMovementMethod(null);
            holder.text.setText(message.getText());
        } else {
            ChatMarkdownContent.render(holder.content, markdown, message.getText());
        }
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final View bubble;
        final TextView sender;
        final TextView text;
        final TextView time;
        final LinearLayout content;
        Holder(View view) {
            super(view);
            avatar = view.findViewById(R.id.image_chat_avatar);
            bubble = view.findViewById(R.id.container_chat_message);
            sender = view.findViewById(R.id.text_chat_sender);
            text = view.findViewById(R.id.text_chat_message);
            time = view.findViewById(R.id.text_chat_time);
            content = view.findViewById(R.id.container_chat_markdown);
        }
    }
}
