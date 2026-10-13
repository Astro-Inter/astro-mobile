package com.example.astro_mobile.employee.events;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EmployeeEventDetailsFragment extends Fragment {
    static final String EVENT_TITLE = "event_title";
    static final String EVENT_DATE = "event_date";
    static final String EVENT_STATUS = "event_status";
    static final String EVENT_URGENT = "event_urgent";
    static final String COMPLETED_EVENT = "completed_event";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private EmployeeEventDetailsViewModel state;
    private Runnable finishLoading;

    public EmployeeEventDetailsFragment() { super(R.layout.fragment_employee_event_details); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        state = new ViewModelProvider(this).get(EmployeeEventDetailsViewModel.class);
        state.initialize(new EmployeeEvent(args.getString(EVENT_TITLE),
                LocalDate.parse(args.getString(EVENT_DATE)),
                EmployeeEvent.Status.valueOf(args.getString(EVENT_STATUS)), args.getBoolean(EVENT_URGENT)));
        render(view);

        // Voltar mantém o calendário, a busca e os filtros da lista anterior.
        view.findViewById(R.id.button_employee_event_details_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_employee_event_attachment).setOnClickListener(clicked ->
                showMockNotice(R.string.employee_event_attachment_mock));
        view.findViewById(R.id.button_employee_event_evidence).setOnClickListener(clicked -> {
            // Recoloca somente o arquivo de exemplo, sem abrir câmera nem enviar arquivos.
            if (!state.isEvidenceAttached()) {
                state.setEvidenceAttached(true);
                renderEvidence(view);
            }
            showMockNotice(R.string.employee_event_evidence_mock);
        });
        view.findViewById(R.id.button_employee_event_remove_evidence).setOnClickListener(clicked -> {
            state.setEvidenceAttached(false);
            renderEvidence(view);
        });
        view.findViewById(R.id.button_employee_event_complete).setOnClickListener(clicked -> completeMock());

        // Mostra o skeleton por um segundo sem bloquear a interface.
        if (!state.isLoaded()) {
            View content = view.findViewById(R.id.scroll_employee_event_details);
            View skeleton = view.findViewById(R.id.skeleton_employee_event_details);
            content.setVisibility(View.INVISIBLE);
            skeleton.setVisibility(View.VISIBLE);
            finishLoading = () -> {
                state.markLoaded();
                skeleton.setVisibility(View.GONE);
                content.setVisibility(View.VISIBLE);
                finishLoading = null;
            };
            view.postDelayed(finishLoading, 1_000L);
        }
    }

    private void render(View view) {
        // Preenche o detalhe com o evento selecionado e informações claramente mockadas.
        EmployeeEvent event = state.getEvent();
        ((TextView) view.findViewById(R.id.text_employee_event_details_title)).setText(event.title);
        ((TextView) view.findViewById(R.id.text_employee_event_description)).setText(state.getDescription());
        ((TextView) view.findViewById(R.id.text_employee_event_deadline)).setText(DATE.format(event.date));
        ((TextView) view.findViewById(R.id.text_employee_event_validity)).setText(DATE.format(event.date.plusYears(2)));
        ((TextView) view.findViewById(R.id.text_employee_event_urgent_deadline)).setText(
                getString(R.string.employee_event_deadline_message, DATE.format(event.date)));
        TextView status = view.findViewById(R.id.text_employee_event_details_status);
        int statusText = event.status == EmployeeEvent.Status.DONE ? R.string.employee_events_done
                : event.status == EmployeeEvent.Status.REVIEW ? R.string.employee_events_review
                : event.urgent ? R.string.employee_event_urgent : R.string.employee_events_pending;
        status.setText(statusText);
        ImageView badge = view.findViewById(R.id.image_employee_event_badge);
        badge.setVisibility(event.urgent ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.container_employee_event_calendar).setVisibility(event.urgent ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.image_employee_event_urgent).setVisibility(event.urgent ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.container_employee_event_urgent_deadline).setVisibility(event.urgent ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.container_employee_event_completion).setVisibility(
                event.status == EmployeeEvent.Status.PENDING ? View.VISIBLE : View.GONE);
        renderEvidence(view);
    }

    private void renderEvidence(View view) {
        boolean attached = state.isEvidenceAttached();
        TextView evidence = view.findViewById(R.id.button_employee_event_evidence);
        evidence.setText(attached ? R.string.employee_event_evidence_filename : R.string.employee_event_add_evidence);
        evidence.setPaintFlags(attached ? evidence.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG
                : evidence.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        view.findViewById(R.id.button_employee_event_remove_evidence).setVisibility(attached ? View.VISIBLE : View.GONE);
    }

    private void completeMock() {
        // Devolve a conclusão local à lista e abre a confirmação, sem API.
        NavController navigation = NavHostFragment.findNavController(this);
        Bundle result = new Bundle();
        result.putString(EVENT_TITLE, state.getEvent().title);
        result.putString(EVENT_DATE, state.getEvent().date.toString());
        navigation.getBackStackEntry(R.id.employeeEventsFragment).getSavedStateHandle().set(COMPLETED_EVENT, result);
        navigation.navigate(R.id.action_employee_event_details_to_confirmation, AuthArgs.copy(getArguments()));
    }

    private void showMockNotice(int message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        // Cancela o carregamento pendente quando o detalhe é fechado.
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
