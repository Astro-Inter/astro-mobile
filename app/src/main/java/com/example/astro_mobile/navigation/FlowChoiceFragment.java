package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.MockSession;

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
        // Ao escolher Colaborador, salva a Home como destino da sessão mock.
        view.findViewById(R.id.button_flow_choice_employee).setOnClickListener(clickedView -> {
            Bundle args = getArguments();
            if (args != null) {
                MockSession.save(requireContext(), args.getString(AuthArgs.EMAIL),
                        args.getString(AuthArgs.USER_TYPE),
                        MockSession.Destination.EMPLOYEE_HOME);
            }
            Navigation.findNavController(clickedView)
                    .navigate(R.id.action_flow_choice_to_employee_home,
                            AuthArgs.copy(args));
        });
    }
}
