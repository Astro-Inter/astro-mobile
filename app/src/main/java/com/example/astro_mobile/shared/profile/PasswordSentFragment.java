package com.example.astro_mobile.shared.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;

public final class PasswordSentFragment extends Fragment {
    public PasswordSentFragment() { super(R.layout.fragment_profile_password_sent); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        String email = requireArguments().getString(AuthArgs.EMAIL);
        ((TextView) view.findViewById(R.id.text_password_email_confirmation_body))
                .setText(getString(R.string.profile_change_password_sent_description, email));
        TextView button = view.findViewById(R.id.button_password_email_confirmation_return);
        button.setText(R.string.profile_back);
        // Remove os passos de envio, sem abrir login nem criar outro perfil.
        button.setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(R.id.profileFragment, false));
    }
}
