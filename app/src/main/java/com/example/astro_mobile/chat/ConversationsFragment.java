package com.example.astro_mobile.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.data.local.FlowPreferences;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

public final class ConversationsFragment extends Fragment {

    public ConversationsFragment() {
        super(R.layout.fragment_conversations);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navigation = NavHostFragment.findNavController(this);

        // O Gestor pode usar o fluxo de Colaborador; o texto acompanha o fluxo ativo.
        ((TextView) view.findViewById(R.id.text_conversations_subtitle)).setText(
                FlowPreferences.wasManagerFlow(requireContext())
                        ? R.string.conversations_manager_subtitle : R.string.conversations_subtitle);

        // Voltar e fechar retornam à Home que já está na pilha; a Activity controla as abas.
        View.OnClickListener returnHome = clicked ->
                navigation.popBackStack(HomeNavigation.destination(requireContext()), false);
        view.findViewById(R.id.button_conversations_back).setOnClickListener(returnHome);
        view.findViewById(R.id.button_conversations_close).setOnClickListener(returnHome);

        // A IA fixada abre sua lista de sessões; o mascote mantém acesso direto ao chat.
        view.findViewById(R.id.button_conversations_ai).setOnClickListener(clicked ->
                navigation.navigate(R.id.action_conversations_to_ai_sessions, AuthArgs.copy(getArguments())));

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

        // A busca ainda aguarda sua funcionalidade.
        view.findViewById(R.id.button_conversations_search).setOnClickListener(clicked ->
                Toast.makeText(requireContext(), R.string.employee_home_mock_unavailable,
                        Toast.LENGTH_SHORT).show());
    }
}
