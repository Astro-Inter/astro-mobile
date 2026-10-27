package com.example.astro_mobile.manager.events;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

public final class ManagerEventDetailsFragment extends Fragment {
    static final String EVENT_TITLE = "event_title";
    static final String COMPLETION_MODE = "event_completion_mode";
    private Runnable finishLoading;

    public ManagerEventDetailsFragment() { super(R.layout.fragment_manager_event_details); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ManagerEventDetailsViewModel state = new ViewModelProvider(this)
                .get(ManagerEventDetailsViewModel.class);
        Bundle args = requireArguments();
        boolean selfDeclaration = ManagerEvent.CompletionMode.valueOf(args.getString(COMPLETION_MODE))
                == ManagerEvent.CompletionMode.SELF_DECLARATION;

        // O modo vem do evento escolhido, não é inferido pelo estado da lista.
        ((TextView) view.findViewById(R.id.text_manager_event_title)).setText(args.getString(EVENT_TITLE));
        ((TextView) view.findViewById(R.id.text_manager_event_subtitle)).setText(selfDeclaration
                ? R.string.manager_event_self_declaration : R.string.manager_event_assignment);
        ((TextView) view.findViewById(R.id.text_manager_event_mode)).setText(selfDeclaration
                ? R.string.employee_event_self_declaration : R.string.manager_event_assignment_mode);
        view.findViewById(R.id.container_manager_event_validations)
                .setVisibility(selfDeclaration ? View.VISIBLE : View.GONE);

        // Voltar preserva a lista; fechar retorna à Home do fluxo ativo.
        view.findViewById(R.id.button_manager_event_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_manager_event_close).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(HomeNavigation.destination(requireContext()), false));

        // O anexo continua visual; participantes abre a variante correspondente ao modo.
        view.findViewById(R.id.button_manager_event_attachment).setOnClickListener(clicked ->
                showMockNotice(R.string.employee_event_attachment_mock));
        view.findViewById(R.id.button_manager_event_participants).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).navigate(
                        R.id.action_manager_event_details_to_participants, AuthArgs.copy(getArguments())));

        // Mostra o skeleton por um segundo sem bloquear a interface ou ocultar a prévia XML.
        if (!state.isLoaded()) {
            View content = view.findViewById(R.id.scroll_manager_event_details);
            View skeleton = view.findViewById(R.id.skeleton_manager_event_details);
            skeleton.findViewById(R.id.skeleton_manager_event_validations)
                    .setVisibility(selfDeclaration ? View.VISIBLE : View.GONE);
            content.setVisibility(View.INVISIBLE);
            skeleton.setVisibility(View.VISIBLE);
            finishLoading = () -> {
                state.markLoaded();
                skeleton.setVisibility(View.GONE);
                content.setVisibility(View.VISIBLE);
                finishLoading = null;
            };
            view.postDelayed(finishLoading, 1_000L);
        }
    }

    private void showMockNotice(int message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        // Descarta a espera caso o usuário feche o detalhe antes de ela terminar.
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
