package com.example.astro_mobile.employee.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;

public final class EmployeeEventsAdapter extends RecyclerView.Adapter<EmployeeEventsAdapter.Row> {
    private final List<EmployeeEvent> events;
    private final View.OnClickListener onClick;

    public EmployeeEventsAdapter(List<EmployeeEvent> events, View.OnClickListener onClick) {
        this.events = events;
        this.onClick = onClick;
    }

    @NonNull @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee_event, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        // A linha é a mesma na lista principal e no resumo do dia.
        EmployeeEvent event = events.get(position);
        row.title.setText(event.title);
        String dueDate = event.date.equals(LocalDate.now().plusDays(1))
                ? row.itemView.getContext().getString(R.string.employee_events_tomorrow)
                : DateTimeFormatter.ofPattern("dd/MM/yyyy").format(event.date);
        row.due.setText(row.itemView.getContext().getString(R.string.employee_events_due,
                dueDate));
        if (event.urgent) {
            row.icon.setImageResource(R.drawable.ic_employee_event_warning);
        } else if (event.status == EmployeeEvent.Status.PENDING) {
            row.icon.setImageResource(R.drawable.ic_employee_event_new);
        } else if (event.status == EmployeeEvent.Status.DONE) {
            row.icon.setImageResource(R.drawable.ic_employee_event_done);
        } else {
            // O estado “Em análise” não exibe ícone no frame do Figma.
            row.icon.setImageDrawable(null);
        }
        int description = event.status == EmployeeEvent.Status.DONE ? R.string.employee_events_done
                : event.status == EmployeeEvent.Status.REVIEW ? R.string.employee_events_review : R.string.employee_events_pending;
        ViewCompat.setStateDescription(row.button, row.itemView.getContext().getString(description));
        row.button.setOnClickListener(onClick);
    }

    @Override public int getItemCount() { return events.size(); }

    static final class Row extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView due;
        final ImageView icon;
        final View button;

        Row(View view) {
            super(view);
            title = view.findViewById(R.id.text_employee_event_title);
            due = view.findViewById(R.id.text_employee_event_due);
            icon = view.findViewById(R.id.image_employee_event_status);
            button = view.findViewById(R.id.button_employee_event);
        }
    }
}
