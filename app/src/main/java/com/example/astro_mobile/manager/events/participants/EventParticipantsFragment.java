package com.example.astro_mobile.manager.events.participants;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;
import com.example.astro_mobile.manager.events.ManagerEvent;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

import java.util.List;

public final class EventParticipantsFragment extends Fragment {
    private final int[] filterIds = {R.id.filter_participants_all,
            R.id.filter_participants_finished, R.id.filter_participants_not_finished};
    private EventParticipantsViewModel state;
    private String eventTitle;
    private Runnable finishLoading;

    public EventParticipantsFragment() { super(R.layout.fragment_event_participants); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        eventTitle = args.getString("event_title");
        ManagerEvent.CompletionMode mode = ManagerEvent.CompletionMode.valueOf(args.getString("event_completion_mode"));
        boolean selfDeclaration = mode == ManagerEvent.CompletionMode.SELF_DECLARATION;
        state = new ViewModelProvider(this).get(EventParticipantsViewModel.class);
        state.initialize(mode);

        // O evento e o modo são os mesmos do detalhe que abriu esta tela.
        String description = getString(R.string.participants_description, eventTitle);
        SpannableString styledDescription = new SpannableString(description);
        styledDescription.setSpan(new StyleSpan(Typeface.BOLD), description.indexOf('“'),
                description.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) view.findViewById(R.id.text_participants_description)).setText(styledDescription);
        view.findViewById(R.id.text_participants_assignment_notice)
                .setVisibility(selfDeclaration ? View.GONE : View.VISIBLE);
        ((TextView) view.findViewById(filterIds[1])).setText(selfDeclaration
                ? R.string.participants_validated : R.string.employee_events_done);
        ((TextView) view.findViewById(filterIds[2])).setText(selfDeclaration
                ? R.string.participants_not_validated : R.string.participants_not_completed);

        // Voltar mantém o detalhe; fechar retorna à Home do fluxo ativo.
        view.findViewById(R.id.button_participants_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_participants_close).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(HomeNavigation.destination(requireContext()), false));
        for (int index = 0; index < filterIds.length; index++) {
            EventParticipantsViewModel.Filter filter = EventParticipantsViewModel.Filter.values()[index];
            view.findViewById(filterIds[index]).setOnClickListener(clicked -> {
                state.setFilter(filter);
                render(view);
            });
        }
        EditText search = view.findViewById(R.id.input_participants_search);
        search.setText(state.getQuery());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                state.setQuery(s.toString());
                render(view);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        search.setOnEditorActionListener((input, action, event) -> {
            // A pesquisa é instantânea; o botão do teclado só fecha o teclado.
            InputMethodManager keyboard = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
            input.clearFocus();
            return true;
        });
        render(view);

        // Simula um segundo de espera; o skeleton fica oculto no editor de XML.
        if (!state.isLoaded()) {
            View content = view.findViewById(R.id.container_participants_content);
            View skeleton = view.findViewById(R.id.skeleton_event_participants);
            skeleton.findViewById(R.id.skeleton_participants_assignment_notice)
                    .setVisibility(selfDeclaration ? View.GONE : View.VISIBLE);
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

    private void render(View view) {
        // Atualiza a lista local, mantendo as diferenças dos filtros das duas variantes.
        List<EventParticipant> participants = state.visibleParticipants();
        RecyclerView list = view.findViewById(R.id.list_event_participants);
        list.setAdapter(new EventParticipantsAdapter(participants, eventTitle, clicked ->
                Toast.makeText(requireContext(), R.string.participants_validation_pending, Toast.LENGTH_SHORT).show()));
        boolean empty = participants.isEmpty();
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.text_participants_empty).setVisibility(empty ? View.VISIBLE : View.GONE);
        for (int index = 0; index < filterIds.length; index++) {
            view.findViewById(filterIds[index]).setSelected(index == state.getFilter().ordinal());
        }
    }

    @Override
    public void onDestroyView() {
        // Descarta a espera quando a tela é fechada antes do carregamento mock terminar.
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
