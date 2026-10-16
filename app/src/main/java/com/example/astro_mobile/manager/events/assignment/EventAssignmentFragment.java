package com.example.astro_mobile.manager.events.assignment;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

public final class EventAssignmentFragment extends Fragment {
    private Runnable finishLoading;

    public EventAssignmentFragment() {
        super(R.layout.fragment_event_assignment);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();

        // Mostra o participante selecionado e mantém o título do evento que abriu a lista.
        int[] avatars = {R.drawable.participant_avatar_1,
                R.drawable.participant_avatar_2, R.drawable.participant_avatar_3};
        ((ImageView) view.findViewById(R.id.image_assignment_participant))
                .setImageResource(avatars[args.getInt("participant_avatar_index")]);
        ((TextView) view.findViewById(R.id.text_assignment_participant))
                .setText(args.getString("participant_name"));
        ((TextView) view.findViewById(R.id.text_assignment_event))
                .setText(args.getString("event_title"));

        // Voltar preserva a lista; o evento abre seu detalhe e o X retorna à Home.
        view.findViewById(R.id.button_assignment_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_assignment_event).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(R.id.managerEventDetailsFragment, false));
        view.findViewById(R.id.button_assignment_close).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(HomeNavigation.destination(requireContext()), false));

        // O protótipo não define o resultado deste botão. A atribuição real fica na SCRUM-286.
        view.findViewById(R.id.button_assignment_submit).setOnClickListener(clicked ->
                Toast.makeText(requireContext(), R.string.event_assignment_action_pending, Toast.LENGTH_SHORT).show());

        // Simula a espera de um segundo sem bloquear a tela; o XML mantém o conteúdo visível na prévia.
        View content = view.findViewById(R.id.container_assignment_content);
        View skeleton = view.findViewById(R.id.skeleton_event_assignment);
        content.setVisibility(View.INVISIBLE);
        skeleton.setVisibility(View.VISIBLE);
        finishLoading = () -> {
            skeleton.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
            finishLoading = null;
        };
        view.postDelayed(finishLoading, 1_000L);
    }

    @Override
    public void onDestroyView() {
        // Cancela a espera se a tela for fechada antes de mostrar os dados locais.
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
