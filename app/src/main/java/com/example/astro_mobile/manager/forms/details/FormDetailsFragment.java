package com.example.astro_mobile.manager.forms.details;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.ReceivedFormDetailsRepository;
import com.example.astro_mobile.data.api.dto.ReceivedFormDetails;
import com.example.astro_mobile.data.location.FormLocationRepository;
import com.example.astro_mobile.manager.forms.answer.FormAnswerViewModel;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class FormDetailsFragment extends Fragment {
    public static final String FORM_ID = "form_id";
    public static final String FORM_TITLE = "form_title";
    public static final String DEADLINE = "form_deadline";
    public static final String REQUIRES_UNIT_LOCATION = "form_requires_unit_location";

    private FormDetailsViewModel state;
    private FormAnswerViewModel answer;
    private boolean requiresUnitLocation;
    private Runnable finishLoading;
    private String formId;
    private boolean missingCoordinates;
    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (getView() == null) return;
                state.onPermissionResult(hasPreciseLocation());
                renderConsent(requireView());
                if (hasPreciseLocation()) checkLocation(false);
            });

    public FormDetailsFragment() { super(R.layout.fragment_form_details); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this, new FormDetailsViewModel.Factory()).get(FormDetailsViewModel.class);
        answer = new ViewModelProvider(NavHostFragment.findNavController(this)
                .getBackStackEntry(R.id.formDetailsFragment), FormAnswerViewModel.factory(requireContext()))
                .get(FormAnswerViewModel.class);
        Bundle args = requireArguments();
        formId = args.getString(FORM_ID);
        requiresUnitLocation = args.getBoolean(REQUIRES_UNIT_LOCATION, true);
        ((TextView) view.findViewById(R.id.text_form_details_title)).setText(args.getString(FORM_TITLE));
        ((TextView) view.findViewById(R.id.text_form_details_deadline)).setText(args.getString(DEADLINE));

        view.findViewById(R.id.button_form_details_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).navigateUp());
        view.findViewById(R.id.button_form_details_close).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(HomeNavigation.destination(requireContext()), false));

        ((TextView) view.findViewById(R.id.text_form_details_location_explanation)).setText(requiresUnitLocation
                ? R.string.form_details_location_explanation : R.string.form_details_anywhere_explanation);

        view.findViewById(R.id.checkbox_form_location).setOnClickListener(clicked -> onConsentClick((CheckBox) clicked));
        view.findViewById(R.id.button_form_location_settings).setOnClickListener(clicked -> openLocationSettings());
        view.findViewById(R.id.button_form_details_location_retry).setOnClickListener(clicked -> {
            if (answer.getFailure() == FormLocationRepository.Failure.LOCATION_DISABLED) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else checkLocation(false);
        });
        view.findViewById(R.id.button_form_details_answer).setOnClickListener(clicked -> {
            if (formId != null && !requiresUnitLocation) {
                showAnswerIntegrationPending();
                return;
            }
            // Revalida a posição no toque: uma leitura antiga não autoriza iniciar fora do raio.
            if (!hasPreciseLocation()) {
                state.onPermissionResult(false);
                renderConsent(view);
                return;
            }
            if (state.isConsentAccepted()) checkLocation(true);
        });
        if (formId != null) {
            setApiLoading(view, true);
            return;
        }
        renderConsent(view);

        // O XML mostra o conteúdo real; em execução o skeleton simula a espera de 1s.
        if (!state.isLoaded()) {
            View content = view.findViewById(R.id.scroll_form_details);
            View skeleton = view.findViewById(R.id.skeleton_form_details);
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

    private void setApiLoading(View view, boolean loading) {
        view.findViewById(R.id.scroll_form_details).setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        view.findViewById(R.id.skeleton_form_details).setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void loadDetails() {
        View view = getView();
        if (view == null || state.isLoading()) return;
        setApiLoading(view, true);
        state.load(formId, new ReceivedFormDetailsRepository.ResultCallback() {
            @Override public void onSuccess(ReceivedFormDetails details) {
                if (getView() != view || !isResumed()) return;
                String deadline;
                try {
                    deadline = details.form.dueDate == null ? getString(R.string.form_details_unavailable)
                            : LocalDate.parse(details.form.dueDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } catch (java.time.DateTimeException error) {
                    onFailure(FailureKind.INTERNAL);
                    return;
                }
                requiresUnitLocation = details.form.unitId != null;
                ((TextView) view.findViewById(R.id.text_form_details_location_explanation)).setText(requiresUnitLocation
                        ? R.string.form_details_location_explanation : R.string.form_details_anywhere_explanation);
                ReceivedFormDetails.UnitLocation location = details.form.unitLocation;
                missingCoordinates = requiresUnitLocation && (location == null || !location.isValid());
                answer.setUnitReference(requiresUnitLocation,
                        missingCoordinates || location == null ? null : location.latitude,
                        missingCoordinates || location == null ? null : location.longitude);
                ((TextView) view.findViewById(R.id.text_form_details_title)).setText(details.form.title);
                ((TextView) view.findViewById(R.id.text_form_details_description)).setText(details.form.description);
                view.findViewById(R.id.text_form_details_description).setVisibility(
                        details.form.description == null || details.form.description.isEmpty() ? View.GONE : View.VISIBLE);
                ((TextView) view.findViewById(R.id.text_form_details_deadline)).setText(deadline);
                ((TextView) view.findViewById(R.id.text_form_details_sender)).setText(
                        details.senderName == null ? getString(R.string.form_details_unavailable) : details.senderName);
                ((TextView) view.findViewById(R.id.text_form_details_unit)).setText(details.unitName == null
                        ? getString(requiresUnitLocation ? R.string.form_details_unavailable : R.string.form_details_without_unit)
                        : details.unitName);
                TextView review = view.findViewById(R.id.text_form_details_review);
                boolean completed = "CONCLUIDO".equals(details.form.status);
                review.setVisibility(completed ? View.VISIBLE : View.GONE);
                if (completed) {
                    String status = details.review == null ? "" : details.review.status;
                    int label = "approved".equals(status) ? R.string.form_details_approved
                            : "rejected".equals(status) ? R.string.form_details_rejected : R.string.form_details_analysis;
                    String reason = details.review == null ? null : details.review.description;
                    review.setText(reason == null || reason.isEmpty() ? getString(label)
                            : getString(R.string.form_details_review_reason, getString(label), reason));
                }
                setApiLoading(view, false);
                renderConsent(view);
                if (!completed && requiresUnitLocation && !missingCoordinates && hasPreciseLocation()) checkLocation(false);
            }
            @Override public void onFailure(FailureKind failure) {
                if (getView() != view || !isResumed()) return;
                state.cancel();
                if (failure == FailureKind.SESSION) SessionNavigation.signOut(FormDetailsFragment.this);
                else NavHostFragment.findNavController(FormDetailsFragment.this).navigate(failure == FailureKind.CONNECTION
                        ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
            }
        });
    }

    private void showAnswerIntegrationPending() {
        // O próximo card integra as perguntas: não conclui um formulário real usando as respostas do mock.
        Toast.makeText(requireContext(), R.string.form_details_answer_integration_pending, Toast.LENGTH_LONG).show();
    }

    private void onConsentClick(CheckBox checkbox) {
        // O toque sozinho não marca o checkbox: primeiro é preciso confirmar a permissão precisa.
        checkbox.setChecked(false);
        if (state.isConsentAccepted()) {
            state.clearConsent();
        } else if (hasPreciseLocation()) {
            state.onPermissionResult(true);
            checkLocation(false);
        } else if (state.hasPermissionError()
                && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Após o bloqueio do Android, o novo toque leva às configurações em vez de parecer sem ação.
            openLocationSettings();
        } else {
            // Uma recusa não desabilita o checkbox: o usuário pode solicitar novamente.
            locationPermission.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
        renderConsent(requireView());
    }

    private void openLocationSettings() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + requireContext().getPackageName())));
    }

    private boolean hasPreciseLocation() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void checkLocation(boolean openAnswer) {
        if (formId != null && (state.getData() == null || missingCoordinates || !requiresUnitLocation
                || "CONCLUIDO".equals(state.getData().form.status))) return;
        answer.requestLocation(false, requiresUnitLocation, () -> {
            if (getView() == null || !isResumed()) return;
            renderConsent(requireView());
            if (openAnswer && state.isConsentAccepted() && hasPreciseLocation() && answer.canStart()) {
                if (formId != null) showAnswerIntegrationPending();
                else NavHostFragment.findNavController(this).navigate(R.id.action_form_details_to_answer, getArguments());
            }
        });
        renderConsent(requireView());
    }

    private void renderConsent(View view) {
        if (formId != null) {
            if (state.getData() == null) return;
            boolean completed = "CONCLUIDO".equals(state.getData().form.status);
            view.findViewById(R.id.container_form_details_consent).setVisibility(
                    completed || !requiresUnitLocation ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.button_form_details_answer).setVisibility(completed ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.container_form_details_location_message).setVisibility(completed ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.container_form_details_location_notice).setVisibility(completed || !requiresUnitLocation ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.button_form_details_location_retry).setVisibility(View.GONE);
            if (completed) return;
            if (missingCoordinates || !requiresUnitLocation) {
                showLocationMessage(view,
                        missingCoordinates ? R.string.form_details_unit_location_unavailable : R.string.form_details_no_specific_address,
                        missingCoordinates ? R.drawable.conversations_close : R.drawable.form_location_check,
                        missingCoordinates ? R.drawable.bg_form_location_error : R.drawable.bg_home_positive_message);
                view.findViewById(R.id.button_form_details_answer).setEnabled(!missingCoordinates);
                view.findViewById(R.id.container_form_details_consent).setVisibility(View.GONE);
                TextView retry = view.findViewById(R.id.button_form_details_location_retry);
                retry.setVisibility(missingCoordinates ? View.VISIBLE : View.GONE);
                retry.setText(R.string.form_details_reload);
                retry.setOnClickListener(clicked -> loadDetails());
                return;
            }
        }
        boolean preciseGranted = hasPreciseLocation();
        if (state.isConsentAccepted() && !preciseGranted) state.onPermissionResult(false);
        renderLocationMessage(view, preciseGranted);
        boolean addressAllowed = answer.canStart();
        view.findViewById(R.id.container_form_details_consent).setVisibility(
                !preciseGranted || !answer.isOutside() ? View.VISIBLE : View.GONE);
        ((CheckBox) view.findViewById(R.id.checkbox_form_location)).setChecked(state.isConsentAccepted());
        boolean denied = state.hasPermissionError();
        view.findViewById(R.id.text_form_location_permission_error).setVisibility(denied ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.button_form_location_settings).setVisibility(denied ? View.VISIBLE : View.GONE);
        TextView retry = view.findViewById(R.id.button_form_details_location_retry);
        retry.setVisibility(preciseGranted && !answer.isBusy() && !addressAllowed && !answer.isCompleted()
                ? View.VISIBLE : View.GONE);
        retry.setText(answer.getFailure() == FormLocationRepository.Failure.LOCATION_DISABLED
                ? R.string.form_answer_location_settings : R.string.form_details_check_location);
        view.findViewById(R.id.button_form_details_answer).setEnabled(
                addressAllowed && state.isConsentAccepted() && preciseGranted);
    }

    private void renderLocationMessage(View view, boolean preciseGranted) {
        int text;
        int icon;
        int background;
        // A permissão vem antes do cenário de endereço: sem ela, não exibimos uma confirmação verde.
        if (!preciseGranted) {
            text = R.string.form_details_precise_location_required;
            icon = R.drawable.conversations_close;
            background = R.drawable.bg_form_location_error;
        } else if (answer.isCompleted()) {
            text = R.string.form_answer_completed;
            icon = R.drawable.form_location_check;
            background = R.drawable.bg_home_positive_message;
        } else if (answer.isBusy() || (!answer.canStart() && !answer.isOutside() && answer.getFailure() == null)) {
            text = R.string.form_answer_locating;
            icon = R.drawable.ic_employee_event_warning;
            background = R.drawable.bg_profile_panel;
        } else if (answer.getFailure() != null) {
            text = answer.getFailure() == FormLocationRepository.Failure.LOCATION_DISABLED
                    ? R.string.form_answer_location_disabled : R.string.form_answer_location_unavailable;
            icon = R.drawable.conversations_close;
            background = R.drawable.bg_form_location_error;
        } else if (answer.isOutside()) {
            text = R.string.form_details_outside_unit;
            icon = R.drawable.conversations_close;
            background = R.drawable.bg_form_location_error;
        } else {
            // Endereço correto e formulário sem endereço obrigatório usam os dois avisos positivos.
            text = requiresUnitLocation ? R.string.form_details_inside_unit : R.string.form_details_no_specific_address;
            icon = R.drawable.form_location_check;
            background = R.drawable.bg_home_positive_message;
        }
        showLocationMessage(view, text, icon, background);
        TextView statusText = view.findViewById(R.id.text_form_details_location_status);
        if (formId != null && text == R.string.form_details_outside_unit) {
            String unit = state.getData().unitName;
            statusText.setText(getString(R.string.form_details_outside_real_unit,
                    unit == null ? getString(R.string.form_details_unit_label) : unit));
        }
    }

    private void showLocationMessage(View view, int text, int icon, int background) {
        // Usa as variantes do Figma e reaplica texto/ícone claros ao trocar de estado.
        view.findViewById(R.id.container_form_details_location_message).setBackgroundResource(background);
        TextView message = view.findViewById(R.id.text_form_details_location_status);
        message.setText(text);
        message.setTextColor(ContextCompat.getColor(requireContext(), R.color.astro_light_background));
        ImageView statusIcon = view.findViewById(R.id.image_form_details_location_status);
        statusIcon.setImageResource(icon);
        statusIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.astro_light_background)));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Atualiza a tentativa ao voltar das configurações; revogar a permissão também bloqueia o botão.
        if (state != null) {
            if (formId != null) {
                if (state.hasPermissionError() && hasPreciseLocation()) state.onPermissionResult(true);
                loadDetails();
                return;
            }
            if (state.hasPermissionError() && hasPreciseLocation()) state.onPermissionResult(true);
            renderConsent(requireView());
            if (hasPreciseLocation()) checkLocation(false);
        }
    }

    @Override public void onPause() {
        if (answer != null) answer.pauseLocation();
        if (state != null) state.cancel();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
