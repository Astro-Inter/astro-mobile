package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;

public class PasswordResetConfirmationFragment extends Fragment {
    public PasswordResetConfirmationFragment() {
        super(R.layout.fragment_password_reset_confirmation);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        String email = args == null ? null : args.getString(AuthArgs.EMAIL);
        TextView message = view.findViewById(R.id.text_password_reset_confirmation_body);
        // Informa qual endereço deve receber o link de redefinição.
        message.setText(getString(R.string.password_reset_confirmation_body,
                email == null ? "" : email));

        // Volta à identificação de e-mail após o envio do link.
        view.findViewById(R.id.button_password_reset_confirmation_email)
                .setOnClickListener(clickedView -> {
                    NavController navController = NavHostFragment.findNavController(this);
                    if (!navController.popBackStack(R.id.emailIdentificationFragment, false)) {
                        navController.navigate(R.id.emailIdentificationFragment);
                    }
                });
    }
}
