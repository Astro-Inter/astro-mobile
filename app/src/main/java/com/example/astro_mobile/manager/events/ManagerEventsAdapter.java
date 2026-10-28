package com.example.astro_mobile.manager.events;

import android.view.LayoutInflater;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;

import java.util.List;
import java.util.function.Consumer;

public final class ManagerEventsAdapter extends RecyclerView.Adapter<ManagerEventsAdapter.Row> {
    private final List<ManagerEvent> events;
    private final Consumer<ManagerEvent> onClick;

    public ManagerEventsAdapter(List<ManagerEvent> events, Consumer<ManagerEvent> onClick) {
        this.events = events;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manager_event, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        // Cada estado usa o texto e o ícone do seu exemplo no Figma.
        ManagerEvent event = events.get(position);
        row.title.setText(event.title);
        row.subtitle.setVisibility(event.status == ManagerEvent.Status.CANCELLED ? View.GONE : View.VISIBLE);
        row.icon.setVisibility(View.VISIBLE);
        row.icon.setImageTintList(event.status == ManagerEvent.Status.CANCELLED
                ? ColorStateList.valueOf(row.itemView.getContext().getColor(R.color.employee_event_status_red)) : null);
        switch (event.status) {
            case VALIDATION_PENDING:
                row.subtitle.setText(row.itemView.getContext().getString(
                        R.string.manager_events_pending_count, event.pendingValidations));
                row.status.setText(R.string.manager_events_validation_pending);
                row.icon.setImageResource(R.drawable.home_new);
                break;
            case ASSIGNMENT_PENDING:
                row.subtitle.setText(R.string.manager_events_assign_hint);
                row.status.setText(R.string.manager_events_assign);
                row.icon.setImageResource(R.drawable.manager_event_notebook);
                break;
            case NO_ACTION:
                row.subtitle.setText(R.string.manager_events_no_validation);
                row.status.setText(R.string.manager_events_no_action);
                row.icon.setVisibility(View.INVISIBLE);
                break;
            case FINISHED:
                row.subtitle.setText(R.string.manager_events_finished_status);
                row.status.setText(R.string.manager_events_finished_status);
                row.icon.setImageResource(R.drawable.ic_employee_event_done);
                break;
            case CANCELLED:
                row.status.setText(R.string.manager_events_cancelled);
                row.icon.setImageResource(R.drawable.conversations_close);
                break;
        }
        // O caderno ocupa 20dp; os demais preservam a geometria de 18dp do design.
        float size = event.status == ManagerEvent.Status.ASSIGNMENT_PENDING ? 20f : 18f;
        float density = row.itemView.getResources().getDisplayMetrics().density;
        ViewGroup.LayoutParams params = row.icon.getLayoutParams();
        params.width = Math.round((event.status == ManagerEvent.Status.FINISHED ? 18.2f : size) * density);
        params.height = Math.round(size * density);
        row.icon.setLayoutParams(params);
        row.button.setOnClickListener(clicked -> onClick.accept(event));
    }

    @Override
    public int getItemCount() { return events.size(); }

    static final class Row extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView status;
        final ImageView icon;
        final View button;

        Row(View view) {
            super(view);
            title = view.findViewById(R.id.text_manager_event_title);
            subtitle = view.findViewById(R.id.text_manager_event_subtitle);
            status = view.findViewById(R.id.text_manager_event_status);
            icon = view.findViewById(R.id.image_manager_event_status);
            button = view.findViewById(R.id.button_manager_event);
        }
    }
}
