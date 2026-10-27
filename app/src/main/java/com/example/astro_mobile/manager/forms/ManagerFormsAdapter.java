package com.example.astro_mobile.manager.forms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;

import java.util.List;
import java.util.function.Consumer;

public final class ManagerFormsAdapter extends RecyclerView.Adapter<ManagerFormsAdapter.Row> {
    private final List<ManagerForm> forms;
    private final Consumer<ManagerForm> onClick;

    public ManagerFormsAdapter(List<ManagerForm> forms, Consumer<ManagerForm> onClick) {
        this.forms = forms;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manager_form, parent, false);
        return new Row(item);
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        // Exibe o estado da resposta sem confundir envio com aprovação.
        ManagerForm form = forms.get(position);
        row.title.setText(form.title);
        row.deadline.setVisibility(form.deadline.isEmpty() ? View.GONE : View.VISIBLE);
        row.nr.setVisibility(form.nr.isEmpty() ? View.GONE : View.VISIBLE);
        row.deadline.setText(row.itemView.getContext().getString(R.string.manager_forms_deadline, form.deadline));
        row.nr.setText(form.nr);
        row.status.setText(statusLabel(form.status));
        row.icon.setImageTintList(form.status == ManagerForm.Status.REJECTED
                ? android.content.res.ColorStateList.valueOf(row.itemView.getContext().getColor(R.color.employee_event_status_red))
                : null);
        int icon = statusIcon(form.status);
        row.icon.setVisibility(icon == 0 ? View.INVISIBLE : View.VISIBLE);
        if (icon != 0) row.icon.setImageResource(icon);
        row.button.setOnClickListener(clicked -> onClick.accept(form));
    }

    @Override
    public int getItemCount() { return forms.size(); }

    private int statusLabel(ManagerForm.Status status) {
        switch (status) {
            case NEW: return R.string.manager_forms_new;
            case ANALYSIS: return R.string.manager_forms_analysis;
            case APPROVED: return R.string.manager_forms_approved;
            case REJECTED: return R.string.manager_forms_rejected;
            default: return R.string.manager_forms_pending;
        }
    }

    private int statusIcon(ManagerForm.Status status) {
        switch (status) {
            case NEW: return R.drawable.ic_employee_event_new;
            case ANALYSIS: return R.drawable.home_clock;
            case APPROVED: return R.drawable.ic_employee_event_done;
            case REJECTED: return R.drawable.conversations_close;
            default: return 0;
        }
    }

    static final class Row extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView deadline;
        final TextView nr;
        final TextView status;
        final ImageView icon;
        final View button;

        Row(View view) {
            super(view);
            title = view.findViewById(R.id.text_manager_form_title);
            deadline = view.findViewById(R.id.text_manager_form_deadline);
            nr = view.findViewById(R.id.text_manager_form_nr);
            status = view.findViewById(R.id.text_manager_form_status);
            icon = view.findViewById(R.id.image_manager_form_status);
            button = view.findViewById(R.id.button_manager_form);
        }
    }
}
