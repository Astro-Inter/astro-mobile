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
            Navigation.findNavController(clickedView)
                    .navigate(R.id.action_flow_choice_to_employee_home,
                            AuthArgs.copy(args));
        });
    }
}
