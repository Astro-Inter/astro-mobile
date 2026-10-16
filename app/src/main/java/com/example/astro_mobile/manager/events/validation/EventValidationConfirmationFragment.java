package com.example.astro_mobile.manager.events.validation;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;

public final class EventValidationConfirmationFragment extends Fragment {
    private Runnable returnHome;

    public EventValidationConfirmationFragment() {
        super(R.layout.fragment_employee_event_conclusion);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Reutiliza o mesmo check e layout da confirmação do Colaborador, com o texto do Gestor.
        ((TextView) view.findViewById(R.id.text_event_confirmation))
                .setText(R.string.event_validation_confirmation);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Exibe a confirmação por dois segundos e abre uma nova Home do Gestor.
        returnHome = () -> NavHostFragment.findNavController(this).navigate(
                R.id.action_event_validation_confirmation_to_home, AuthArgs.copy(getArguments()));
        requireView().postDelayed(returnHome, 2_000L);
    }

    @Override
    public void onPause() {
        // Cancela o retorno automático quando o usuário sai da confirmação.
        requireView().removeCallbacks(returnHome);
        returnHome = null;
        super.onPause();
    }
}
