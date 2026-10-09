package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AccessKeyViewModel;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.EmailVerificationViewModel;
import com.example.astro_mobile.auth.FirstLoginPasswordViewModel;
import com.example.astro_mobile.auth.MockSession;
import com.example.astro_mobile.auth.SessionViewModel;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.example.astro_mobile.data.local.FlowPreferences;

public class FirstLoginPasswordFragment extends Fragment {
    private FirstLoginPasswordViewModel viewModel;

    public FirstLoginPasswordFragment() {
        super(R.layout.fragment_first_login_password);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(),
                new FirstLoginPasswordViewModel.Factory()).get(FirstLoginPasswordViewModel.class);
        AccessKeyViewModel accessKeyViewModel = new ViewModelProvider(requireActivity(),
                new AccessKeyViewModel.Factory()).get(AccessKeyViewModel.class);
        Bundle args = getArguments();
        String email = args == null ? null : args.getString(AuthArgs.EMAIL);
        boolean resumeOnly = args != null && args.getBoolean(AuthArgs.RESUME_ACTIVATION);
        NavController navController = NavHostFragment.findNavController(this);
        // A ativação pertence somente ao colaborador; o e-mail vem do fluxo anterior.
        if (email == null || !"COLABORADOR".equals(accessKeyViewModel.getUserType())) {
            navController.navigate(R.id.action_first_login_password_to_generic_error);
            return;
        }

        EditText passwordInput = view.findViewById(R.id.input_first_login_password);
        EditText confirmationInput = view.findViewById(R.id.input_first_login_password_confirmation);
        configurePasswordVisibility(passwordInput,
                view.findViewById(R.id.button_first_login_password_visibility));
        configurePasswordVisibility(confirmationInput,
                view.findViewById(R.id.button_first_login_password_confirmation_visibility));
        View passwordContainer = view.findViewById(R.id.container_first_login_password_input);
        View confirmationContainer = view.findViewById(R.id.container_first_login_password_confirmation);
        TextView error = view.findViewById(R.id.text_first_login_password_mismatch);
        View registerButton = view.findViewById(R.id.button_first_login_password_enter);
        TextView registerLabel = view.findViewById(R.id.text_first_login_password_enter);
        ImageView registerIcon = view.findViewById(R.id.image_first_login_password_enter);
        ProgressBar progress = view.findViewById(R.id.progress_first_login_password);
        View backButton = view.findViewById(R.id.button_first_login_password_back);
        showRegistrationState(view, email, resumeOnly);

