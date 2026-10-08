package com.example.astro_mobile.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;

public final class ConversationsFragment extends Fragment {

    public ConversationsFragment() {
        super(R.layout.fragment_conversations);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navigation = NavHostFragment.findNavController(this);

        // Voltar, fechar e Home retornam à Home que já está na pilha.
        View.OnClickListener returnHome = clicked -> navigation.popBackStack();
        view.findViewById(R.id.button_conversations_back).setOnClickListener(returnHome);
        view.findViewById(R.id.button_conversations_close).setOnClickListener(returnHome);
        view.findViewById(R.id.button_conversations_nav_home).setOnClickListener(returnHome);

        // A IA fixada abre sua lista de sessões; o mascote mantém acesso direto ao chat.
        view.findViewById(R.id.button_conversations_ai).setOnClickListener(clicked ->
                navigation.navigate(R.id.action_conversations_to_ai_sessions));

        // Nesta etapa, os filtros alternam apenas a seleção visual.
        int[] filterIds = {R.id.button_conversations_filter_all,
                R.id.button_conversations_filter_employees, R.id.button_conversations_filter_managers};
        view.findViewById(R.id.button_conversations_filter_all).setSelected(true);
        for (int filterId : filterIds) {
            view.findViewById(filterId).setOnClickListener(clicked -> {
                for (int id : filterIds) {
                    view.findViewById(id).setSelected(id == clicked.getId());
                }
            });
        }

        // Busca e outras abas ainda aguardam suas funcionalidades.
        int[] pendingIds = {R.id.button_conversations_search,
                R.id.button_conversations_nav_events, R.id.button_conversations_nav_profile};
        for (int id : pendingIds) {
            view.findViewById(id).setOnClickListener(clicked ->
                    Toast.makeText(requireContext(), R.string.employee_home_mock_unavailable,
                            Toast.LENGTH_SHORT).show());
        }
    }
}
