package com.example.astro_mobile.shared.profile;

import android.os.Bundle;
import android.view.View;
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
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.firebase.AuthFailureKind;

public final class ChangePasswordFragment extends Fragment {
    private PasswordResetViewModel model;

    public ChangePasswordFragment() { super(R.layout.fragment_profile_change_password); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        NavController navigation = NavHostFragment.findNavController(this);
        model = new ViewModelProvider(navigation.getBackStackEntry(R.id.changePasswordFragment),
                PasswordResetViewModel.Factory.createDefault()).get(PasswordResetViewModel.class);
        String email = model.getCurrentEmail();
        if (email == null) {
            SessionNavigation.signOut(this);
            return;
        }
        // O usuário confirma o destinatário da sessão, sem poder alterar o e-mail.
        ((TextView) view.findViewById(R.id.text_profile_password_description))
                .setText(getString(R.string.profile_change_password_description, email));
        view.findViewById(R.id.button_profile_password_back).setOnClickListener(clicked -> navigation.navigateUp());
        View button = view.findViewById(R.id.button_profile_password_send);
        button.setOnClickListener(clicked -> {
            button.setEnabled(false);
            view.findViewById(R.id.text_profile_password_send).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.progress_profile_password_send).setVisibility(View.VISIBLE);
            model.sendPasswordResetEmail(email, new PasswordResetViewModel.ResultCallback() {
                @Override
                public void onSuccess() {
                    if (getView() != view || !isAdded()) return;
                    // Enviar o link não encerra a sessão e não confirma a troca da senha.
                    navigation.navigate(R.id.action_change_password_to_sent, AuthArgs.of(email, null));
                }

                @Override
                public void onFailure(AuthFailureKind kind) {
                    if (getView() != view || !isAdded()) return;
                    navigation.navigate(R.id.action_change_password_to_generic_error);
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        // O SDK pode terminar o envio, mas não atualiza uma tela que já foi fechada.
        model.clearRequest();
        super.onDestroyView();
    }
}
