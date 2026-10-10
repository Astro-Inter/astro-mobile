package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.NavOptions;

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
        // Guarda o fluxo escolhido; ele muda a Home, mas não altera o perfil da conta.
        if (manager) FlowPreferences.saveManagerFlow(requireContext());
        else FlowPreferences.saveEmployeeFlow(requireContext());
        Bundle args = AuthArgs.copy(getArguments());
        boolean switching = args.getBoolean(AuthArgs.FROM_PROFILE);
        args.remove(AuthArgs.FROM_PROFILE);
        // Ao trocar pelo Perfil, remove o fluxo anterior sem repetir o login.
        if (switching) {
            Navigation.findNavController(clicked).navigate(manager
                            ? R.id.managerHomeFragment : R.id.employeeHomeFragment,
                    args, new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true)
                            .setEnterAnim(R.anim.push_enter).setExitAnim(R.anim.push_exit)
                            .setPopEnterAnim(R.anim.push_pop_enter).setPopExitAnim(R.anim.push_pop_exit)
                            .build());
        } else {
            Navigation.findNavController(clicked).navigate(manager
                    ? R.id.action_flow_choice_to_manager_home : R.id.action_flow_choice_to_employee_home, args);
        }
    }
}
