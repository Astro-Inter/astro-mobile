package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.graphics.Color;
import android.util.Patterns;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.EmailVerificationViewModel;

public class MockDestinationFragment extends Fragment {
    private EmailVerificationViewModel verificationViewModel;

    public MockDestinationFragment() {
        super(R.layout.fragment_mock_destination);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText emailInput = view.findViewById(R.id.input_email_identification);
        View emailInputContainer = view.findViewById(R.id.container_email_identification_input);
        TextView emailError = view.findViewById(R.id.text_email_identification_error);
        View continueButton = view.findViewById(R.id.button_email_identification_continue);
        TextView continueText = view.findViewById(R.id.text_email_identification_continue);
        ImageView continueIcon = view.findViewById(R.id.image_email_identification_continue);
        ProgressBar progress = view.findViewById(R.id.progress_email_identification);
        verificationViewModel = new ViewModelProvider(requireActivity(),
                EmailVerificationViewModel.Factory.createDefault())
                .get(EmailVerificationViewModel.class);
        if (emailInput.length() == 0 && verificationViewModel.getLastEmail() != null) {
            emailInput.setText(verificationViewModel.getLastEmail());
        }
        String pendingError = verificationViewModel.consumeInlineError();
        if (pendingError != null) {
            showError(emailInputContainer, emailError, pendingError);
        }

        continueButton.setOnClickListener(clickedView -> {
            if (verificationViewModel.isLoading()) {
                return;
            }
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                showError(emailInputContainer, emailError,
                        getString(R.string.email_identification_empty_error));
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError(emailInputContainer, emailError,
                        getString(R.string.email_identification_invalid_error));
                return;
            }
            setLoading(continueButton, continueText, continueIcon, progress, true);
            verificationViewModel.verifyEmail(email, (destination, message, userType) -> {
                if (getView() != view || !isAdded()) {
                    return;
                }
                setLoading(continueButton, continueText, continueIcon, progress, false);
                if (destination == EmailVerificationViewModel.Destination.INLINE_ERROR) {
                    showError(emailInputContainer, emailError,
                            message != null ? message : getString(R.string.email_identification_error));
                    return;
                }
                if (destination == EmailVerificationViewModel.Destination.DISABLED) {
                    showError(emailInputContainer, emailError,
                            getString(R.string.email_identification_disabled_error));
                    return;
                }
                int action;
                Bundle args = null;
                if (destination == EmailVerificationViewModel.Destination.FIRST_ACCESS_KEY) {
                    action = R.id.action_mock_destination_to_first_login_access_key;
                    args = AuthArgs.of(email, userType);
                } else if (destination == EmailVerificationViewModel.Destination.PASSWORD) {
                    action = R.id.action_mock_destination_to_login_password;
                    args = AuthArgs.of(email, userType);
                } else if (destination == EmailVerificationViewModel.Destination.CONNECTION_ERROR) {
                    action = R.id.action_mock_destination_to_connection_error;
                } else {
                    action = R.id.action_mock_destination_to_generic_error;
                }
                Navigation.findNavController(view).navigate(action, args);
            });
        });

        emailInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_GO
                    && actionId != EditorInfo.IME_ACTION_DONE
                    && actionId != EditorInfo.IME_ACTION_NEXT) {
                return false;
            }
            continueButton.performClick();
            return true;
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
                if (emailError.getVisibility() == View.VISIBLE) {
                    emailInputContainer.setBackgroundResource(R.drawable.bg_email_identification_input);
                    emailError.setVisibility(View.GONE);
                }
            }
        });

        TextView help = view.findViewById(R.id.text_email_identification_help);
        String helpText = help.getText().toString();
        String linkText = "Saiba mais";
        int linkStart = helpText.indexOf(linkText);
        if (linkStart < 0) {
            return;
        }

        SpannableString clickableText = new SpannableString(help.getText());
        clickableText.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Navigation.findNavController(widget)
                        .navigate(R.id.action_mock_destination_to_access_key_information);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint drawState) {
                drawState.setColor(Color.rgb(0, 153, 255));
                drawState.setUnderlineText(true);
            }
        }, linkStart, linkStart + linkText.length(),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        help.setText(clickableText);
        help.setMovementMethod(LinkMovementMethod.getInstance());
        help.setHighlightColor(ContextCompat.getColor(requireContext(), R.color.access_key_help_ripple));
    }

    private void showError(View inputContainer, TextView errorView, String message) {
        inputContainer.setBackgroundResource(R.drawable.bg_login_password_input_error);
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    private void setLoading(View button, TextView label, ImageView icon,
                            ProgressBar progress, boolean loading) {
        button.setEnabled(!loading);
        label.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        icon.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        if (verificationViewModel != null && verificationViewModel.isLoading()) {
            verificationViewModel.cancelCurrentRequest();
        }
        super.onDestroyView();
    }
}
