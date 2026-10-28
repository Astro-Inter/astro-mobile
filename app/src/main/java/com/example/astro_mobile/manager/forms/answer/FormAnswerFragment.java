package com.example.astro_mobile.manager.forms.answer;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.FormResponseRepository;
import com.example.astro_mobile.data.api.dto.ReceivedFormDetails;
import com.example.astro_mobile.data.camera.FormEvidenceRepository;
import com.example.astro_mobile.data.location.FormLocationRepository;
import com.example.astro_mobile.shared.navigation.HomeNavigation;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public final class FormAnswerFragment extends Fragment {
    private FormAnswerViewModel state;
    private FormEvidenceRepository evidence;
    private final ActivityResultLauncher<Uri> camera = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), captured -> {
                if (state == null || getView() == null) return;
                if (!state.finishPhotoCapture(captured) && captured) {
                    Toast.makeText(requireContext(), R.string.form_answer_photo_invalid, Toast.LENGTH_LONG).show();
                }
                renderQuestions(requireView());
            });
    private final Runnable locationTick = new Runnable() {
        @Override public void run() {
            if (getView() == null || !isResumed() || state.isCompleted() || !state.requiresLocation()) return;
            checkLocation(false);
            // O acompanhamento ocorre só nesta tela, sem serviço ou histórico de GPS.
            if (getView() != null) requireView().postDelayed(this, 60_000L);
        }
    };
    public FormAnswerFragment() { super(R.layout.fragment_form_answer); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(NavHostFragment.findNavController(this)
                .getBackStackEntry(R.id.formDetailsFragment), FormAnswerViewModel.factory(requireContext()))
                .get(FormAnswerViewModel.class);
        evidence = new FormEvidenceRepository(requireContext());
        view.findViewById(R.id.button_form_answer_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).navigateUp());
        view.findViewById(R.id.button_form_answer_close).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(HomeNavigation.destination(requireContext()), false));
        view.findViewById(R.id.button_form_answer_location_retry).setOnClickListener(clicked -> retryLocation());
        view.findViewById(R.id.button_form_answer_check_submission).setOnClickListener(clicked -> refresh());
        view.findViewById(R.id.button_form_answer_complete).setOnClickListener(clicked -> {
            if (state.canSubmit()) checkLocation(true);
        });
        if (state.getDetails() != null) renderQuestions(view);
    }
    private void renderQuestions(View view) {
        ((TextView) view.findViewById(R.id.text_form_answer_title)).setText(state.getDetails().form.title);
        LinearLayout questions = view.findViewById(R.id.list_form_answer_questions);
        questions.removeAllViews();
        FormQuestionRenderer renderer = new FormQuestionRenderer(getLayoutInflater(), state,
                () -> renderControls(view), this::takePhoto, state::removePhoto,
                this::openSubmittedImage, evidence);
        if (state.getDetails().form.questions != null) {
            for (int index = 0; index < state.getDetails().form.questions.size(); index++)
                questions.addView(renderer.create(questions, state.getDetails().form.questions.get(index), index));
        }
        if (questions.getChildCount() == 0) showInline(getString(R.string.form_answer_no_questions));
        renderControls(view);
    }

    private void takePhoto(Long questionId) {
        ReceivedFormDetails.Question question = null;
        for (ReceivedFormDetails.Question item : state.getDetails().form.questions) {
            if (item.id.equals(questionId)) { question = item; break; }
        }
        if (question == null || !state.canAddPhoto(question)) return;
        Uri photo;
        try {
            photo = evidence.createCapture();
        } catch (java.io.IOException error) {
            Toast.makeText(requireContext(), R.string.form_answer_photo_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        state.beginPhotoCapture(questionId, photo);
        try {
            camera.launch(photo);
        } catch (ActivityNotFoundException | IllegalStateException error) {
            state.finishPhotoCapture(false);
            Toast.makeText(requireContext(), R.string.form_answer_photo_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void openSubmittedImage(Long questionId, String imageId) {
        ImageView image = new ImageView(requireContext());
        int size = (int) (320 * getResources().getDisplayMetrics().density);
        image.setLayoutParams(new ViewGroup.LayoutParams(size, size));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackgroundColor(Color.BLACK);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.form_answer_photo_title).setView(image)
                .setNegativeButton(android.R.string.cancel, null).create();
        dialog.show();
        state.getImageUrl(imageId, new FormResponseRepository.ImageUrlCallback() {
            @Override public void onSuccess(String url) {
                if (dialog.isShowing()) Glide.with(FormAnswerFragment.this).load(url).fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image).into(image);
            }
            @Override public void onFailure(FailureKind failure) {
                if (dialog.isShowing()) dialog.dismiss();
                if (getView() != null && isResumed()) showFailure(failure);
            }
        });
    }
    private void refresh() {
        View view = requireView();
        view.removeCallbacks(locationTick);
        setLoading(view, true);
        // Ao voltar de um envio incerto, confirma o estado antes de habilitar outro POST.
        state.refresh(new FormResponseRepository.ResultCallback() {
            @Override public void onSuccess(ReceivedFormDetails data) {
                if (getView() != view || !isResumed()) return;
                setLoading(view, false);
                showInline(null);
                renderQuestions(view);
                if (!state.isCompleted()) locationTick.run();
            }
            @Override public void onFailure(FailureKind failure, String message, boolean uncertain) {
                if (getView() == view && isResumed()) showFailure(failure);
            }
            @Override public void onNotSubmitted() {
                if (getView() != view || !isResumed()) return;
                setLoading(view, false);
                showInline(getString(R.string.form_answer_submission_not_found));
                renderControls(view);
            }
        });
    }
    private void checkLocation(boolean submit) {
        View view = getView();
        if (view == null || state.isBusy() || state.isCompleted() || state.isLoading()) return;
        state.requestLocation(() -> {
            if (getView() != view || !isResumed()) return;
            renderControls(view);
            if (state.isOutside()) {
                view.removeCallbacks(locationTick);
                NavHostFragment.findNavController(this).navigate(R.id.action_form_answer_to_outside);
            } else if (submit && state.canSubmit()) submit();
        });
        if (getView() == view) renderControls(view);
    }
    private void submit() {
        View view = requireView();
        showInline(null);
        state.submit(new FormResponseRepository.ResultCallback() {
            @Override public void onSuccess(ReceivedFormDetails data) {
                if (getView() != view || !isResumed()) return;
                view.removeCallbacks(locationTick);
                renderQuestions(view);
                Toast.makeText(requireContext(), R.string.form_answer_completed, Toast.LENGTH_LONG).show();
            }
            @Override public void onFailure(FailureKind failure, String message, boolean uncertain) {
                if (getView() != view || !isResumed()) return;
                renderControls(view);
                if (uncertain) {
                    showInline(getString(R.string.form_answer_submission_uncertain));
                    return;
                }
                if (failure == FailureKind.BUSINESS) showInline(message == null
                        ? getString(R.string.form_answer_send_error) : message);
                else showFailure(failure);
            }
        });
        renderControls(view);
    }
    private void retryLocation() {
        FormLocationRepository.Failure failure = state.getFailure();
        if (failure == FormLocationRepository.Failure.PERMISSION_REQUIRED) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + requireContext().getPackageName())));
        } else if (failure == FormLocationRepository.Failure.LOCATION_DISABLED) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } else checkLocation(false);
    }
    private void renderControls(View view) {
        View questionsView = view.findViewById(R.id.list_form_answer_questions);
        boolean fieldsEnabled = !state.isCompleted() && !state.isLoading() && !state.isOutside();
        setFieldsEnabled(questionsView, fieldsEnabled);
        if (state.getDetails().form.questions != null) {
            for (int index = 0; index < state.getDetails().form.questions.size(); index++) {
                ReceivedFormDetails.Question question = state.getDetails().form.questions.get(index);
                if (!"foto".equals(question.type) || index >= ((ViewGroup) questionsView).getChildCount()) continue;
                View button = ((ViewGroup) questionsView).getChildAt(index)
                        .findViewById(R.id.button_form_question_take_photo);
                if (button != null) button.setEnabled(fieldsEnabled && state.canAddPhoto(question));
            }
        }
        TextView button = view.findViewById(R.id.button_form_answer_complete);
        button.setEnabled(state.canSubmit());
        button.setText(state.isCompleted() ? R.string.form_answer_completed_button
                : state.isLoading() ? R.string.form_answer_sending : R.string.form_answer_complete);
        view.findViewById(R.id.button_form_answer_check_submission).setVisibility(
                state.needsConfirmation() && !state.isLoading() ? View.VISIBLE : View.GONE);
        renderLocation(view);
    }
    private void setFieldsEnabled(View view, boolean enabled) {
        boolean photoContainer = view.getId() == R.id.container_form_question_photos
                || view.getId() == R.id.list_form_question_photos;
        boolean submittedPhoto = view.getId() == R.id.form_answer_submitted_photo;
        view.setEnabled(submittedPhoto || photoContainer && state.isCompleted() || enabled);
        if (submittedPhoto) {
            view.setClickable(true);
            view.setFocusable(true);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) setFieldsEnabled(group.getChildAt(index), enabled);
        }
    }
    private void renderLocation(View view) {
        FormLocationRepository.Failure failure = state.getFailure();
        boolean completed = state.isCompleted();
        view.findViewById(R.id.container_form_answer_location).setVisibility(
                failure != null || completed || state.isBusy() ? View.VISIBLE : View.GONE);
        TextView message = view.findViewById(R.id.text_form_answer_location);
        message.setTextColor(ContextCompat.getColor(requireContext(), R.color.astro_light_background));
        view.findViewById(R.id.container_form_answer_location).setBackgroundResource(completed
                ? R.drawable.bg_home_positive_message : failure != null
                ? R.drawable.bg_form_location_error : R.drawable.bg_profile_panel);
        message.setText(completed ? R.string.form_answer_read_only : state.isBusy() ? R.string.form_answer_locating
                : failure == FormLocationRepository.Failure.PERMISSION_REQUIRED ? R.string.form_answer_location_permission
                : failure == FormLocationRepository.Failure.LOCATION_DISABLED ? R.string.form_answer_location_disabled
                : R.string.form_answer_location_unavailable);
        view.findViewById(R.id.button_form_answer_location_retry).setVisibility(
                failure != null && !state.isBusy() ? View.VISIBLE : View.GONE);
        ((TextView) view.findViewById(R.id.button_form_answer_location_retry)).setText(
                failure == FormLocationRepository.Failure.UNAVAILABLE ? R.string.form_answer_location_retry
                : R.string.form_answer_location_settings);
    }
    private void showInline(String message) {
        TextView error = requireView().findViewById(R.id.text_form_answer_error);
        error.setText(message);
        error.setVisibility(message == null ? View.GONE : View.VISIBLE);
    }
    private void setLoading(View view, boolean loading) {
        view.findViewById(R.id.scroll_form_answer).setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        view.findViewById(R.id.skeleton_form_answer).setVisibility(loading ? View.VISIBLE : View.GONE);
    }
    private void showFailure(FailureKind failure) {
        if (failure == FailureKind.SESSION) SessionNavigation.signOut(this);
        else NavHostFragment.findNavController(this).navigate(failure == FailureKind.CONNECTION
                ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
    }
    @Override public void onResume() {
        super.onResume();
        if (state.getDetails() == null) { showFailure(FailureKind.INTERNAL); return; }
        if (state.needsConfirmation() || state.isCompleted()) refresh();
        else locationTick.run();
    }
    @Override public void onPause() {
        if (getView() != null) requireView().removeCallbacks(locationTick);
        state.pauseLocation();
        state.cancelRequest();
        super.onPause();
    }
}
