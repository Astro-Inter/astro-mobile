package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.shared.navigation.HomeNavigation;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.AccessKeyViewModel;
import com.example.astro_mobile.auth.EmailVerificationViewModel;
import com.example.astro_mobile.auth.FirstLoginPasswordViewModel;
import com.example.astro_mobile.auth.MockSession;
import com.example.astro_mobile.auth.PasswordResetViewModel;
import com.example.astro_mobile.data.local.FlowPreferences;
import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.auth.SessionNavigation;

public class ErrorFragment extends Fragment {
    private EmailVerificationViewModel verificationViewModel;
    private PasswordResetViewModel resetViewModel;
    private PasswordResetViewModel.ResultCallback resetResultCallback;
    private AccessKeyViewModel accessKeyViewModel;
    private FirstLoginPasswordViewModel registrationViewModel;

    public ErrorFragment() {
        super(R.layout.fragment_error);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = NavHostFragment.findNavController(this);
        int errorDestination = navController.getCurrentDestination().getId();
        NavBackStackEntry previous = navController.getPreviousBackStackEntry();
        int source = previous == null ? 0 : previous.getDestination().getId();
        // Erros do perfil não retomam a identificação de e-mail nem a recuperação do login.
        if (source == R.id.profileFragment || source == R.id.changePasswordFragment) {
            configureProfileError(view, navController, source);
            return;
        }
        verificationViewModel = new ViewModelProvider(requireActivity(),
                EmailVerificationViewModel.Factory.createDefault())
                .get(EmailVerificationViewModel.class);
        if (source == R.id.firstLoginAccessKeyFragment
                || source == R.id.firstLoginPasswordFragment) {
            accessKeyViewModel = new ViewModelProvider(requireActivity(),
                    new AccessKeyViewModel.Factory()).get(AccessKeyViewModel.class);
        }
        if (source == R.id.firstLoginPasswordFragment) {
            registrationViewModel = new ViewModelProvider(requireActivity(),
                    new FirstLoginPasswordViewModel.Factory())
                    .get(FirstLoginPasswordViewModel.class);
        }
        TextView title = view.findViewById(R.id.text_email_error_title);
        TextView body = view.findViewById(R.id.text_email_error_body);
        View retryButton = view.findViewById(R.id.button_email_error_retry);
        TextView retryLabel = view.findViewById(R.id.text_email_error_retry);
        ProgressBar progress = view.findViewById(R.id.progress_email_error_retry);
        // Reaproveita o mesmo layout para os dois erros, mudando o título e a mensagem.
        showCopy(title, body, errorDestination == R.id.connectionErrorFragment);
        if (source == R.id.splashFragment) {
            view.findViewById(R.id.button_email_error_start).setVisibility(View.GONE);
        }
        if (source == R.id.passwordResetRequestFragment) {
            resetViewModel = new ViewModelProvider(requireActivity(),
                    PasswordResetViewModel.Factory.createDefault())
                    .get(PasswordResetViewModel.class);
            resetResultCallback = new PasswordResetViewModel.ResultCallback() {
                @Override
                public void onSuccess() {
                    showResetConfirmation(view, navController);
                }

                @Override
                public void onFailure(AuthFailureKind kind) {
                    if (getView() != view || !isAdded()) {
                        return;
                    }
                    retryButton.setEnabled(true);
                    retryLabel.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                    if (kind == AuthFailureKind.INVALID_CREDENTIALS
                            || kind == AuthFailureKind.DISABLED) {
                        showResetConfirmation(view, navController);
                    } else {
                        showCopy(title, body, kind == AuthFailureKind.CONNECTION);
                    }
                }
            };
            ((TextView) view.findViewById(R.id.button_email_error_start))
                    .setText(R.string.password_reset_back_to_login);
            view.findViewById(R.id.button_email_error_start)
                    .setOnClickListener(clickedView -> returnToPasswordLogin(navController));
        }

        // No login, volta ao campo sem conservar a senha; na splash, repete a sessão.
        retryButton.setOnClickListener(clickedView -> {
            if (source == R.id.firstLoginPasswordFragment) {
                retryActivation(view, retryButton, retryLabel, progress, title, body,
                        navController);
                return;
            }
            if (source == R.id.firstLoginAccessKeyFragment) {
                retryAccessKey(view, retryButton, retryLabel, progress, title, body,
                        navController, errorDestination);
                return;
            }
            if (source == R.id.passwordResetRequestFragment) {
                retryPasswordReset(retryButton, retryLabel, progress, navController);
                return;
            }
            if (source == R.id.loginPasswordFragment || source == R.id.splashFragment) {
                navController.popBackStack(source, false);
                return;
            }
            // Na identificação, repete o POST com o último e-mail informado.
            if (verificationViewModel.isLoading()) {
                return;
            }
            String email = verificationViewModel.getLastEmail();
            if (email == null) {
                navController.popBackStack(R.id.emailIdentificationFragment, false);
                return;
            }
            retryButton.setEnabled(false);
            retryLabel.setVisibility(View.INVISIBLE);
            progress.setVisibility(View.VISIBLE);
            verificationViewModel.verifyEmail(email, (destination, message, userType) -> {
                if (getView() != view || !isAdded()) {
                    return;
                }
                retryButton.setEnabled(true);
                retryLabel.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);

                // Volta ao campo para erros de negócio ou segue no fluxo após sucesso.
                if (destination == EmailVerificationViewModel.Destination.INLINE_ERROR
                        || destination == EmailVerificationViewModel.Destination.DISABLED) {
                    verificationViewModel.setPendingInlineError(
                            destination == EmailVerificationViewModel.Destination.DISABLED
                                    ? getString(R.string.email_identification_disabled_error)
                                    : message != null ? message
                                            : getString(R.string.email_identification_error));
                    navController.popBackStack(R.id.emailIdentificationFragment, false);
                } else if (destination == EmailVerificationViewModel.Destination.CONNECTION_ERROR
                        || destination == EmailVerificationViewModel.Destination.INTERNAL_ERROR) {
                    showCopy(title, body,
                            destination == EmailVerificationViewModel.Destination.CONNECTION_ERROR);
                } else {
                    int target = destination == EmailVerificationViewModel.Destination.FIRST_ACCESS_KEY
                            ? R.id.firstLoginAccessKeyFragment : R.id.loginPasswordFragment;
                    Bundle args = AuthArgs.of(email, userType);
                    // Retoma o cadastro pendente sem repetir a chave já utilizada.
                    FirstLoginPasswordViewModel registration = new ViewModelProvider(
                            requireActivity(), new FirstLoginPasswordViewModel.Factory())
                            .get(FirstLoginPasswordViewModel.class);
                    if (destination == EmailVerificationViewModel.Destination.FIRST_ACCESS_KEY
                            && "COLABORADOR".equals(userType)
                            && registration.hasAuthenticatedAccount(email)) {
                        new ViewModelProvider(requireActivity(), new AccessKeyViewModel.Factory())
                                .get(AccessKeyViewModel.class).setContext(email, userType);
                        target = R.id.firstLoginPasswordFragment;
                        args = new Bundle();
                        args.putString(AuthArgs.EMAIL, email);
                    }
                    navController.navigate(target, args,
                            pushReplacing(errorDestination));
                }
            });
        });

