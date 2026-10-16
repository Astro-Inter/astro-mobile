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
import com.example.astro_mobile.shared.navigation.HomeNavigation;

public final class FormDetailsFragment extends Fragment {
    public static final String FORM_TITLE = "form_title";
    public static final String DEADLINE = "form_deadline";
    public static final String LOCATION_MATCHES_UNIT = "form_location_matches_unit";
    public static final String REQUIRES_UNIT_LOCATION = "form_requires_unit_location";

    private FormDetailsViewModel state;
    private boolean locationMatchesUnit;
    private boolean requiresUnitLocation;
    private Runnable finishLoading;
    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (getView() == null) return;
                state.onPermissionResult(hasPreciseLocation());
                renderConsent(requireView());
            });

    public FormDetailsFragment() { super(R.layout.fragment_form_details); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this).get(FormDetailsViewModel.class);
        Bundle args = requireArguments();
        locationMatchesUnit = args.getBoolean(LOCATION_MATCHES_UNIT);
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
        view.findViewById(R.id.button_form_details_answer).setOnClickListener(clicked -> {
            // Confere a permissão novamente antes de liberar o próximo passo do protótipo.
            if (!hasPreciseLocation()) {
                state.onPermissionResult(false);
                renderConsent(view);
                return;
            }
            Toast.makeText(requireContext(), R.string.form_details_answer_pending, Toast.LENGTH_SHORT).show();
        });
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

    private void onConsentClick(CheckBox checkbox) {
        // O toque sozinho não marca o checkbox: primeiro é preciso confirmar a permissão precisa.
        checkbox.setChecked(false);
        if (state.isConsentAccepted()) {
            state.clearConsent();
        } else if (hasPreciseLocation()) {
            state.onPermissionResult(true);
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

    private void renderConsent(View view) {
        boolean preciseGranted = hasPreciseLocation();
        if (state.isConsentAccepted() && !preciseGranted) state.onPermissionResult(false);
        renderLocationMessage(view, preciseGranted);
        boolean addressAllowed = !requiresUnitLocation || locationMatchesUnit;
        view.findViewById(R.id.container_form_details_consent).setVisibility(
                !preciseGranted || addressAllowed ? View.VISIBLE : View.GONE);
        ((CheckBox) view.findViewById(R.id.checkbox_form_location)).setChecked(state.isConsentAccepted());
        boolean denied = state.hasPermissionError();
        view.findViewById(R.id.text_form_location_permission_error).setVisibility(denied ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.button_form_location_settings).setVisibility(denied ? View.VISIBLE : View.GONE);
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
            icon = R.drawable.ic_employee_event_warning;
            background = R.drawable.bg_profile_panel;
        } else if (requiresUnitLocation && !locationMatchesUnit) {
            text = R.string.form_details_outside_unit;
            icon = R.drawable.conversations_close;
            background = R.drawable.bg_form_location_error;
        } else {
            // Endereço correto e formulário sem endereço obrigatório usam os dois avisos positivos.
            text = requiresUnitLocation ? R.string.form_details_inside_unit : R.string.form_details_no_specific_address;
            icon = R.drawable.form_location_check;
            background = R.drawable.bg_home_positive_message;
        }
        view.findViewById(R.id.container_form_details_location_message).setBackgroundResource(background);
        ((TextView) view.findViewById(R.id.text_form_details_location_status)).setText(text);
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
            if (state.hasPermissionError() && hasPreciseLocation()) state.onPermissionResult(true);
            renderConsent(requireView());
        }
    }

    @Override
    public void onDestroyView() {
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
