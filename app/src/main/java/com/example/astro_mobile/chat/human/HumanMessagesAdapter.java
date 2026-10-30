package com.example.astro_mobile.chat.human;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;
import com.bumptech.glide.Glide;

import java.util.List;

public final class HumanMessagesAdapter extends RecyclerView.Adapter<HumanMessagesAdapter.Row> {
    private final List<HumanMessage> messages;
    private final int avatar;
    private final boolean manager;
    private final String profilePhotoUrl;

    public HumanMessagesAdapter(List<HumanMessage> messages, int avatar, boolean manager, String profilePhotoUrl) {
        this.messages = messages;
        this.avatar = avatar;
        this.manager = manager;
        this.profilePhotoUrl = profilePhotoUrl;
    }

    @NonNull @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reutiliza a composição visual; nenhum estado ou serviço do chatbot é usado.
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        HumanMessage message = messages.get(position);
        row.text.setText(message.text);
        row.text.setTextSize(manager ? 17f : 16f);
        row.text.setTextColor(row.itemView.getContext().getColor(manager
                ? R.color.chat_text : R.color.astro_light_background));
        row.time.setText(message.time);
        Glide.with(row.avatar).load(profilePhotoUrl).circleCrop().placeholder(avatar).fallback(avatar).error(avatar).into(row.avatar);
        row.avatar.setVisibility(message.sent ? View.GONE : View.VISIBLE);
        row.bubble.setBackgroundResource(message.sent ? R.drawable.bg_chat_user : R.drawable.bg_chat_assistant);

        // Balões acompanham o texto e quebram linhas sem encobrir o horário ou ultrapassar a tela.
        float density = row.itemView.getResources().getDisplayMetrics().density;
        float avatarSize = manager ? 37f : 41f;
        ViewGroup.LayoutParams avatarParams = row.avatar.getLayoutParams();
        avatarParams.width = avatarParams.height = Math.round(avatarSize * density);
        row.avatar.setLayoutParams(avatarParams);
        ConstraintLayout.LayoutParams bubbleParams = (ConstraintLayout.LayoutParams) row.bubble.getLayoutParams();
        bubbleParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        bubbleParams.constrainedWidth = true;
        bubbleParams.horizontalBias = message.sent ? 1f : 0f;
        bubbleParams.setMarginStart(Math.round((message.sent ? 48f : avatarSize + 8f) * density));
        bubbleParams.setMarginEnd(0);
        row.bubble.setLayoutParams(bubbleParams);
    }

    @Override public int getItemCount() { return messages.size(); }

    public void notifyHistoryReplaced(int previousCount) {
        int currentCount = messages.size();
        if (previousCount > currentCount) {
            notifyItemRangeRemoved(currentCount, previousCount - currentCount);
        } else if (currentCount > previousCount) {
            if (previousCount > 0) notifyItemRangeChanged(0, previousCount);
            notifyItemRangeInserted(previousCount, currentCount - previousCount);
            return;
        }
        if (currentCount > 0) notifyItemRangeChanged(0, currentCount);
    }

    static final class Row extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final View bubble;
        final TextView text, time;

        Row(View view) {
            super(view);
            avatar = view.findViewById(R.id.image_chat_avatar);
            bubble = view.findViewById(R.id.container_chat_message);
            text = view.findViewById(R.id.text_chat_message);
            time = view.findViewById(R.id.text_chat_time);
        }
    }
}
