package com.example.astro_mobile.manager.events;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.ManagerEventDetailsData;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class ManagerEventDetailsFragment extends Fragment {
    public static final String EVENT_TITLE = "event_title";
    public static final String COMPLETION_MODE = "event_completion_mode";
    private ManagerEventDetailsViewModel state;
    private boolean selfDeclaration;

    public ManagerEventDetailsFragment() { super(R.layout.fragment_manager_event_details); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this).get(ManagerEventDetailsViewModel.class);
        Bundle args = requireArguments();
        selfDeclaration = ManagerEvent.CompletionMode.valueOf(args.getString(COMPLETION_MODE))
                == ManagerEvent.CompletionMode.SELF_DECLARATION;
        ((TextView) view.findViewById(R.id.text_manager_event_title)).setText(args.getString(EVENT_TITLE));
        ((TextView) view.findViewById(R.id.text_manager_event_subtitle)).setText(selfDeclaration
                ? R.string.manager_event_self_declaration : R.string.manager_event_assignment);
        ((TextView) view.findViewById(R.id.text_manager_event_mode)).setText(selfDeclaration
                ? R.string.employee_event_self_declaration : R.string.manager_event_assignment_mode);
        view.findViewById(R.id.container_manager_event_validations)
                .setVisibility(selfDeclaration ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.button_manager_event_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_manager_event_close).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(HomeNavigation.destination(requireContext()), false));
        view.findViewById(R.id.button_manager_event_attachment).setOnClickListener(clicked -> {
            openMaterial(state.getDetails().externalLink);
        });
        view.findViewById(R.id.button_manager_event_participants).setOnClickListener(clicked -> {
            Bundle participantArgs = AuthArgs.copy(getArguments());
            if (state.getDetails() != null) {
                participantArgs.putString(EVENT_TITLE, state.getDetails().eventTitle);
            }
            NavHostFragment.findNavController(this).navigate(R.id.action_manager_event_details_to_participants, participantArgs);
        });
        if (state.getDetails() == null) loadDetails(view);
        else render(view, state.getDetails());
    }

    // Enquanto os dados chegam, mantém o skeleton da modalidade correspondente.
    private void showLoading(View view) {
        view.findViewById(R.id.scroll_manager_event_details).setVisibility(View.INVISIBLE);
        View skeleton = view.findViewById(R.id.skeleton_manager_event_details);
        skeleton.findViewById(R.id.skeleton_manager_event_validations)
                .setVisibility(selfDeclaration ? View.VISIBLE : View.GONE);
        skeleton.setVisibility(View.VISIBLE);
    }

    private void showContent(View view) {
        view.findViewById(R.id.skeleton_manager_event_details).setVisibility(View.GONE);
        view.findViewById(R.id.scroll_manager_event_details).setVisibility(View.VISIBLE);
    }

    private void loadDetails(View view) {
        showLoading(view);
        state.load(requireArguments().getLong("event_id"), selfDeclaration, new ManagerEventDetailsViewModel.Callback() {
            @Override public void onSuccess() {
                if (getView() != view || !isAdded()) return;
                render(view, state.getDetails());
            }
            @Override public void onFailure(FailureKind failure) {
                if (getView() != view || !isAdded()) return;
                if (failure == FailureKind.SESSION) SessionNavigation.signOut(ManagerEventDetailsFragment.this);
                else NavHostFragment.findNavController(ManagerEventDetailsFragment.this).navigate(
                        failure == FailureKind.CONNECTION ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
            }
        });
    }

    // Preenche a tela com o retorno; validade ainda não faz parte do contrato.
    private void render(View view, ManagerEventDetailsData data) {
        setValue(view, R.id.text_manager_event_title, data.eventTitle);
        setValue(view, R.id.text_manager_event_description, data.eventDescription);
        setValue(view, R.id.text_manager_event_sender, data.managerName);
        if (selfDeclaration) {
            setValue(view, R.id.text_manager_event_pending_count, data.pendingValidationCount);
            setValue(view, R.id.text_manager_event_done_count, data.validatedCount);
            setValue(view, R.id.text_manager_event_not_submitted_count, data.notSubmittedCount);
        }
        setValue(view, R.id.text_manager_event_participant_count, data.participantCount);
        String deadline = data.lastClassLimitDate;
        if (deadline != null) {
            try { deadline = LocalDateTime.parse(deadline).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
            catch (DateTimeParseException ignored) { deadline = null; }
        }
        setValue(view, R.id.text_manager_event_deadline, deadline);
        setValue(view, R.id.text_manager_event_validity, null);
        boolean hasMaterial = data.externalLink != null && !data.externalLink.trim().isEmpty();
        view.findViewById(R.id.heading_manager_event_material).setVisibility(hasMaterial ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.container_manager_event_material).setVisibility(hasMaterial ? View.VISIBLE : View.GONE);
        showContent(view);
    }

    private void setValue(View view, int id, Object value) {
        ((TextView) view.findViewById(id)).setText(value == null || value.toString().trim().isEmpty()
                ? getString(R.string.staff_detail_value_unavailable) : value.toString());
    }

    private void openMaterial(String link) {
        Uri uri = Uri.parse(link);
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            Toast.makeText(requireContext(), R.string.employee_event_open_error, Toast.LENGTH_SHORT).show();
            return;
        }
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException ignored) {
            Toast.makeText(requireContext(), R.string.employee_event_open_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onResume() {
        super.onResume();
        Boolean retry = NavHostFragment.findNavController(this).getCurrentBackStackEntry()
                .getSavedStateHandle().remove("retry_manager_event_details");
        if (Boolean.TRUE.equals(retry) && getView() != null) loadDetails(getView());
    }

    @Override public void onDestroyView() {
        state.cancel();
        super.onDestroyView();
    }
}
