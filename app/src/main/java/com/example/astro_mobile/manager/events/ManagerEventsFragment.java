package com.example.astro_mobile.manager.events;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;
import com.example.astro_mobile.shared.ui.ResponsiveFilters;
import com.example.astro_mobile.shared.ui.PullToRefresh;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

import java.util.List;

public final class ManagerEventsFragment extends Fragment {
    private final int[] filterIds = {R.id.filter_manager_events_all,
            R.id.filter_manager_events_progress, R.id.filter_manager_events_finished,
            R.id.filter_manager_events_cancelled, R.id.filter_manager_events_expired};
    private final ManagerEventsViewModel.Filter[] filters = {
            ManagerEventsViewModel.Filter.ALL, ManagerEventsViewModel.Filter.IN_PROGRESS,
            ManagerEventsViewModel.Filter.FINISHED, ManagerEventsViewModel.Filter.CANCELLED,
            ManagerEventsViewModel.Filter.EXPIRED
    };
    private ManagerEventsViewModel state;

    public ManagerEventsFragment() { super(R.layout.fragment_manager_events); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ResponsiveFilters.bind(view.findViewById(R.id.container_manager_events_filters));
        PullToRefresh.attach(this, view.findViewById(R.id.container_manager_events_content),
                view.findViewById(R.id.list_manager_events), () -> loadEvents());
        state = new ViewModelProvider(this, new ManagerEventsViewModel.Factory()).get(ManagerEventsViewModel.class);

        // Voltar e fechar retornam à Home, sem mudar o fluxo ativo da conta.
        View.OnClickListener returnHome = clicked -> NavHostFragment.findNavController(this)
                .popBackStack(HomeNavigation.destination(requireContext()), false);
        view.findViewById(R.id.button_manager_events_back).setOnClickListener(returnHome);
        view.findViewById(R.id.button_manager_events_close).setOnClickListener(returnHome);
        for (int index = 0; index < filterIds.length; index++) {
            ManagerEventsViewModel.Filter filter = filters[index];
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

    }

    @Override public void onResume() {
        super.onResume();
        if (!state.isLoaded() && !state.isLoading()) loadEvents();
    }

    // Exibe o skeleton enquanto a lista é consultada e encaminha falhas à tela adequada.
    private void loadEvents() {
        View view = getView();
        if (view == null) return;
        View content = view.findViewById(R.id.container_manager_events_content);
        View skeleton = view.findViewById(R.id.skeleton_manager_events);
        content.setVisibility(View.INVISIBLE);
        skeleton.setVisibility(View.VISIBLE);
        state.load(new ManagerEventsViewModel.ResultCallback() {
            @Override public void onSuccess() {
                if (getView() != view || !isResumed()) return;
                skeleton.setVisibility(View.GONE);
                content.setVisibility(View.VISIBLE);
                render(view);
            }
            @Override public void onFailure(FailureKind failure) {
                if (getView() != view || !isResumed()) return;
                if (failure == FailureKind.SESSION) {
                    SessionNavigation.signOut(ManagerEventsFragment.this);
                } else {
                    NavHostFragment.findNavController(ManagerEventsFragment.this).navigate(
                            failure == FailureKind.CONNECTION ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
                }
            }
        });
    }

    private void render(View view) {
        List<ManagerEvent> events = state.visibleEvents();
        boolean empty = events.isEmpty();
        RecyclerView list = view.findViewById(R.id.list_manager_events);
        list.setAdapter(new ManagerEventsAdapter(events, this::openDetails));
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_manager_events_empty).setVisibility(empty ? View.VISIBLE : View.GONE);
        // Os filtros permanecem disponíveis mesmo quando não há resultados.
        for (int index = 0; index < filterIds.length; index++) {
            view.findViewById(filterIds[index]).setSelected(index == state.getFilter().ordinal());
        }
    }

    private void openDetails(ManagerEvent event) {
        // Leva o evento e seu modo ao detalhe, preservando o contexto da conta.
        Bundle args = AuthArgs.copy(getArguments());
        args.putLong("event_id", event.id);
        args.putString(ManagerEventDetailsFragment.EVENT_TITLE, event.title);
        args.putString(ManagerEventDetailsFragment.COMPLETION_MODE, event.completionMode.name());
        NavHostFragment.findNavController(this).navigate(R.id.action_manager_events_to_details, args);
    }

    @Override public void onPause() {
        state.cancel();
        super.onPause();
    }
}
