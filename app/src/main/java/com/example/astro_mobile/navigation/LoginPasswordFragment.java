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
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.MockSession;
import com.example.astro_mobile.auth.EmailVerificationViewModel;
import com.example.astro_mobile.auth.PasswordLoginViewModel;
import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.example.astro_mobile.data.local.FlowPreferences;

public class LoginPasswordFragment extends Fragment {
    private PasswordLoginViewModel loginViewModel;
    private PasswordLoginViewModel.ResultCallback resultCallback;

    public LoginPasswordFragment() {
        super(R.layout.fragment_login_password);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText passwordInput = view.findViewById(R.id.input_login_password);
        View inputContainer = view.findViewById(R.id.container_login_password_input);
        TextView error = view.findViewById(R.id.text_login_password_error);
        ImageButton visibilityButton = view.findViewById(R.id.button_login_password_visibility);
        View enterButton = view.findViewById(R.id.button_login_password_enter);
        TextView enterText = view.findViewById(R.id.text_login_password_enter);
        ImageView enterIcon = view.findViewById(R.id.image_login_password_enter);
        ProgressBar progress = view.findViewById(R.id.progress_login_password);
        TextView forgotPassword = view.findViewById(R.id.text_login_password_forgot);
        loginViewModel = new ViewModelProvider(this, PasswordLoginViewModel.Factory.createDefault())
                .get(PasswordLoginViewModel.class);
        setLoading(enterButton, enterText, enterIcon, progress, forgotPassword,
                loginViewModel.isLoading());

        // Alterna a exibição da senha e o ícone do botão de visibilidade.
        visibilityButton.setOnClickListener(clickedView -> {
            boolean isPasswordHidden = passwordInput.getTransformationMethod()
                    instanceof PasswordTransformationMethod;
            passwordInput.setTransformationMethod(isPasswordHidden
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            visibilityButton.setContentDescription(getString(isPasswordHidden
                    ? R.string.login_password_hide_description
                    : R.string.login_password_show_description));
            visibilityButton.setImageResource(isPasswordHidden
                    ? R.drawable.ic_login_eye_off
                    : R.drawable.ic_login_eye);
            passwordInput.setSelection(passwordInput.length());
        });

        // Confere a senha no Firebase antes de avançar para o fluxo do perfil recebido.
        enterButton.setOnClickListener(clickedView -> {
            if (loginViewModel.isLoading()) {
                return;
            }
            String password = passwordInput.getText().toString();
            if (password.isEmpty()) {
                showError(inputContainer, error, R.string.login_password_empty_error);
                return;
            }
            Bundle args = getArguments();
            String email = args == null ? null : args.getString(AuthArgs.EMAIL);
            String userType = args == null ? null : args.getString(AuthArgs.USER_TYPE);
            if (email == null || !MockSession.isKnownUserType(userType)) {
                Navigation.findNavController(clickedView)
                        .navigate(R.id.action_login_password_to_generic_error);
                return;
            }
            setLoading(enterButton, enterText, enterIcon, progress, forgotPassword, true);
            loginViewModel.signIn(email, password);
        });
        // Abre a recuperação já preenchida com o e-mail validado anteriormente.
        forgotPassword.setOnClickListener(clickedView -> {
            if (loginViewModel.isLoading()) {
                return;
            }
            Bundle args = getArguments();
            String email = args == null ? null : args.getString(AuthArgs.EMAIL);
            if (email == null) {
                Navigation.findNavController(clickedView)
                        .navigate(R.id.action_login_password_to_generic_error);
                return;
            }
            Navigation.findNavController(clickedView).navigate(
                    R.id.action_login_password_to_reset_request,
                    AuthArgs.copy(args));
        });
        // Permite acionar Entrar pelo teclado.
        passwordInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return false;
            }
            enterButton.performClick();
            return true;
        });
        // Retira o erro visual quando o usuário volta a digitar.
        passwordInput.addTextChangedListener(new TextWatcher() {
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

        view.findViewById(R.id.button_login_password_back)
                .setOnClickListener(clickedView ->
                        NavHostFragment.findNavController(this).navigateUp());

        // Reassocia o resultado ao layout atual após uma possível recriação da tela.
        resultCallback = new PasswordLoginViewModel.ResultCallback() {
            @Override
            public void onSuccess() {
                if (getView() != view || !isAdded()) {
                    return;
                }
                setLoading(enterButton, enterText, enterIcon, progress, forgotPassword, false);
                Bundle args = getArguments();
                String userType = args == null ? null : args.getString(AuthArgs.USER_TYPE);
                passwordInput.setText(null);
                // O Firebase é a sessão real; remove qualquer sessão mock anterior.
                MockSession.clear(requireContext());
                FlowPreferences.clear(requireContext());
                Toast.makeText(requireContext(),
                        getString(R.string.auth_user_type_toast, userType),
                        Toast.LENGTH_LONG).show();
                Navigation.findNavController(view)
                        .navigate("COLABORADOR".equals(userType)
                                        ? R.id.action_login_password_to_employee_home
                                        : R.id.action_login_password_to_flow_choice,
                                AuthArgs.copy(args));
            }

            @Override
            public void onFailure(AuthFailureKind kind) {
                if (getView() != view || !isAdded()) {
                    return;
                }
                setLoading(enterButton, enterText, enterIcon, progress, forgotPassword, false);
                if (kind == AuthFailureKind.INVALID_CREDENTIALS) {
                    showError(inputContainer, error, R.string.login_password_error);
                } else if (kind == AuthFailureKind.DISABLED) {
                    new ViewModelProvider(requireActivity(),
                            EmailVerificationViewModel.Factory.createDefault())
                            .get(EmailVerificationViewModel.class)
                            .setPendingInlineError(
                                    getString(R.string.email_identification_disabled_error));
                    NavHostFragment.findNavController(LoginPasswordFragment.this).navigateUp();
                } else {
                    Navigation.findNavController(view).navigate(
                            kind == AuthFailureKind.CONNECTION
                                    ? R.id.action_login_password_to_connection_error
                                    : R.id.action_login_password_to_generic_error);
                }
            }

        };
        loginViewModel.attach(resultCallback);
    }

    @Override
    public void onDestroyView() {
        if (loginViewModel != null && resultCallback != null) {
            loginViewModel.detach(resultCallback);
            resultCallback = null;
        }
        super.onDestroyView();
    }

    private void showError(View inputContainer, TextView error, int message) {
        inputContainer.setBackgroundResource(R.drawable.bg_login_password_input_error);
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    private void setLoading(View button, TextView label, ImageView icon, ProgressBar progress,
                            TextView forgotPassword, boolean loading) {
        button.setEnabled(!loading);
        forgotPassword.setEnabled(!loading);
        label.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        icon.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
