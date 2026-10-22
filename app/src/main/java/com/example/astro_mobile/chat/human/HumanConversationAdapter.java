package com.example.astro_mobile.chat.human;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;

import java.util.Collections;
import java.util.List;

public final class HumanConversationAdapter extends RecyclerView.Adapter<HumanConversationAdapter.Row> {
    public interface Listener { void open(HumanConversation conversation); }
    private final boolean contacts;
    private final Listener listener;
    private List<HumanConversation> items = Collections.emptyList();

    public HumanConversationAdapter(boolean contacts, Listener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    public void submit(List<HumanConversation> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Row(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_human_conversation, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Row row, int position) {
        HumanConversation conversation = items.get(position);
        row.name.setText(conversation.manager ? Html.fromHtml(row.itemView.getContext()
                .getString(R.string.human_conversation_manager_label, Html.escapeHtml(conversation.name)),
                Html.FROM_HTML_MODE_LEGACY) : conversation.name);
        // A pesquisa mostra identificação; a lista mostra a última mensagem enviada.
        row.preview.setText(contacts ? conversation.role : row.itemView.getContext().getString(
                R.string.human_conversation_sent_preview,
                conversation.messages.get(conversation.messages.size() - 1).text));
        int[] avatars = {R.drawable.participant_avatar_1, R.drawable.participant_avatar_2,
                R.drawable.participant_avatar_3};
        row.avatar.setImageResource(conversation.manager ? R.drawable.human_chat_manager_avatar
                : avatars[conversation.avatarIndex]);
        row.button.setOnClickListener(clicked -> listener.open(conversation));
    }

    @Override public int getItemCount() { return items.size(); }

    static final class Row extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView preview;
        final ImageView avatar;
        final View button;

        Row(View view) {
            super(view);
            name = view.findViewById(R.id.text_human_conversation_name);
            preview = view.findViewById(R.id.text_human_conversation_preview);
            avatar = view.findViewById(R.id.image_human_conversation_avatar);
            button = view.findViewById(R.id.button_human_conversation);
        }
    }
}
