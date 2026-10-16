package com.example.astro_mobile.manager.forms.answer;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
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
import com.example.astro_mobile.data.location.FormLocationRepository;
import com.example.astro_mobile.manager.forms.details.FormDetailsFragment;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class FormAnswerFragment extends Fragment {
    private final int[] groupIds = {R.id.group_form_answer_helmets, R.id.group_form_answer_boots,
            R.id.group_form_answer_gloves};
    private FormAnswerViewModel state;
    private boolean requiresUnitLocation;
    private Runnable finishLoading;
    private final ActivityResultLauncher<Uri> camera = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), captured -> {
                state.onPhotoResult(captured);
                if (getView() == null) return;
                renderEvidences(requireView());
                render(requireView());
            });
    private final Runnable locationTick = new Runnable() {
        @Override public void run() {
            if (getView() == null || !isResumed() || state.isCompleted() || state.isOutside()) return;
            requestLocation(false);
            // O intervalo existe somente enquanto este formulário permanece em primeiro plano.
            if (!state.isCompleted() && !state.isOutside()) requireView().postDelayed(this, 60_000L);
        }
    };

    public FormAnswerFragment() { super(R.layout.fragment_form_answer); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Detalhes e resposta compartilham a tentativa enquanto os detalhes estiverem na pilha.
        state = new ViewModelProvider(NavHostFragment.findNavController(this)
                .getBackStackEntry(R.id.formDetailsFragment), FormAnswerViewModel.factory(requireContext()))
                .get(FormAnswerViewModel.class);
        Bundle args = requireArguments();
        requiresUnitLocation = args.getBoolean(FormDetailsFragment.REQUIRES_UNIT_LOCATION, true);
        ((TextView) view.findViewById(R.id.text_form_answer_title))
                .setText(args.getString(FormDetailsFragment.FORM_TITLE));
        view.findViewById(R.id.button_form_answer_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).navigateUp());
        view.findViewById(R.id.button_form_answer_close).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(HomeNavigation.destination(requireContext()), false));

        // Cada pergunta admite apenas uma opção; as respostas ficam no ViewModel ao pausar a tela.
        for (int index = 0; index < groupIds.length; index++) {
            int question = index;
            RadioGroup group = view.findViewById(groupIds[index]);
            if (state.choice(index) != -1) group.check(state.choice(index) == 0
                    ? R.id.radio_form_answer_ok : R.id.radio_form_answer_problematic);
            group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
                if (checkedId == -1) return;
                state.setChoice(question, checkedId == R.id.radio_form_answer_ok ? 0 : 1);
                render(view);
            });
        }
        EditText risks = view.findViewById(R.id.input_form_answer_risks);
        risks.setText(state.getRisks());
        risks.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                state.setRisks(text.toString());
                render(view);
            }
            @Override public void afterTextChanged(Editable text) { }
        });
        view.findViewById(R.id.button_form_answer_date).setOnClickListener(clicked -> showDatePicker());
        view.findViewById(R.id.button_form_answer_attach).setOnClickListener(clicked -> openCamera());
        view.findViewById(R.id.button_form_answer_location_retry).setOnClickListener(clicked -> retryLocation());
        view.findViewById(R.id.button_form_answer_complete).setOnClickListener(clicked -> {
            if (state.canSubmit()) requestLocation(true);
        });
        renderEvidences(view);
        render(view);

        // O preview mostra a tela; o atraso de 1s é apenas o loading mock dos futuros dados da API.
        if (!state.isLoaded()) {
            view.findViewById(R.id.scroll_form_answer).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.skeleton_form_answer).setVisibility(View.VISIBLE);
            finishLoading = () -> {
                state.markLoaded();
                view.findViewById(R.id.skeleton_form_answer).setVisibility(View.GONE);
                view.findViewById(R.id.scroll_form_answer).setVisibility(View.VISIBLE);
                finishLoading = null;
            };
            view.postDelayed(finishLoading, 1_000L);
        }
    }

    private void openCamera() {
        try {
            Uri photo = state.preparePhoto();
            if (photo != null) camera.launch(photo);
        } catch (IOException | ActivityNotFoundException | SecurityException error) {
            state.onPhotoResult(false);
            Toast.makeText(requireContext(), R.string.form_answer_camera_error, Toast.LENGTH_LONG).show();
        }
    }

    private void showDatePicker() {
        Calendar date = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            date.set(year, month, day);
            state.setDate(new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).format(date.getTime()));
            render(requireView());
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void requestLocation(boolean finish) {
        state.requestLocation(finish, requiresUnitLocation, () -> {
            if (getView() == null || !isResumed()) return;
            render(requireView());
            if (state.isOutside()) {
                // Sai da resposta sem descartá-la; após o aviso, os detalhes verificam a localização novamente.
                NavHostFragment.findNavController(this).navigate(R.id.action_form_answer_to_outside);
                return;
            }
            if (state.isCompleted()) requireView().removeCallbacks(locationTick);
            if (state.isCompleted()) Toast.makeText(requireContext(), R.string.form_answer_completed, Toast.LENGTH_LONG).show();
        });
        render(requireView());
    }

    private void retryLocation() {
        FormLocationRepository.Failure failure = state.getFailure();
        if (failure == FormLocationRepository.Failure.PERMISSION_REQUIRED) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + requireContext().getPackageName())));
        } else if (failure == FormLocationRepository.Failure.LOCATION_DISABLED) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } else requestLocation(false);
    }

    private void render(View view) {
        boolean editable = !state.isCompleted() && !state.isOutside();
        for (int groupId : groupIds) {
            RadioGroup group = view.findViewById(groupId);
            for (int index = 0; index < group.getChildCount(); index++) group.getChildAt(index).setEnabled(editable);
        }
        view.findViewById(R.id.input_form_answer_risks).setEnabled(editable);
        TextView date = view.findViewById(R.id.button_form_answer_date);
        date.setText(state.getDate());
        date.setEnabled(editable);
        view.findViewById(R.id.button_form_answer_attach).setEnabled(editable
                && state.evidenceCount() < FormAnswerViewModel.MAX_EVIDENCES);
        LinearLayout evidences = view.findViewById(R.id.list_form_answer_evidences);
        for (int index = 0; index < evidences.getChildCount(); index++) {
            evidences.getChildAt(index).findViewById(R.id.button_form_answer_remove_evidence).setEnabled(editable);
        }
        ((TextView) view.findViewById(R.id.text_form_answer_evidence_count))
                .setText(getString(R.string.form_answer_evidence_count, state.evidenceCount()));
        TextView complete = view.findViewById(R.id.button_form_answer_complete);
        complete.setEnabled(state.canSubmit());
        complete.setText(state.isCompleted() ? R.string.form_answer_completed_button : R.string.form_answer_complete);
        renderLocation(view);
    }

    private void renderEvidences(View view) {
        LinearLayout list = view.findViewById(R.id.list_form_answer_evidences);
        list.removeAllViews();
        for (int index = 0; index < state.evidenceCount(); index++) {
            Uri photo = state.evidenceAt(index);
            View item = getLayoutInflater().inflate(R.layout.item_form_answer_evidence, list, false);
            ((TextView) item.findViewById(R.id.text_form_answer_evidence))
                    .setText(getString(R.string.form_answer_evidence_name, index + 1));
            ImageView preview = item.findViewById(R.id.image_form_answer_evidence);
            try {
                preview.setImageBitmap(state.evidencePreview(photo));
            } catch (IOException error) {
                preview.setVisibility(View.GONE);
            }
            item.findViewById(R.id.button_form_answer_remove_evidence).setOnClickListener(clicked -> {
                state.removeEvidence(photo);
                renderEvidences(view);
                render(view);
            });
            list.addView(item);
        }
    }

    private void renderLocation(View view) {
        TextView message = view.findViewById(R.id.text_form_answer_location);
        TextView retry = view.findViewById(R.id.button_form_answer_location_retry);
        FormLocationRepository.Failure failure = state.getFailure();
        // Sem painel de depuração: só mostra uma falha ou a conclusão local.
        view.findViewById(R.id.container_form_answer_location).setVisibility(
                failure != null || state.isCompleted() ? View.VISIBLE : View.GONE);
        boolean error = failure != null;
        message.setTextColor(ContextCompat.getColor(requireContext(), error ? R.color.astro_error : R.color.astro_light_background));
        view.findViewById(R.id.container_form_answer_location).setBackgroundResource(state.isCompleted()
                ? R.drawable.bg_home_positive_message : R.drawable.bg_profile_panel);
        retry.setVisibility(failure != null && !state.isBusy() ? View.VISIBLE : View.GONE);
        retry.setText(failure == FormLocationRepository.Failure.UNAVAILABLE
                ? R.string.form_answer_location_retry : R.string.form_answer_location_settings);
        if (state.isCompleted()) message.setText(R.string.form_answer_completed);
        else if (failure == FormLocationRepository.Failure.PERMISSION_REQUIRED) message.setText(R.string.form_answer_location_permission);
        else if (failure == FormLocationRepository.Failure.LOCATION_DISABLED) message.setText(R.string.form_answer_location_disabled);
        else if (failure != null) message.setText(R.string.form_answer_location_unavailable);
    }

    @Override public void onResume() {
        super.onResume();
        if (state != null) locationTick.run();
    }

    @Override public void onPause() {
        if (getView() != null) getView().removeCallbacks(locationTick);
        if (state != null) state.pauseLocation();
        super.onPause();
    }

    @Override public void onDestroyView() {
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
