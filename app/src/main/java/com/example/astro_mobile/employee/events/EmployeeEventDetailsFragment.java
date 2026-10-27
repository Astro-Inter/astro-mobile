package com.example.astro_mobile.employee.events;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.api.EventEvidenceFile;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.CollaboratorEventDetailsData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class EmployeeEventDetailsFragment extends Fragment {
    public static final String EVENT_ID = "event_id";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private EmployeeEventDetailsViewModel state;
    private final ActivityResultLauncher<String[]> selectEvidence = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (getView() == null) return;
                try {
                    if (uris.size() + state.getFiles().size() > 3) {
                        showEvidenceError(getString(R.string.employee_event_file_limit));
                        return;
                    }
                    for (Uri uri : uris) state.addFile(EventEvidenceFile.read(requireContext().getContentResolver(), uri));
                    showEvidenceError(null);
                } catch (IOException | RuntimeException error) {
                    showEvidenceError(getString(R.string.employee_event_file_invalid));
                }
                if (state.getDetails() != null) renderEvidence(requireView());
            });

    public EmployeeEventDetailsFragment() { super(R.layout.fragment_employee_event_details); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this, new EmployeeEventDetailsViewModel.Factory()).get(EmployeeEventDetailsViewModel.class);
        view.findViewById(R.id.button_employee_event_details_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_employee_event_attachment).setOnClickListener(clicked -> openUrl(state.getDetails().getExternalLink()));
        view.findViewById(R.id.button_employee_event_evidence).setOnClickListener(clicked -> selectEvidence.launch(new String[]{"*/*"}));
        view.findViewById(R.id.button_employee_event_complete).setOnClickListener(clicked -> complete());
    }

    @Override public void onResume() {
        super.onResume();
        View view = requireView();
        view.findViewById(R.id.scroll_employee_event_details).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.skeleton_employee_event_details).setVisibility(View.VISIBLE);
        long id = requireArguments().getLong(EVENT_ID);
        if (id <= 0) { showFailure(FailureKind.INTERNAL); return; }
        // Toda origem envia o ID real; o backend devolve a participação da sessão atual.
        state.load(id, new EmployeeEventDetailsViewModel.ResultCallback() {
            @Override public void onSuccess() {
                if (getView() != view || !isResumed()) return;
                render(view);
                setSubmitting(view, false);
                view.findViewById(R.id.skeleton_employee_event_details).setVisibility(View.GONE);
                view.findViewById(R.id.scroll_employee_event_details).setVisibility(View.VISIBLE);
            }
            @Override public void onFailure(FailureKind failure, String message) {
                if (getView() == view && isResumed()) showFailure(failure);
            }
        });
    }

    private void render(View view) {
        CollaboratorEventDetailsData detail = state.getDetails();
        setText(view, R.id.text_employee_event_details_title, detail.getEventTitle());
        setText(view, R.id.text_employee_event_description, detail.getEventDescription());
        setText(view, R.id.text_employee_event_deadline, DATE.format(detail.getDeadline()));
        setText(view, R.id.text_employee_event_validity, formatValidity(detail.getComplianceValidity()));
        setText(view, R.id.text_employee_event_sender, detail.getManagerName());
        setText(view, R.id.text_employee_event_completion_mode, getString(
                detail.getCompletionMode() == CollaboratorEventDetailsData.CompletionMode.COLABORADOR
                        ? R.string.employee_event_self_declaration : R.string.employee_event_manager_completion));
        setText(view, R.id.text_employee_event_urgent_deadline,
                getString(R.string.employee_event_deadline_message, DATE.format(detail.getDeadline())));
        EventStatusClassifier.DisplayStatus status = state.getDisplayStatus();
        boolean urgent = status == EventStatusClassifier.DisplayStatus.URGENT;
        view.findViewById(R.id.image_employee_event_badge).setVisibility(urgent ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.container_employee_event_calendar).setVisibility(urgent ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_employee_event_urgent_deadline).setVisibility(urgent ? View.VISIBLE : View.GONE);
        ImageView icon = view.findViewById(R.id.image_employee_event_urgent);
        int label = R.string.employee_events_pending;
        int drawable = 0;
        boolean red = false;
        switch (status) {
            case URGENT: label = R.string.employee_events_urgent; drawable = R.drawable.ic_employee_event_warning; break;
            case ANALYSIS: label = R.string.employee_events_review; drawable = R.drawable.home_clock; break;
            case DONE: label = R.string.employee_events_done; drawable = R.drawable.ic_employee_event_done; break;
            case REJECTED: label = R.string.employee_events_rejected; red = true; break;
            case CANCELLED: label = R.string.employee_events_cancelled; red = true; break;
            case EXPIRED: label = R.string.employee_events_expired; red = true; break;
            default: break;
        }
        setText(view, R.id.text_employee_event_details_status, getString(label));
        icon.setImageTintList(red ? ColorStateList.valueOf(requireContext().getColor(R.color.employee_event_status_red)) : null);
        if (red) drawable = R.drawable.conversations_close;
        icon.setVisibility(drawable == 0 ? View.GONE : View.VISIBLE);
        if (drawable != 0) icon.setImageResource(drawable);
        view.findViewById(R.id.container_employee_event_material).setVisibility(
                detail.getExternalLink() == null || detail.getExternalLink().trim().isEmpty() ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_employee_event_completion).setVisibility(state.canComplete() ? View.VISIBLE : View.GONE);
        renderEvidence(view);
    }

    private void renderEvidence(View view) {
        LinearLayout list = view.findViewById(R.id.list_employee_event_evidences);
        list.removeAllViews();
        for (CollaboratorEventDetailsData.Evidence evidence : state.getDetails().getEvidences()) {
            if (evidence == null) continue;
            View row = getLayoutInflater().inflate(R.layout.item_employee_event_evidence, list, false);
            ((TextView) row.findViewById(R.id.button_employee_event_file)).setText(evidence.getOriginalName());
            row.findViewById(R.id.button_employee_event_file).setOnClickListener(clicked -> openUrl(evidence.getUrl()));
            row.findViewById(R.id.button_employee_event_remove_file).setVisibility(View.GONE);
            list.addView(row);
        }
        LinearLayout selected = view.findViewById(R.id.list_employee_event_selected_files);
        selected.removeAllViews();
        for (EventEvidenceFile file : state.getFiles()) {
            View row = getLayoutInflater().inflate(R.layout.item_employee_event_evidence, selected, false);
            ((TextView) row.findViewById(R.id.button_employee_event_file)).setText(file.getName());
            row.findViewById(R.id.button_employee_event_file).setOnClickListener(clicked -> {
                Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(file.getUri(),
                        requireContext().getContentResolver().getType(file.getUri())).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try { startActivity(intent); } catch (ActivityNotFoundException error) { showUrlError(); }
            });
            row.findViewById(R.id.button_employee_event_remove_file).setOnClickListener(clicked -> {
                state.removeFile(file);
                renderEvidence(view);
            });
            selected.addView(row);
        }
    }

    private void complete() {
        if (state.isLoading() || !state.canComplete()) return;
        if (state.getDetails().isEvidenceMandatory() && state.getFiles().isEmpty()) {
            showEvidenceError(getString(R.string.employee_event_evidence_required));
            return;
        }
        View view = requireView();
        setSubmitting(view, true);
        showEvidenceError(null);
        // Só o HTTP 201 abre a confirmação; não marca conclusão definitiva no cliente.
        state.complete(requireContext().getContentResolver(), new EmployeeEventDetailsViewModel.ResultCallback() {
            @Override public void onSuccess() {
                if (getView() != view || !isResumed()) return;
                NavHostFragment.findNavController(EmployeeEventDetailsFragment.this).navigate(
                        R.id.action_employee_event_details_to_confirmation, AuthArgs.copy(getArguments()));
            }
            @Override public void onFailure(FailureKind failure, String message) {
                if (getView() != view || !isResumed()) return;
                setSubmitting(view, false);
                if (failure == FailureKind.BUSINESS) showEvidenceError(message == null ? getString(R.string.employee_event_submit_error) : message);
                else showFailure(failure);
            }
        });
    }

    private void setSubmitting(View view, boolean submitting) {
        TextView button = view.findViewById(R.id.button_employee_event_complete);
        button.setEnabled(!submitting);
        button.setText(submitting ? R.string.employee_event_sending : R.string.employee_event_complete);
        view.findViewById(R.id.button_employee_event_evidence).setEnabled(!submitting);
        view.findViewById(R.id.list_employee_event_selected_files).setVisibility(submitting ? View.GONE : View.VISIBLE);
    }

    private void showEvidenceError(String message) {
        TextView error = requireView().findViewById(R.id.text_employee_event_evidence_error);
        error.setText(message);
        error.setVisibility(message == null ? View.GONE : View.VISIBLE);
    }
    private void showFailure(FailureKind failure) {
        if (failure == FailureKind.SESSION) SessionNavigation.signOut(this);
        else NavHostFragment.findNavController(this).navigate(
                failure == FailureKind.CONNECTION ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
    }
    private void openUrl(String url) {
        if (url == null) { showUrlError(); return; }
        Uri uri = Uri.parse(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) { showUrlError(); return; }
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException error) { showUrlError(); }
    }
    private void showUrlError() { Toast.makeText(requireContext(), R.string.employee_event_open_error, Toast.LENGTH_SHORT).show(); }
    private void setText(View view, int id, String text) { ((TextView) view.findViewById(id)).setText(text == null ? "—" : text); }
    private String formatValidity(String value) {
        if (value == null) return getString(R.string.employee_event_no_validity);
        try { return DATE.format(LocalDate.parse(value)); }
        catch (DateTimeParseException error) { return getString(R.string.employee_event_no_validity); }
    }
    @Override public void onPause() { state.cancel(); super.onPause(); }
    @Override public void onDestroyView() { state.cancel(); super.onDestroyView(); }
}
