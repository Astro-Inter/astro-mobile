package com.example.astro_mobile.manager.events.participants;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;

import java.util.List;

public final class EventParticipantsAdapter extends RecyclerView.Adapter<EventParticipantsAdapter.Row> {
    public interface OnParticipantClickListener {
        void onClick(EventParticipant participant);
    }

    private final int[] avatars = {R.drawable.participant_avatar_1,
            R.drawable.participant_avatar_2, R.drawable.participant_avatar_3};
    private final List<EventParticipant> participants;
    private final String eventTitle;
    private final boolean assignmentMode;
    private final OnParticipantClickListener participantClick;

    public EventParticipantsAdapter(List<EventParticipant> participants, String eventTitle,
                                    boolean assignmentMode, OnParticipantClickListener participantClick) {
        this.participants = participants;
        this.eventTitle = eventTitle;
        this.assignmentMode = assignmentMode;
        this.participantClick = participantClick;
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_participant, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        // Mantém os textos, retratos e indicadores de cada exemplo do Figma.
        EventParticipant participant = participants.get(position);
        row.name.setText(participant.name);
        row.event.setText(eventTitle);
        row.avatar.setImageResource(avatars[participant.avatarIndex]);
        boolean validationPending = participant.status == EventParticipant.Status.VALIDATION_PENDING;
        row.icon.setVisibility(View.VISIBLE);
        switch (participant.status) {
            case VALIDATION_PENDING:
                row.status.setText(R.string.manager_events_validation_pending);
                row.icon.setImageResource(R.drawable.home_new);
                break;
            case AWAITING_COMPLETION:
                row.status.setText(R.string.participants_awaiting_completion);
                row.icon.setVisibility(View.INVISIBLE);
                break;
            case VALIDATED:
                row.status.setText(R.string.participants_validated_status);
                row.icon.setImageResource(R.drawable.ic_employee_event_done);
                break;
            case ASSIGNED:
                row.status.setText(R.string.participants_assigned_status);
                row.icon.setImageResource(R.drawable.ic_employee_event_done);
                break;
        }
        // Preserva o tamanho de raiz do ponto vermelho e do check dentro do slot de 38dp.
        float density = row.itemView.getResources().getDisplayMetrics().density;
        ViewGroup.LayoutParams iconParams = row.icon.getLayoutParams();
        iconParams.width = Math.round((validationPending ? 22f : 18.2f) * density);
        iconParams.height = Math.round((validationPending ? 22f : 18f) * density);
        row.icon.setLayoutParams(iconParams);
        // No modo Atribuição, os exemplos ainda não concluídos abrem a tela do participante.
        boolean actionable = validationPending || (assignmentMode
                && participant.status == EventParticipant.Status.AWAITING_COMPLETION);
        row.button.setOnClickListener(actionable ? clicked -> participantClick.onClick(participant) : null);
        row.button.setClickable(actionable);
        row.button.setFocusable(actionable);
        row.button.setBackgroundResource(actionable ? R.drawable.bg_home_row_ripple : 0);
    }

    @Override
    public int getItemCount() { return participants.size(); }

    static final class Row extends RecyclerView.ViewHolder {
        final TextView name, event, status;
        final ImageView avatar, icon;
        final View button;

        Row(View view) {
            super(view);
            name = view.findViewById(R.id.text_event_participant_name);
            event = view.findViewById(R.id.text_event_participant_event);
            status = view.findViewById(R.id.text_event_participant_status);
            avatar = view.findViewById(R.id.image_event_participant_avatar);
            icon = view.findViewById(R.id.image_event_participant_status);
            button = view.findViewById(R.id.button_event_participant);
        }
    }
}
