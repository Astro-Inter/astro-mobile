package com.example.astro_mobile.manager.events;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

import java.util.List;

public final class ManagerEventsFragment extends Fragment {
    private final int[] filterIds = {R.id.filter_manager_events_all,
            R.id.filter_manager_events_progress, R.id.filter_manager_events_finished};
    private final int[] filterLabels = {R.string.employee_events_all,
            R.string.manager_events_in_progress, R.string.manager_events_finished};
    private ManagerEventsViewModel state;
    private Runnable finishLoading;

    public ManagerEventsFragment() { super(R.layout.fragment_manager_events); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this).get(ManagerEventsViewModel.class);

        // Voltar e fechar retornam à Home, sem mudar o fluxo ativo da conta.
        View.OnClickListener returnHome = clicked -> NavHostFragment.findNavController(this)
                .popBackStack(HomeNavigation.destination(requireContext()), false);
        view.findViewById(R.id.button_manager_events_back).setOnClickListener(returnHome);
        view.findViewById(R.id.button_manager_events_close).setOnClickListener(returnHome);
        view.findViewById(R.id.button_manager_events_filter).setOnClickListener(clicked -> showFilters(view, clicked));
        for (int index = 0; index < filterIds.length; index++) {
            ManagerEventsViewModel.Filter filter = ManagerEventsViewModel.Filter.values()[index];
            view.findViewById(filterIds[index]).setOnClickListener(clicked -> {
                state.setFilter(filter);
                render(view);
            });
        }
        EditText search = view.findViewById(R.id.input_manager_events_search);
        search.setText(state.getQuery());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // A pesquisa atualiza a lista ao digitar; não envia requisições.
                state.setQuery(s.toString());
                render(view);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        search.setOnEditorActionListener((input, action, event) -> {
            InputMethodManager keyboard = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
            input.clearFocus();
            return true;
        });
        render(view);

        // Simula um segundo de espera sem bloquear a interface; o XML mantém a prévia real.
        if (!state.isLoaded()) {
            View content = view.findViewById(R.id.container_manager_events_content);
            View skeleton = view.findViewById(R.id.skeleton_manager_events);
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

    private void showFilters(View view, View anchor) {
        // O seletor e os chips da tela vazia usam os mesmos três filtros do Figma.
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        for (int index = 0; index < filterLabels.length; index++) {
            menu.getMenu().add(0, index, index, filterLabels[index]);
        }
        menu.setOnMenuItemClickListener(item -> {
            state.setFilter(ManagerEventsViewModel.Filter.values()[item.getItemId()]);
            render(view);
            return true;
        });
        menu.show();
    }

    private void render(View view) {
        List<ManagerEvent> events = state.visibleEvents();
        boolean empty = events.isEmpty();
        RecyclerView list = view.findViewById(R.id.list_manager_events);
        list.setAdapter(new ManagerEventsAdapter(events, event -> Toast.makeText(requireContext(),
                R.string.manager_events_details_pending, Toast.LENGTH_SHORT).show()));
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_manager_events_empty).setVisibility(empty ? View.VISIBLE : View.GONE);
        // Preserva as duas composições: dropdown na lista e chips no estado vazio.
        view.findViewById(R.id.container_manager_events_dropdown).setVisibility(empty ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_manager_events_filters).setVisibility(empty ? View.VISIBLE : View.GONE);
        TextView filterText = view.findViewById(R.id.button_manager_events_filter);
        filterText.setText(state.getFilter() == ManagerEventsViewModel.Filter.ALL
                ? R.string.manager_events_filter : filterLabels[state.getFilter().ordinal()]);
        for (int index = 0; index < filterIds.length; index++) {
            view.findViewById(filterIds[index]).setSelected(index == state.getFilter().ordinal());
        }
    }

    @Override
    public void onDestroyView() {
        // Descarta o carregamento mock se a tela for fechada antes de completar a espera.
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