        if (source != R.id.passwordResetRequestFragment) {
            view.findViewById(R.id.button_email_error_start).setOnClickListener(clickedView -> {
                if (!navController.popBackStack(R.id.emailIdentificationFragment, false)) {
                    navController.navigate(R.id.emailIdentificationFragment, null,
                            new NavOptions.Builder().setPopUpTo(R.id.nav_graph, false).build());
                }
            });
        }
    }

    private void retryActivation(View view, View button, TextView label, ProgressBar progress,
                                 TextView title, TextView body, NavController navController) {
        if (registrationViewModel.isLoading()) {
            return;
        }
        String email = registrationViewModel.getEmail();
        if (email == null || !registrationViewModel.hasAuthenticatedAccount(email)) {
            // Sem sessão Firebase, pede a senha na tela de cadastro antes de ativar.
            navController.popBackStack(R.id.firstLoginPasswordFragment, false);
            return;
        }
        button.setEnabled(false);
        label.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.VISIBLE);
        // A identidade já existe: repete somente o POST /activate com o mesmo UID.
        registrationViewModel.register(email, null, true,
                new FirstLoginPasswordViewModel.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        if (getView() != view || !isAdded()) {
                            return;
                        }
                        MockSession.clear(requireContext());
                        FlowPreferences.saveEmployeeFlow(requireContext());
                        String userType = accessKeyViewModel.getUserType();
                        Toast.makeText(requireContext(),
                                getString(R.string.auth_user_type_toast, userType),
                                Toast.LENGTH_LONG).show();
                        navController.navigate(R.id.employeeHomeFragment,
                                AuthArgs.of(email, userType),
                                new NavOptions.Builder()
                                        .setPopUpTo(R.id.nav_graph, false)
                                        .setEnterAnim(R.anim.push_enter)
                                        .setExitAnim(R.anim.push_exit)
                                        .setPopEnterAnim(R.anim.push_pop_enter)
                                        .setPopExitAnim(R.anim.push_pop_exit)
                                        .build());
                    }

                    @Override
                    public void onAuthFailure(AuthFailureKind kind) {
                        if (getView() == view && isAdded()) {
                            navController.popBackStack(R.id.firstLoginPasswordFragment, false);
                        }
                    }

                    @Override
                    public void onActivationFailure(FailureKind kind) {
                        if (getView() != view || !isAdded()) {
                            return;
                        }
                        button.setEnabled(true);
                        label.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                        showCopy(title, body, kind == FailureKind.CONNECTION);
                    }
                });
    }

    private void configureProfileError(View view, NavController navigation, int source) {
        showCopy(view.findViewById(R.id.text_email_error_title),
                view.findViewById(R.id.text_email_error_body), false);
        TextView back = view.findViewById(R.id.button_email_error_start);
        back.setText(R.string.profile_retry_back);
        back.setOnClickListener(clicked -> navigation.popBackStack(
                source == R.id.profileFragment ? HomeNavigation.destination(requireContext())
                        : R.id.changePasswordFragment, false));
        View retry = view.findViewById(R.id.button_email_error_retry);
        retry.setOnClickListener(clicked -> {
            if (source == R.id.profileFragment) {
                // Retorna ao perfil, que mostra skeleton e repete sua consulta.
                navigation.popBackStack(R.id.profileFragment, false);
                return;
            }
            // No erro do envio, tenta novamente com a conta Firebase da sessão.
            resetViewModel = new ViewModelProvider(navigation.getBackStackEntry(R.id.changePasswordFragment),
                    PasswordResetViewModel.Factory.createDefault()).get(PasswordResetViewModel.class);
            String email = resetViewModel.getCurrentEmail();
            if (email == null) {
                SessionNavigation.signOut(this);
                return;
            }
            retry.setEnabled(false);
            view.findViewById(R.id.text_email_error_retry).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.progress_email_error_retry).setVisibility(View.VISIBLE);
            resetViewModel.sendPasswordResetEmail(email, new PasswordResetViewModel.ResultCallback() {
                @Override
                public void onSuccess() {
                    if (getView() != view || !isAdded()) return;
                    navigation.navigate(R.id.passwordSentFragment, AuthArgs.of(email, null),
                            pushReplacing(R.id.changePasswordFragment));
                }

                @Override
                public void onFailure(AuthFailureKind kind) {
                    if (getView() != view || !isAdded()) return;
                    retry.setEnabled(true);
                    view.findViewById(R.id.text_email_error_retry).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.progress_email_error_retry).setVisibility(View.GONE);
                }
            });
        });
    }

    private void retryAccessKey(View view, View button, TextView label, ProgressBar progress,
                                TextView title, TextView body, NavController navController,
                                int errorDestination) {
        // Repete a validação da chave, e não a identificação do e-mail.
        if (accessKeyViewModel.isLoading()) {
            return;
        }
        String key = accessKeyViewModel.getLastKey();
        if (key == null || accessKeyViewModel.getEmail() == null) {
            navController.popBackStack(R.id.firstLoginAccessKeyFragment, false);
            return;
        }
        button.setEnabled(false);
        label.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.VISIBLE);
        accessKeyViewModel.verifyKey(key, (failure, message) -> {
            if (getView() != view || !isAdded()) {
                return;
            }
            button.setEnabled(true);
            label.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
            if (failure == FailureKind.BUSINESS) {
                accessKeyViewModel.setPendingInlineError(message != null ? message
                        : getString(R.string.first_login_access_key_error));
                navController.popBackStack(R.id.firstLoginAccessKeyFragment, false);
            } else if (failure != null) {
                showCopy(title, body, failure == FailureKind.CONNECTION);
            } else {
                Bundle args = new Bundle();
                args.putString(AuthArgs.EMAIL, accessKeyViewModel.getEmail());
                navController.navigate(R.id.firstLoginPasswordFragment, args,
                        pushReplacing(errorDestination));
            }
        });
    }

    private void retryPasswordReset(View retryButton, TextView retryLabel, ProgressBar progress,
                                    NavController navController) {
        if (resetViewModel.isLoading()) {
            return;
        }
        String email = resetViewModel.getLastEmail();
        if (email == null) {
            navController.popBackStack(R.id.passwordResetRequestFragment, false);
            return;
        }
        retryButton.setEnabled(false);
        retryLabel.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.VISIBLE);
        resetViewModel.sendPasswordResetEmail(email, resetResultCallback);
    }

    private void showResetConfirmation(View view, NavController navController) {
        if (getView() != view || !isAdded()) {
            return;
        }
        navController.navigate(R.id.passwordResetConfirmationFragment,
                AuthArgs.of(resetViewModel.getLastEmail(), null),
                pushReplacing(R.id.loginPasswordFragment));
    }

    private NavOptions pushReplacing(int destination) {
        // Mantém o mesmo push e remove da pilha a tela substituída após a nova tentativa.
        return new NavOptions.Builder()
                .setPopUpTo(destination, true)
                .setEnterAnim(R.anim.push_enter)
                .setExitAnim(R.anim.push_exit)
                .setPopEnterAnim(R.anim.push_pop_enter)
                .setPopExitAnim(R.anim.push_pop_exit)
                .build();
    }

    private void returnToPasswordLogin(NavController navController) {
        if (!navController.popBackStack(R.id.loginPasswordFragment, false)) {
            navController.navigate(R.id.emailIdentificationFragment);
        }
    }

    private void showCopy(TextView title, TextView body, boolean connection) {
        title.setText(connection ? R.string.email_connection_error_title
                : R.string.email_generic_error_title);
        body.setText(connection ? R.string.email_connection_error_body
                : R.string.email_generic_error_body);
        if (!connection && registrationViewModel != null
                && registrationViewModel.getEmail() != null
                && registrationViewModel.hasAuthenticatedAccount(registrationViewModel.getEmail())) {
            body.setText(R.string.first_login_password_activation_error);
        }
    }

    @Override
    public void onDestroyView() {
        if (registrationViewModel != null) {
            registrationViewModel.clearRequest();
        }
        if (accessKeyViewModel != null) {
            accessKeyViewModel.cancelCurrentRequest();
        }
        if (resetViewModel != null) {
            resetViewModel.clearRequest();
        }
        resetResultCallback = null;
        // Cancela a tentativa pendente quando esta tela é fechada.
        if (verificationViewModel != null && verificationViewModel.isLoading()) {
            verificationViewModel.cancelCurrentRequest();
        }
        super.onDestroyView();
    }
}
