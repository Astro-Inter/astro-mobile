package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.astro_mobile.R;

public class FlowChoiceFragment extends Fragment {

    public FlowChoiceFragment() {
        super(R.layout.fragment_flow_choice);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        WindowCompat.getInsetsController(requireActivity().getWindow(), view)
                .hide(WindowInsetsCompat.Type.ime());

        view.findViewById(R.id.button_flow_choice_manager).setOnClickListener(clickedView ->
                Toast.makeText(requireContext(), R.string.flow_choice_manager_mock,
                        Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.button_flow_choice_employee).setOnClickListener(clickedView ->
                Toast.makeText(requireContext(), R.string.flow_choice_employee_mock,
                        Toast.LENGTH_SHORT).show());
    }
}
