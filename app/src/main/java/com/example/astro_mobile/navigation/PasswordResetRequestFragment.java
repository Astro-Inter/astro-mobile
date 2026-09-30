package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.PasswordResetViewModel;
import com.example.astro_mobile.data.firebase.AuthFailureKind;

public class PasswordResetRequestFragment extends Fragment {
    private PasswordResetViewModel resetViewModel;
    private PasswordResetViewModel.ResultCallback resultCallback;

    public PasswordResetRequestFragment() {
        super(R.layout.fragment_password_reset_request);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText emailInput = view.findViewById(R.id.input_password_reset_email);
        View inputContainer = view.findViewById(R.id.container_password_reset_email);
        TextView error = view.findViewById(R.id.text_password_reset_email_error);
        View sendButton = view.findViewById(R.id.button_password_reset_send);
        TextView sendLabel = view.findViewById(R.id.text_password_reset_send);
        ProgressBar progress = view.findViewById(R.id.progress_password_reset_send);
        resetViewModel = new ViewModelProvider(requireActivity(),
                PasswordResetViewModel.Factory.createDefault())
                .get(PasswordResetViewModel.class);

        Bundle args = getArguments();
        String initialEmail = args == null ? null : args.getString(AuthArgs.EMAIL);
        if (savedInstanceState == null && initialEmail != null) {
            emailInput.setText(initialEmail);
        }
        setLoading(emailInput, sendButton, sendLabel, progress, resetViewModel.isLoading());

        // Valida o endereço no aparelho antes de chamar o Firebase.
        sendButton.setOnClickListener(clickedView -> {
            if (resetViewModel.isLoading()) {
                return;
            }
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                showError(inputContainer, error, R.string.password_reset_email_empty_error);
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError(inputContainer, error, R.string.email_identification_invalid_error);
                return;
            }
            inputContainer.setBackgroundResource(R.drawable.bg_email_identification_input);
            error.setVisibility(View.GONE);
            setLoading(emailInput, sendButton, sendLabel, progress, true);
            resetViewModel.sendPasswordResetEmail(email);
        });

        emailInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                // No-op.
            }

            @Override
            public void afterTextChanged(Editable text) {
                if (error.getVisibility() == View.VISIBLE) {
                    inputContainer.setBackgroundResource(R.drawable.bg_email_identification_input);
                    error.setVisibility(View.GONE);
                }
            }
        });

        view.findViewById(R.id.button_password_reset_back)
                .setOnClickListener(clickedView ->
                        NavHostFragment.findNavController(this).navigateUp());

        resultCallback = new PasswordResetViewModel.ResultCallback() {
            @Override
            public void onSuccess() {
                showConfirmation(view);
            }

            @Override
            public void onFailure(AuthFailureKind kind) {
                if (getView() != view || !isAdded()) {
                    return;
                }
                setLoading(emailInput, sendButton, sendLabel, progress, false);
                if (kind == AuthFailureKind.INVALID_CREDENTIALS
                        || kind == AuthFailureKind.DISABLED) {
                    // Mantém a resposta neutra para não revelar contas cadastradas.
                    showConfirmation(view);
                    return;
                }
                NavController navController = NavHostFragment.findNavController(
                        PasswordResetRequestFragment.this);
                navController.navigate(kind == AuthFailureKind.CONNECTION
                                ? R.id.action_password_reset_request_to_connection_error
                                : R.id.action_password_reset_request_to_generic_error);
            }

            private void showConfirmation(View sourceView) {
                if (getView() != sourceView || !isAdded()) {
                    return;
                }
                NavHostFragment.findNavController(PasswordResetRequestFragment.this).navigate(
                        R.id.action_password_reset_request_to_confirmation,
                        AuthArgs.of(resetViewModel.getLastEmail(), null));
            }
        };
        resetViewModel.attach(resultCallback);
    }

    @Override
    public void onDestroyView() {
        if (resetViewModel != null && resultCallback != null) {
            resetViewModel.detach(resultCallback);
            resultCallback = null;
        }
        super.onDestroyView();
    }

    private void showError(View inputContainer, TextView error, int message) {
        inputContainer.setBackgroundResource(R.drawable.bg_login_password_input_error);
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    private void setLoading(EditText emailInput, View button, TextView label,
                            ProgressBar progress, boolean loading) {
        emailInput.setEnabled(!loading);
        button.setEnabled(!loading);
        label.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
