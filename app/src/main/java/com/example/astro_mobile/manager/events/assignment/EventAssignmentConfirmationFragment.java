package com.example.astro_mobile.manager.events.assignment;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.astro_mobile.R;

/** Mostra o sucesso e volta à lista que será atualizada. */
public final class EventAssignmentConfirmationFragment extends Fragment {
    private Runnable returnToParticipants;
    public EventAssignmentConfirmationFragment() { super(R.layout.fragment_employee_event_conclusion); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) view.findViewById(R.id.text_event_confirmation)).setText(R.string.event_assignment_success);
        returnToParticipants = () -> NavHostFragment.findNavController(this).popBackStack(R.id.eventParticipantsFragment, false);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { returnToParticipants.run(); }
        });
        view.postDelayed(returnToParticipants, 2_000L);
    }
    @Override public void onDestroyView() {
        if (returnToParticipants != null) requireView().removeCallbacks(returnToParticipants);
        returnToParticipants = null;
        super.onDestroyView();
    }
}
