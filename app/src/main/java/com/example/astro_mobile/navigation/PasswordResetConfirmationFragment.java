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

public class PasswordResetConfirmationFragment extends Fragment {
    public PasswordResetConfirmationFragment() {
        super(R.layout.fragment_password_reset_confirmation);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView button = view.findViewById(R.id.button_password_email_confirmation_return);
        button.setText(R.string.password_reset_back_to_email);
        // Volta à identificação de e-mail após o envio do link.
        button.setOnClickListener(clickedView -> {
            NavController navController = NavHostFragment.findNavController(this);
            if (!navController.popBackStack(R.id.emailIdentificationFragment, false)) {
                navController.navigate(R.id.emailIdentificationFragment);
            }
        });
    }
}
