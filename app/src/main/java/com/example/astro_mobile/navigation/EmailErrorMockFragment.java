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
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.EmailVerificationViewModel;

public class EmailErrorMockFragment extends Fragment {
    private EmailVerificationViewModel verificationViewModel;

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
        TextView title = view.findViewById(R.id.text_email_error_title);
        TextView body = view.findViewById(R.id.text_email_error_body);
        View retryButton = view.findViewById(R.id.button_email_error_retry);
        TextView retryLabel = view.findViewById(R.id.text_email_error_retry);
        ProgressBar progress = view.findViewById(R.id.progress_email_error_retry);
        // Usa a mesma tela mock para apresentar erro de conexão ou erro interno.
        showCopy(title, body, errorDestination == R.id.connectionErrorFragment);

        // Repete a verificação com o último e-mail informado.
        retryButton.setOnClickListener(clickedView -> {
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

        view.findViewById(R.id.button_email_error_start).setOnClickListener(clickedView ->
                navController.popBackStack(R.id.emailIdentificationFragment, false));
    }

    private void showCopy(TextView title, TextView body, boolean connection) {
        title.setText(connection ? R.string.email_connection_error_title
                : R.string.email_generic_error_title);
        body.setText(connection ? R.string.email_connection_error_body
                : R.string.email_generic_error_body);
    }

    @Override
    public void onDestroyView() {
        // Cancela a tentativa pendente quando esta tela é fechada.
        if (verificationViewModel != null && verificationViewModel.isLoading()) {
            verificationViewModel.cancelCurrentRequest();
        }
        super.onDestroyView();
    }
}
