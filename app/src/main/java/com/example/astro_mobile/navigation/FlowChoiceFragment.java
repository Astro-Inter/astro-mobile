package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.data.local.FlowPreferences;

public class FlowChoiceFragment extends Fragment {
    public FlowChoiceFragment() { super(R.layout.fragment_flow_choice); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WindowCompat.getInsetsController(requireActivity().getWindow(), view)
                .hide(WindowInsetsCompat.Type.ime());
        view.findViewById(R.id.button_flow_choice_manager).setOnClickListener(clicked -> openHome(clicked, true));
        view.findViewById(R.id.button_flow_choice_employee).setOnClickListener(clicked -> openHome(clicked, false));
    }

    private void openHome(View clicked, boolean manager) {
        // Guarda somente o fluxo escolhido; a sessão continua sendo mantida pelo Firebase.
        if (manager) FlowPreferences.saveManagerFlow(requireContext());
        else FlowPreferences.saveEmployeeFlow(requireContext());
        Navigation.findNavController(clicked).navigate(manager
                        ? R.id.action_flow_choice_to_manager_home : R.id.action_flow_choice_to_employee_home,
                AuthArgs.copy(getArguments()));
    }
}
