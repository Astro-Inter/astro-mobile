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

public final class HumanConversationAdapter extends RecyclerView.Adapter<HumanConversationAdapter.Row> {
    private final boolean manager;
    private final View.OnClickListener openConversation;

    public HumanConversationAdapter(boolean manager, View.OnClickListener openConversation) {
        this.manager = manager;
        this.openConversation = openConversation;
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Row(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_human_conversation, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        // Cada fluxo mostra apenas um contato de exemplo, sem simular dados da API.
        String name = row.itemView.getContext().getString(manager
                ? R.string.human_chat_employee_name : R.string.human_chat_manager_name);
        row.name.setText(manager ? name : Html.fromHtml(row.itemView.getContext()
                .getString(R.string.human_conversation_manager_label, name), Html.FROM_HTML_MODE_LEGACY));
        row.preview.setText(manager ? R.string.human_conversation_employee_preview
                : R.string.human_conversation_manager_preview);
        row.avatar.setImageResource(manager ? R.drawable.participant_avatar_1
                : R.drawable.human_chat_manager_avatar);
        row.count.setVisibility(manager ? View.GONE : View.VISIBLE);
        row.divider.setVisibility(manager ? View.VISIBLE : View.GONE);
        row.button.setOnClickListener(openConversation);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static final class Row extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView preview;
        final ImageView avatar;
        final View count;
        final View divider;
        final View button;

        Row(View view) {
            super(view);
            name = view.findViewById(R.id.text_human_conversation_name);
            preview = view.findViewById(R.id.text_human_conversation_preview);
            avatar = view.findViewById(R.id.image_human_conversation_avatar);
            count = view.findViewById(R.id.text_human_conversation_count);
            divider = view.findViewById(R.id.divider_human_conversation);
            button = view.findViewById(R.id.button_human_conversation);
        }
    }
}
