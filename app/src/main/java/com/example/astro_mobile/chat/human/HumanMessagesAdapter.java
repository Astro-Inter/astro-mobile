package com.example.astro_mobile.chat.human;

import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
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
    public interface Listener {
        void onOptions(HumanMessage message, View anchor);
        void onQuote(String messageId);
        void onCopy(HumanMessage message);
    }
    private final HumanConversation conversation;
    private final List<HumanMessage> messages;
    private final Listener listener;

    public HumanMessagesAdapter(HumanConversation conversation, Listener listener) {
        this.conversation = conversation;
        this.messages = conversation.messages;
        this.listener = listener;
    }
    @NonNull @Override public Row onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_human_message, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Row row, int position) {
        HumanMessage message = messages.get(position);
        row.text.setText(message.text);
        row.time.setText(message.time);
        row.copy.setOnClickListener(view -> listener.onCopy(message));
        row.bubble.setBackgroundResource(message.sent ? R.drawable.bg_chat_user : R.drawable.bg_chat_assistant);
        float density = row.itemView.getResources().getDisplayMetrics().density;
        boolean sameNext = position + 1 < messages.size() && messages.get(position + 1).sent == message.sent
                && sameDay(message, messages.get(position + 1));
        row.itemView.setPadding(0, 0, 0, Math.round((sameNext ? 6 : 16) * density));
        Glide.with(row.avatar).load(conversation.profilePhotoUrl).circleCrop()
                .placeholder(R.drawable.profile_avatar_placeholder).fallback(R.drawable.profile_avatar_placeholder)
                .error(R.drawable.profile_avatar_placeholder).into(row.avatar);
        row.avatar.setVisibility(message.sent || sameNext ? View.GONE : View.VISIBLE);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) row.bubble.getLayoutParams();
        boolean hasQuote = message.replyToMessageId != null;
        params.width = hasQuote ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
        params.constrainedWidth = !hasQuote;
        params.matchConstraintDefaultWidth = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD;
        params.horizontalBias = message.sent ? 1f : 0f;
        params.setMarginStart(Math.round((message.sent ? 52 : 45) * density));
        params.setMarginEnd(Math.round((message.sent ? 0 : 12) * density));
        row.bubble.setLayoutParams(params);
        HumanMessage quoted = conversation.findMessage(message.replyToMessageId);
        row.quote.setVisibility(hasQuote ? View.VISIBLE : View.GONE);
        row.quoteAuthor.setText(R.string.human_reply_action);
        if (quoted != null) row.quoteAuthor.setText(quoted.sent
                ? row.itemView.getContext().getString(R.string.human_reply_you) : conversation.name);
        row.quoteContent.setText(quoted == null ? row.itemView.getContext().getString(R.string.human_reply_missing) : quoted.text);
        row.quote.setOnClickListener(view -> listener.onQuote(message.replyToMessageId));
        row.bubble.setOnClickListener(view -> listener.onOptions(message, row.bubble));
        row.bubble.setOnLongClickListener(view -> { listener.onOptions(message, row.bubble); return true; });
        row.bubble.setContentDescription(row.itemView.getContext().getString(R.string.human_reply_accessibility,
                message.sent ? row.itemView.getContext().getString(R.string.human_reply_you) : conversation.name, message.time, message.text));
    }
    private static boolean sameDay(HumanMessage first, HumanMessage second) {
        return first.createdAt != null && second.createdAt != null
                && java.time.Instant.parse(first.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                .equals(java.time.Instant.parse(second.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate());
    }
    @Override public int getItemCount() { return messages.size(); }
    public void notifyHistoryReplaced(int previousCount) { notifyDataSetChanged(); }
    static final class Row extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final View bubble, quote, copy;
        final TextView text, time, quoteAuthor, quoteContent;
        Row(View view) {
            super(view);
            avatar = view.findViewById(R.id.image_human_message_avatar);
            bubble = view.findViewById(R.id.container_human_message);
            text = view.findViewById(R.id.text_human_message_content);
            time = view.findViewById(R.id.text_human_message_time);
            copy = view.findViewById(R.id.button_human_message_copy);
            bubble.addOnLayoutChangeListener((changed, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                // Extend into the footer margin and bottom padding, preserving a 48dp touch target.
                float density = view.getResources().getDisplayMetrics().density;
                Rect target = new Rect();
                copy.getDrawingRect(target);
                ((ViewGroup) bubble).offsetDescendantRectToMyCoords(copy, target);
                target.top -= Math.round(2 * density);
                target.bottom = target.top + Math.round(48 * density);
                bubble.setTouchDelegate(new TouchDelegate(target, copy));
            });
            quote = view.findViewById(R.id.container_human_quote);
            quoteAuthor = view.findViewById(R.id.text_human_quote_author);
            quoteContent = view.findViewById(R.id.text_human_quote_content);
        }
    }
}
