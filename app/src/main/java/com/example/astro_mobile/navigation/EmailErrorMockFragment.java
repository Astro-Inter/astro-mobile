package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.AccessKeyViewModel;
import com.example.astro_mobile.auth.EmailVerificationViewModel;
import com.example.astro_mobile.auth.PasswordResetViewModel;
import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.example.astro_mobile.data.api.FailureKind;

public class EmailErrorMockFragment extends Fragment {
    private EmailVerificationViewModel verificationViewModel;
    private PasswordResetViewModel resetViewModel;
    private PasswordResetViewModel.ResultCallback resetResultCallback;
    private AccessKeyViewModel accessKeyViewModel;

    public EmailErrorMockFragment() {
        super(R.layout.fragment_email_error_mock);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        verificationViewModel = new ViewModelProvider(requireActivity(),
                EmailVerificationViewModel.Factory.createDefault())
                .get(EmailVerificationViewModel.class);
        NavController navController = NavHostFragment.findNavController(this);
        int errorDestination = navController.getCurrentDestination().getId();
        NavBackStackEntry previous = navController.getPreviousBackStackEntry();
        int source = previous == null ? 0 : previous.getDestination().getId();
        if (source == R.id.firstLoginAccessKeyFragment) {
            accessKeyViewModel = new ViewModelProvider(requireActivity(),
                    new AccessKeyViewModel.Factory()).get(AccessKeyViewModel.class);
        }
        TextView title = view.findViewById(R.id.text_email_error_title);
        TextView body = view.findViewById(R.id.text_email_error_body);
        View retryButton = view.findViewById(R.id.button_email_error_retry);
        TextView retryLabel = view.findViewById(R.id.text_email_error_retry);
        ProgressBar progress = view.findViewById(R.id.progress_email_error_retry);
        // Usa a mesma tela mock para apresentar erro de conexão ou erro interno.
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
            resetViewModel.attach(resetResultCallback);
            ((TextView) view.findViewById(R.id.button_email_error_start))
                    .setText(R.string.password_reset_back_to_login);
            view.findViewById(R.id.button_email_error_start)
                    .setOnClickListener(clickedView -> returnToPasswordLogin(navController));
        }

        // No login, volta ao campo sem conservar a senha; na splash, repete a sessão.
        retryButton.setOnClickListener(clickedView -> {
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
                    NavOptions options = new NavOptions.Builder()
                            .setPopUpTo(errorDestination, true)
                            .setEnterAnim(R.anim.push_enter)
                            .setExitAnim(R.anim.push_exit)
                            .setPopEnterAnim(R.anim.push_pop_enter)
                            .setPopExitAnim(R.anim.push_pop_exit)
                            .build();
                    navController.navigate(target, AuthArgs.of(email, userType), options);
                }
            });
        });

        if (source != R.id.passwordResetRequestFragment) {
            view.findViewById(R.id.button_email_error_start).setOnClickListener(clickedView ->
                    navController.popBackStack(R.id.emailIdentificationFragment, false));
        }
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
                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(errorDestination, true)
                        .setEnterAnim(R.anim.push_enter)
                        .setExitAnim(R.anim.push_exit)
                        .setPopEnterAnim(R.anim.push_pop_enter)
                        .setPopExitAnim(R.anim.push_pop_exit)
                        .build();
                navController.navigate(R.id.firstLoginPasswordFragment, args, options);
            }
        });
    }

    private void retryPasswordReset(View retryButton, TextView retryLabel, ProgressBar progress,
                                    NavController navController) {
        if (resetViewModel == null || resetViewModel.isLoading()) {
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
        resetViewModel.sendPasswordResetEmail(email);
    }

    private void showResetConfirmation(View view, NavController navController) {
        if (getView() != view || !isAdded()) {
            return;
        }
        String email = resetViewModel == null ? null : resetViewModel.getLastEmail();
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.loginPasswordFragment, true)
                .setEnterAnim(R.anim.push_enter)
                .setExitAnim(R.anim.push_exit)
                .setPopEnterAnim(R.anim.push_pop_enter)
                .setPopExitAnim(R.anim.push_pop_exit)
                .build();
        navController.navigate(R.id.passwordResetConfirmationFragment,
                AuthArgs.of(email, null), options);
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
    }

    @Override
    public void onDestroyView() {
        if (accessKeyViewModel != null) {
            accessKeyViewModel.cancelCurrentRequest();
        }
        if (resetViewModel != null && resetResultCallback != null) {
            resetViewModel.detach(resetResultCallback);
            resetResultCallback = null;
        }
        // Cancela a tentativa pendente quando esta tela é fechada.
        if (verificationViewModel != null && verificationViewModel.isLoading()) {
            verificationViewModel.cancelCurrentRequest();
        }
        super.onDestroyView();
    }
}
