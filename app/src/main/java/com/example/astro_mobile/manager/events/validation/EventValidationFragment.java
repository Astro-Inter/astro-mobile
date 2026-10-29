package com.example.astro_mobile.manager.events.validation;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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
import com.example.astro_mobile.data.api.dto.EventValidationData;
import com.example.astro_mobile.shared.navigation.HomeNavigation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class EventValidationFragment extends Fragment {
    private Dialog rejectDialog;
    private EventValidationViewModel state;
    public EventValidationFragment() { super(R.layout.fragment_event_validation); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this).get(EventValidationViewModel.class);
        view.findViewById(R.id.button_validation_back).setOnClickListener(clicked -> NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_validation_event).setOnClickListener(clicked -> NavHostFragment.findNavController(this).popBackStack(R.id.managerEventDetailsFragment, false));
        view.findViewById(R.id.button_validation_close).setOnClickListener(clicked -> NavHostFragment.findNavController(this).popBackStack(HomeNavigation.destination(requireContext()), false));
        view.findViewById(R.id.button_validation_confirm).setOnClickListener(clicked -> decide(view, false));
        view.findViewById(R.id.button_validation_reject).setOnClickListener(clicked -> showRejection(view));
        if (state.getDetails() != null) render(view);
        else {
            String retry = NavHostFragment.findNavController(this).getCurrentBackStackEntry().getSavedStateHandle().get("retry_event_validation");
            if (retry == null) load(view);
        }
    }

    // Substitui os exemplos da tela pelos dados e arquivos enviados.
    private void load(View view) {
        view.findViewById(R.id.container_validation_content).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.skeleton_event_validation).setVisibility(View.VISIBLE);
        state.load(requireArguments().getLong("event_id"), requireArguments().getLong("participant_id"), new EventValidationViewModel.Callback() {
            @Override public void onSuccess() { if (getView() == view && isAdded()) render(view); }
            @Override public void onFailure(FailureKind failure) { if (getView() == view && isAdded()) showFailure(failure, "load"); }
        });
    }
    private void render(View view) {
        EventValidationData data = state.getDetails();
        setValue(view, R.id.text_validation_participant, data.participantName);
        setValue(view, R.id.text_validation_event, data.eventTitle);
        setValue(view, R.id.text_validation_role, data.roleName);
        setValue(view, R.id.text_validation_unit, data.unitName);
        setValue(view, R.id.text_validation_completion, formatDate(data.completionDate, "dd/MM/yyyy 'às' HH:mm"));
        setValue(view, R.id.text_validation_deadline, formatDate(data.endDate, "dd/MM/yyyy"));
        setValue(view, R.id.text_validation_evidence_required, data.evidenceMandatory == null ? null
                : getString(data.evidenceMandatory ? R.string.event_validation_evidence_yes : R.string.event_validation_not_required));
        Glide.with(view).load(data.profilePhotoUrl).circleCrop().placeholder(R.drawable.profile_avatar_placeholder)
                .fallback(R.drawable.profile_avatar_placeholder).error(R.drawable.profile_avatar_placeholder)
                .into((ImageView) view.findViewById(R.id.image_validation_participant));
        LinearLayout list = view.findViewById(R.id.list_validation_evidence);
        list.removeAllViews();
        if (data.evidences != null) for (EventValidationData.Evidence evidence : data.evidences) {
            if (evidence == null) continue;
            View row = getLayoutInflater().inflate(R.layout.item_validation_evidence, list, false);
            TextView name = row.findViewById(R.id.text_validation_evidence_name);
            name.setText(evidence.originalName == null ? getString(R.string.staff_detail_value_unavailable) : evidence.originalName);
            name.setPaintFlags(name.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            row.setOnClickListener(clicked -> openEvidence(evidence));
            list.addView(row);
        }
        view.findViewById(R.id.text_validation_evidence_empty).setVisibility(list.getChildCount() == 0 ? View.VISIBLE : View.GONE);
        boolean pending = "PENDENTE".equals(data.status);
        view.findViewById(R.id.button_validation_confirm).setEnabled(pending);
        view.findViewById(R.id.button_validation_reject).setEnabled(pending);
        view.findViewById(R.id.skeleton_event_validation).setVisibility(View.GONE);
        view.findViewById(R.id.container_validation_content).setVisibility(View.VISIBLE);
    }
    private String formatDate(String value, String pattern) {
        if (value == null) return null;
        try { return LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern(pattern)); }
        catch (DateTimeParseException ignored) { return null; }
    }
    private void setValue(View view, int id, String value) {
        ((TextView) view.findViewById(id)).setText(value == null || value.trim().isEmpty()
                ? getString(R.string.staff_detail_value_unavailable) : value);
    }

    // Imagens abrem no Astro; demais formatos usam um aplicativo compatível.
    private void openEvidence(EventValidationData.Evidence evidence) {
        if (evidence.url == null) { showFileError(); return; }
        Uri uri = Uri.parse(evidence.url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) { showFileError(); return; }
        if (evidence.mimeType != null && evidence.mimeType.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) {
            java.util.ArrayList<String> images = new java.util.ArrayList<>();
            int selected = 0;
            for (EventValidationData.Evidence item : state.getDetails().evidences) {
                if (item != null && item.url != null && item.mimeType != null
                        && item.mimeType.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) {
                    if (item == evidence) selected = images.size();
                    images.add(item.url);
                }
            }
            Bundle args = new Bundle();
            args.putStringArrayList("evidence_images", images);
            args.putInt("evidence_selected", selected);
            NavHostFragment.findNavController(this).navigate(R.id.action_event_validation_to_evidence, args);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, evidence.mimeType == null ? "*/*" : evidence.mimeType);
            try { startActivity(intent); } catch (ActivityNotFoundException error) { showFileError(); }
        }
    }
    private void showFileError() { Toast.makeText(requireContext(), R.string.employee_event_open_error, Toast.LENGTH_SHORT).show(); }

    private void showRejection(View view) {
        if (rejectDialog != null && rejectDialog.isShowing()) return;
        rejectDialog = new Dialog(requireContext());
        rejectDialog.setContentView(R.layout.dialog_event_validation_reject);
        EditText reason = rejectDialog.findViewById(R.id.input_validation_rejection_reason);
        View submit = rejectDialog.findViewById(R.id.button_validation_rejection_submit);
        reason.setText(state.getReason());
        submit.setEnabled(!state.getReason().trim().isEmpty());
        reason.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                state.setReason(s.toString()); submit.setEnabled(!s.toString().trim().isEmpty() && !state.isLoading());
            }
            @Override public void afterTextChanged(Editable text) { }
        });
        rejectDialog.findViewById(R.id.button_validation_rejection_close).setOnClickListener(clicked -> { state.setReason(""); rejectDialog.dismiss(); });
        rejectDialog.setOnCancelListener(dialog -> state.setReason(""));
        submit.setOnClickListener(clicked -> decide(view, true));
        rejectDialog.show();
        Window window = rejectDialog.getWindow();
        if (window != null) {
            float density = getResources().getDisplayMetrics().density;
            int width = Math.min(getResources().getDisplayMetrics().widthPixels - Math.round(60 * density), Math.round(370 * density));
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.8f);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // Uma decisão só é confirmada depois da resposta bem-sucedida do servidor.
    private void decide(View view, boolean reject) {
        if (state.isLoading() || state.getDetails() == null || (reject && state.getReason().trim().isEmpty())) return;
        view.findViewById(R.id.button_validation_confirm).setEnabled(false);
        view.findViewById(R.id.button_validation_reject).setEnabled(false);
        if (rejectDialog != null && rejectDialog.isShowing()) {
            rejectDialog.setCancelable(false);
            rejectDialog.findViewById(R.id.button_validation_rejection_close).setEnabled(false);
            TextView submit = rejectDialog.findViewById(R.id.button_validation_rejection_submit);
            submit.setEnabled(false); submit.setText(R.string.event_validation_sending);
        } else ((TextView) view.findViewById(reject ? R.id.button_validation_reject
                : R.id.button_validation_confirm)).setText(R.string.event_validation_sending);
        state.decide(requireArguments().getLong("event_id"), requireArguments().getLong("participant_id"), reject, new EventValidationViewModel.Callback() {
            @Override public void onSuccess() {
                if (getView() != view || !isAdded()) return;
                if (rejectDialog != null) rejectDialog.dismiss();
                state.setReason("");
                NavController nav = NavHostFragment.findNavController(EventValidationFragment.this);
                nav.getBackStackEntry(R.id.eventParticipantsFragment).getSavedStateHandle().set("retry_event_participants", true);
                Bundle args = new Bundle(); args.putBoolean("validation_rejected", reject);
                nav.navigate(R.id.action_event_validation_to_confirmation, args);
            }
            @Override public void onFailure(FailureKind failure) {
                if (getView() != view || !isAdded()) return;
                if (rejectDialog != null) rejectDialog.dismiss();
                showFailure(failure, reject ? "reject" : "approve");
            }
        });
    }
    private void showFailure(FailureKind failure, String operation) {
        if (failure == FailureKind.SESSION) { SessionNavigation.signOut(this); return; }
        if (!"load".equals(operation)) NavHostFragment.findNavController(this)
                .getBackStackEntry(R.id.eventParticipantsFragment).getSavedStateHandle().set("retry_event_participants", true);
        Bundle args = new Bundle(); args.putString("retry_event_validation", operation);
        NavHostFragment.findNavController(this).navigate(failure == FailureKind.CONNECTION ? R.id.connectionErrorFragment : R.id.genericErrorFragment, args);
    }
    @Override public void onResume() {
        super.onResume();
        String retry = NavHostFragment.findNavController(this).getCurrentBackStackEntry().getSavedStateHandle().remove("retry_event_validation");
        if (getView() == null) return;
        if ("load".equals(retry)) load(getView());
        else if ("approve".equals(retry)) decide(getView(), false);
        else if ("reject".equals(retry)) decide(getView(), true);
        else if (state.getDetails() != null && !state.getReason().trim().isEmpty()) showRejection(getView());
    }
    @Override public void onDestroyView() {
        if (rejectDialog != null) rejectDialog.dismiss();
        rejectDialog = null; state.cancel(); super.onDestroyView();
    }
}