        FirstLoginPasswordViewModel.ResultCallback resultCallback =
                new FirstLoginPasswordViewModel.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        if (getView() != view || !isAdded()) {
                            return;
                        }
                        // Só 204 libera a home; a sessão permanece exclusivamente no Firebase.
                        passwordInput.setText(null);
                        confirmationInput.setText(null);
                        MockSession.clear(requireContext());
                        FlowPreferences.saveEmployeeFlow(requireContext());
                        String userType = accessKeyViewModel.getUserType();
                        Toast.makeText(requireContext(),
                                getString(R.string.auth_user_type_toast, userType),
                                Toast.LENGTH_LONG).show();
                        navController.navigate(R.id.action_first_login_password_to_employee_home,
                                AuthArgs.of(email, userType));
                    }

                    @Override
                    public void onAuthFailure(AuthFailureKind kind) {
                        if (getView() != view || !isAdded()) {
                            return;
                        }
                        setLoading(registerButton, registerLabel, registerIcon, progress,
                                passwordInput, confirmationInput, backButton, false);
                        if (kind == AuthFailureKind.WEAK_PASSWORD) {
                            showError(passwordContainer, error, R.string.first_login_password_policy);
                        } else if (kind == AuthFailureKind.INVALID_CREDENTIALS) {
                            showError(passwordContainer, error, R.string.login_password_error);
                        } else if (kind == AuthFailureKind.DISABLED) {
                            // Conta desativada volta ao e-mail com o aviso já usado no projeto.
                            new ViewModelProvider(requireActivity(),
                                    SessionViewModel.Factory.createDefault())
                                    .get(SessionViewModel.class).signOut();
                            new ViewModelProvider(requireActivity(),
                                    EmailVerificationViewModel.Factory.createDefault())
                                    .get(EmailVerificationViewModel.class).setPendingInlineError(
                                            getString(R.string.email_identification_disabled_error));
                            navController.navigate(R.id.emailIdentificationFragment, null,
                                    new NavOptions.Builder().setPopUpTo(R.id.nav_graph, false).build());
                        } else {
                            showRequestError(navController, kind == AuthFailureKind.CONNECTION);
                        }
                    }

                    @Override
                    public void onActivationFailure(FailureKind kind) {
                        if (getView() != view || !isAdded()) {
                            return;
                        }
                        // O erro não apaga a conta criada nem manda criar outra.
                        passwordInput.setText(null);
                        confirmationInput.setText(null);
                        setLoading(registerButton, registerLabel, registerIcon, progress,
                                passwordInput, confirmationInput, backButton, false);
                        showRegistrationState(view, email, resumeOnly);
                        showRequestError(navController, kind == FailureKind.CONNECTION);
                    }
                };

        registerButton.setOnClickListener(clickedView -> {
            if (viewModel.isLoading()) {
                return;
            }
            String password = passwordInput.getText().toString();
            // Uma conta já autenticada não precisa informar ou confirmar a senha de novo.
            if (!viewModel.hasAuthenticatedAccount(email)) {
                if (password.isEmpty()) {
                    showError(passwordContainer, error, R.string.login_password_empty_error);
                    passwordInput.requestFocus();
                    return;
                }
                if (!resumeOnly) {
                    if (!FirstLoginPasswordViewModel.isPasswordValid(password)) {
                        showError(passwordContainer, error, R.string.first_login_password_policy);
                        passwordInput.requestFocus();
                        return;
                    }
                    if (!password.equals(confirmationInput.getText().toString())) {
                        confirmationContainer.setBackgroundResource(
                                R.drawable.bg_login_password_input_error);
                        showError(passwordContainer, error, R.string.first_login_password_mismatch);
                        confirmationInput.requestFocus();
                        return;
                    }
                }
            }
            error.setVisibility(View.GONE);
            setLoading(registerButton, registerLabel, registerIcon, progress,
                    passwordInput, confirmationInput, backButton, true);
            viewModel.register(email, password, resumeOnly, resultCallback);
        });

        // Confirma pelo teclado tanto no cadastro quanto na retomada com senha existente.
        passwordInput.setImeOptions(resumeOnly ? EditorInfo.IME_ACTION_DONE
                : EditorInfo.IME_ACTION_NEXT);
        TextView.OnEditorActionListener submit = (textView, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return false;
            }
            registerButton.performClick();
            return true;
        };
        passwordInput.setOnEditorActionListener(submit);
        confirmationInput.setOnEditorActionListener(submit);

        // Limpa o destaque quando o usuário corrige uma senha.
        TextWatcher clearError = new TextWatcher() {
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
                passwordContainer.setBackgroundResource(R.drawable.bg_email_identification_input);
                confirmationContainer.setBackgroundResource(R.drawable.bg_email_identification_input);
                error.setVisibility(View.GONE);
            }
        };
        passwordInput.addTextChangedListener(clearError);
        confirmationInput.addTextChangedListener(clearError);
        backButton.setOnClickListener(clickedView -> {
            // Após criar a conta, volta ao e-mail sem exigir uma chave já utilizada.
            if (viewModel.hasAuthenticatedAccount(email)) {
                navController.navigate(R.id.emailIdentificationFragment, null,
                        new NavOptions.Builder().setPopUpTo(R.id.nav_graph, false).build());
            } else if (!navController.navigateUp()) {
                navController.navigate(R.id.emailIdentificationFragment);
            }
        });
    }

    private void configurePasswordVisibility(EditText input, ImageButton button) {
        // Usa os mesmos ícones do login e alterna cada campo sem alterar a senha digitada.
        button.setOnClickListener(clicked -> {
            if (!input.isEnabled()) return;
            boolean hidden = input.getTransformationMethod() instanceof PasswordTransformationMethod;
            input.setTransformationMethod(hidden ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            button.setImageResource(hidden ? R.drawable.ic_login_eye_off : R.drawable.ic_login_eye);
            button.setContentDescription(getString(hidden ? R.string.login_password_hide_description
                    : R.string.login_password_show_description));
            input.setSelection(input.length());
        });
    }

    private void showRegistrationState(View view, String email, boolean resumeOnly) {
        boolean pending = viewModel.hasAuthenticatedAccount(email);
        ((TextView) view.findViewById(R.id.text_first_login_password_title)).setText(
                pending || resumeOnly ? R.string.first_login_password_resume_title
                        : R.string.first_login_password_title);
        view.findViewById(R.id.text_first_login_password_label)
                .setVisibility(pending ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_first_login_password_input)
                .setVisibility(pending ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.text_first_login_password_policy)
                .setVisibility(pending || resumeOnly ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.text_first_login_password_confirmation_label)
                .setVisibility(pending || resumeOnly ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_first_login_password_confirmation)
                .setVisibility(pending || resumeOnly ? View.GONE : View.VISIBLE);
        TextView notice = view.findViewById(R.id.text_first_login_password_pending);
        notice.setVisibility(pending || resumeOnly ? View.VISIBLE : View.GONE);
        notice.setText(pending ? R.string.first_login_password_pending
                : R.string.first_login_password_resume_description);
    }

    private void showRequestError(NavController navController, boolean connection) {
        navController.navigate(connection ? R.id.action_first_login_password_to_connection_error
                : R.id.action_first_login_password_to_generic_error);
    }

    private void showError(View container, TextView error, int message) {
        container.setBackgroundResource(R.drawable.bg_login_password_input_error);
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    private void setLoading(View button, TextView label, ImageView icon, ProgressBar progress,
                            EditText password, EditText confirmation, View back, boolean loading) {
        button.setEnabled(!loading);
        password.setEnabled(!loading);
        confirmation.setEnabled(!loading);
        back.setEnabled(!loading);
        label.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        icon.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        if (viewModel != null) {
            viewModel.clearRequest();
        }
        super.onDestroyView();
    }
}
