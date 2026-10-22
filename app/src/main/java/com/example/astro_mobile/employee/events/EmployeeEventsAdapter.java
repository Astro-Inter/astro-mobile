package com.example.astro_mobile.employee.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.res.ColorStateList;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

public final class EmployeeEventsAdapter extends RecyclerView.Adapter<EmployeeEventsAdapter.Row> {
    private final List<EmployeeEvent> events;
    private final Consumer<EmployeeEvent> onClick;

    public EmployeeEventsAdapter(List<EmployeeEvent> events, Consumer<EmployeeEvent> onClick) {
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
        // Limpa o tint da linha reciclada antes de aplicar cada estado.
        row.icon.setImageTintList(null);
        row.icon.setVisibility(View.VISIBLE);
        int description;
        switch (event.displayStatus) {
            case URGENT:
                description = R.string.employee_events_urgent;
                row.icon.setImageResource(R.drawable.ic_employee_event_warning);
                break;
            case NEW:
                description = R.string.employee_events_new;
                row.icon.setImageResource(R.drawable.ic_employee_event_new);
                break;
            case DONE:
                description = R.string.employee_events_done;
                row.icon.setImageResource(R.drawable.ic_employee_event_done);
                break;
            case ANALYSIS:
                description = R.string.employee_events_review;
                row.icon.setImageResource(R.drawable.home_clock);
                break;
            case CANCELLED:
            case REJECTED:
            case EXPIRED:
                description = event.displayStatus == EventStatusClassifier.DisplayStatus.CANCELLED
                        ? R.string.employee_events_cancelled
                        : event.displayStatus == EventStatusClassifier.DisplayStatus.REJECTED
                        ? R.string.employee_events_rejected : R.string.employee_events_expired;
                row.icon.setImageResource(R.drawable.conversations_close);
                row.icon.setImageTintList(ColorStateList.valueOf(row.itemView.getContext()
                        .getColor(R.color.employee_event_status_red)));
                break;
            default:
                description = R.string.employee_events_pending;
                row.icon.setImageDrawable(null);
                row.icon.setVisibility(View.INVISIBLE);
        }
        row.status.setText(description);
        ViewCompat.setStateDescription(row.button, row.itemView.getContext().getString(description));
        row.button.setOnClickListener(clicked -> onClick.accept(event));
    }

    @Override public int getItemCount() { return events.size(); }

    static final class Row extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView due;
        final ImageView icon;
        final View button;
        final TextView status;

        Row(View view) {
            super(view);
            title = view.findViewById(R.id.text_employee_event_title);
            due = view.findViewById(R.id.text_employee_event_due);
            icon = view.findViewById(R.id.image_employee_event_status);
            button = view.findViewById(R.id.button_employee_event);
            status = view.findViewById(R.id.text_employee_event_status);
        }
    }
}
