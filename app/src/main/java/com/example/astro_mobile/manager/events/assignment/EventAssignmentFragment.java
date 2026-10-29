package com.example.astro_mobile.manager.events.assignment;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.EventAssignmentData;
import com.example.astro_mobile.shared.navigation.HomeNavigation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class EventAssignmentFragment extends Fragment {
    private EventAssignmentViewModel state;
    public EventAssignmentFragment() { super(R.layout.fragment_event_assignment); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this).get(EventAssignmentViewModel.class);
        view.findViewById(R.id.button_assignment_back).setOnClickListener(clicked -> NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_assignment_event).setOnClickListener(clicked -> NavHostFragment.findNavController(this).popBackStack(R.id.managerEventDetailsFragment, false));
        view.findViewById(R.id.button_assignment_close).setOnClickListener(clicked -> NavHostFragment.findNavController(this).popBackStack(HomeNavigation.destination(requireContext()), false));
        view.findViewById(R.id.button_assignment_submit).setOnClickListener(clicked -> submit(view));
        if (state.getDetails() == null) {
            String retry = NavHostFragment.findNavController(this).getCurrentBackStackEntry().getSavedStateHandle().get("retry_event_assignment");
            if (retry == null) load(view);
        } else render(view);
    }

    // O skeleton acompanha a consulta e a tela só aparece com dados reais.
    private void load(View view) {
        view.findViewById(R.id.container_assignment_content).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.skeleton_event_assignment).setVisibility(View.VISIBLE);
        state.load(requireArguments().getLong("event_id"), requireArguments().getLong("participant_id"),
                new EventAssignmentViewModel.Callback() {
                    @Override public void onSuccess() { if (getView() == view && isAdded()) render(view); }
                    @Override public void onFailure(FailureKind failure) { if (getView() == view && isAdded()) showFailure(failure, "load"); }
                });
    }
    private void render(View view) {
        EventAssignmentData data = state.getDetails();
        setValue(view, R.id.text_assignment_participant, data.participantName);
        setValue(view, R.id.text_assignment_event, data.eventTitle);
        setValue(view, R.id.text_assignment_role, data.roleName);
        setValue(view, R.id.text_assignment_unit, data.unitName);
        String deadline = data.endDate;
        if (deadline != null) {
            try { deadline = LocalDateTime.parse(deadline).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
            catch (DateTimeParseException ignored) { deadline = null; }
        }
        setValue(view, R.id.text_assignment_deadline, deadline);
        Glide.with(view).load(data.profilePhotoUrl).circleCrop().placeholder(R.drawable.profile_avatar_placeholder)
                .fallback(R.drawable.profile_avatar_placeholder).error(R.drawable.profile_avatar_placeholder)
                .into((ImageView) view.findViewById(R.id.image_assignment_participant));
        view.findViewById(R.id.skeleton_event_assignment).setVisibility(View.GONE);
        view.findViewById(R.id.container_assignment_content).setVisibility(View.VISIBLE);
    }
    private void setValue(View view, int id, String value) {
        ((TextView) view.findViewById(id)).setText(value == null || value.trim().isEmpty()
                ? getString(R.string.staff_detail_value_unavailable) : value);
    }

    // Desativa o botão durante o POST e só confirma após o sucesso da API.
    private void submit(View view) {
        if (state.isLoading() || state.getDetails() == null) return;
        View button = view.findViewById(R.id.button_assignment_submit);
        button.setEnabled(false);
        ((TextView) button).setText(R.string.event_assignment_sending);
        state.submit(requireArguments().getLong("event_id"), requireArguments().getLong("participant_id"),
                new EventAssignmentViewModel.Callback() {
                    @Override public void onSuccess() {
                        if (getView() != view || !isAdded()) return;
                        NavController nav = NavHostFragment.findNavController(EventAssignmentFragment.this);
                        nav.getBackStackEntry(R.id.eventParticipantsFragment).getSavedStateHandle().set("retry_event_participants", true);
                        // Se a lista do funcionário ainda estiver aberta, atualiza-a ao retornar.
                        try {
                            nav.getBackStackEntry(R.id.staffEventsFragment).getSavedStateHandle()
                                    .set("retry_staff_events", true);
                        } catch (IllegalArgumentException noStaffEvents) {
                            // A ação também pode ser aberta diretamente pela lista geral de eventos.
                        }
                        nav.navigate(R.id.action_assignment_to_confirmation);
                    }
                    @Override public void onFailure(FailureKind failure) {
                        if (getView() != view || !isAdded()) return;
                        button.setEnabled(true);
                        ((TextView) button).setText(R.string.event_assignment_submit);
                        showFailure(failure, "submit");
                    }
                });
    }
    private void showFailure(FailureKind failure, String operation) {
        if (failure == FailureKind.SESSION) { SessionNavigation.signOut(this); return; }
        if ("submit".equals(operation)) {
            // Uma falha de transporte pode ocorrer depois do registro no servidor.
            NavHostFragment.findNavController(this).getBackStackEntry(R.id.eventParticipantsFragment)
                    .getSavedStateHandle().set("retry_event_participants", true);
        }
        Bundle args = new Bundle();
        args.putString("retry_event_assignment", operation);
        NavHostFragment.findNavController(this).navigate(
                failure == FailureKind.CONNECTION ? R.id.connectionErrorFragment : R.id.genericErrorFragment, args);
    }
    @Override public void onResume() {
        super.onResume();
        String retry = NavHostFragment.findNavController(this).getCurrentBackStackEntry()
                .getSavedStateHandle().remove("retry_event_assignment");
        if (getView() == null || retry == null) return;
        if ("submit".equals(retry)) submit(getView());
        else load(getView());
    }
    @Override public void onDestroyView() { state.cancel(); super.onDestroyView(); }
}
