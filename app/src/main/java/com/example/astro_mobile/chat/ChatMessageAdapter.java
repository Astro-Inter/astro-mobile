package com.example.astro_mobile.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        holder.sender.setText(user ? message.hasFailed() ? R.string.chat_user_failed_label
                : R.string.chat_user_label : R.string.chat_assistant_label);
        int textColor = holder.itemView.getContext().getColor(user ? R.color.astro_light_text : R.color.chat_text);
        holder.sender.setTextColor(holder.itemView.getContext().getColor(user
                ? R.color.astro_light_text : R.color.chat_muted));
        holder.text.setTextColor(textColor);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.bubble.getLayoutParams();
        float density = holder.itemView.getResources().getDisplayMetrics().density;
        params.setMarginStart(user ? Math.round(52 * density) : 0);
        params.setMarginEnd(user ? 0 : Math.round(8 * density));
        holder.bubble.setLayoutParams(params);
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
        final LinearLayout content;
        Holder(View view) {
            super(view);
            avatar = view.findViewById(R.id.image_chat_avatar);
            bubble = view.findViewById(R.id.container_chat_message);
            sender = view.findViewById(R.id.text_chat_sender);
            text = view.findViewById(R.id.text_chat_message);
            content = view.findViewById(R.id.container_chat_markdown);
        }
    }
}
