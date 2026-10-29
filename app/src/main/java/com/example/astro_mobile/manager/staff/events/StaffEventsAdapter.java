package com.example.astro_mobile.manager.staff.events;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;

import java.util.List;

public final class StaffEventsAdapter extends RecyclerView.Adapter<StaffEventsAdapter.Row> {
    public interface Listener { void onClick(StaffEvent event); }

    private final List<StaffEvent> events;
    private final Listener listener;

    public StaffEventsAdapter(List<StaffEvent> events, Listener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // O formato da linha é o mesmo já usado na lista de eventos do Gestor.
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manager_event, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        StaffEvent event = events.get(position);
        row.title.setText(event.title);
        row.deadline.setText(row.itemView.getContext().getString(R.string.employee_events_due, event.deadline));
        row.status.setTextColor(row.itemView.getContext().getColor(R.color.astro_tertiary_light_text));
        row.icon.setImageTintList(null);
        row.icon.setVisibility(View.VISIBLE);
        switch (event.status) {
            case PENDING:
                row.status.setText(R.string.employee_events_pending);
                row.icon.setVisibility(View.INVISIBLE);
                break;
            case DONE:
                row.status.setText(R.string.employee_events_done);
                row.icon.setImageResource(R.drawable.ic_employee_event_done);
                break;
            case EXPIRED:
                row.status.setText(R.string.staff_events_expired);
                row.icon.setImageResource(R.drawable.staff_event_expired);
                break;
            case ANALYSIS:
                row.status.setText(R.string.employee_events_review);
                row.icon.setImageResource(R.drawable.home_clock);
                row.icon.setImageTintList(ColorStateList.valueOf(row.itemView.getContext()
                        .getColor(R.color.astro_light_background)));
                break;
            case REJECTED:
            case CANCELLED:
                row.status.setText(event.status == StaffEvent.Status.REJECTED
                        ? R.string.employee_events_rejected : R.string.employee_events_cancelled);
                row.icon.setImageResource(R.drawable.conversations_close);
                row.icon.setImageTintList(ColorStateList.valueOf(row.itemView.getContext()
                        .getColor(R.color.employee_event_status_red)));
                break;
        }
        // O check tem raiz 18.2×18; o X preserva a raiz quadrada de 18dp.
        ViewGroup.LayoutParams params = row.icon.getLayoutParams();
        float density = row.itemView.getResources().getDisplayMetrics().density;
        params.width = Math.round((event.status == StaffEvent.Status.DONE ? 18.2f : 18f) * density);
        params.height = Math.round(18f * density);
        row.icon.setLayoutParams(params);

        // Cada linha leva o ID e a modalidade do próprio evento.
        row.button.setOnClickListener(clicked -> listener.onClick(event));
    }

    @Override public int getItemCount() { return events.size(); }

    static final class Row extends RecyclerView.ViewHolder {
        final TextView title, deadline, status;
        final ImageView icon;
        final View button;

        Row(View view) {
            super(view);
            title = view.findViewById(R.id.text_manager_event_title);
            deadline = view.findViewById(R.id.text_manager_event_subtitle);
            status = view.findViewById(R.id.text_manager_event_status);
            icon = view.findViewById(R.id.image_manager_event_status);
            button = view.findViewById(R.id.button_manager_event);
        }
    }
}
