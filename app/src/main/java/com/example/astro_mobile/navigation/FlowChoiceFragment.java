package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.NavOptions;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.SessionViewModel;
import com.example.astro_mobile.data.local.FlowPreferences;

public class FlowChoiceFragment extends Fragment {

    public FlowChoiceFragment() {
        super(R.layout.fragment_flow_choice);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        WindowCompat.getInsetsController(requireActivity().getWindow(), view)
                .hide(WindowInsetsCompat.Type.ime());

        // O destino do Gestor ainda é mock e apenas informa que não está disponível.
        view.findViewById(R.id.button_flow_choice_manager).setOnClickListener(clickedView ->
                Toast.makeText(requireContext(), R.string.flow_choice_manager_mock,
                        Toast.LENGTH_SHORT).show());
        // Guarda apenas o fluxo preferido; a sessão é sempre mantida pelo Firebase.
        view.findViewById(R.id.button_flow_choice_employee).setOnClickListener(clickedView -> {
            Bundle args = getArguments();
            SessionViewModel sessionViewModel = new ViewModelProvider(requireActivity(),
                    SessionViewModel.Factory.createDefault()).get(SessionViewModel.class);
            if (sessionViewModel.hasCurrentUser()) {
                FlowPreferences.saveEmployeeFlow(requireContext());
            }
            Bundle destinationArgs = AuthArgs.copy(args);
            boolean switching = destinationArgs.getBoolean(AuthArgs.FROM_PROFILE);
            destinationArgs.remove(AuthArgs.FROM_PROFILE);
            // Trocar fluxo remove as telas antigas, sem repetir login ou duplicar a Home.
            if (switching) {
                Navigation.findNavController(clickedView).navigate(R.id.employeeHomeFragment,
                        destinationArgs, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .setEnterAnim(R.anim.push_enter).setExitAnim(R.anim.push_exit)
                                .setPopEnterAnim(R.anim.push_pop_enter).setPopExitAnim(R.anim.push_pop_exit)
                                .build());
            } else {
                Navigation.findNavController(clickedView)
                        .navigate(R.id.action_flow_choice_to_employee_home, destinationArgs);
            }
        });
    }
}
