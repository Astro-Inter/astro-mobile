package com.example.astro_mobile.employee.events;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;

public class EmployeeEventConclusionFragment extends Fragment {
    private Runnable returnHome;

    public EmployeeEventConclusionFragment() {
        super(R.layout.fragment_employee_event_conclusion);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Mantém o check por dois segundos e recria a Home para carregar o skeleton novamente.
        returnHome = () -> NavHostFragment.findNavController(this).navigate(
                R.id.action_employee_event_conclusion_to_home, AuthArgs.copy(getArguments()));
        requireView().postDelayed(returnHome, 2_000L);
    }

    @Override
    public void onPause() {
        // Cancela o retorno se o usuário sair da confirmação antes de completar a espera.
        requireView().removeCallbacks(returnHome);
        returnHome = null;
        super.onPause();
    }
}
