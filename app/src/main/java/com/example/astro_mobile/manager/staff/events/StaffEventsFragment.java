package com.example.astro_mobile.manager.staff.events;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
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
import com.example.astro_mobile.manager.staff.StaffDetailsFragment;

public final class StaffEventsFragment extends Fragment {
    private final int[] filterLabels = {R.string.employee_events_all, R.string.employee_events_pending,
            R.string.employee_events_done, R.string.staff_events_expired};
    private StaffEventsViewModel state;
    private Runnable finishLoading;

    public StaffEventsFragment() { super(R.layout.fragment_staff_events); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this).get(StaffEventsViewModel.class);

        // Mantém o nome real selecionado na lista; os eventos seguem mockados nesta tarefa.
        String name = requireArguments().getString(StaffDetailsFragment.ARG_NAME, "");
        ((TextView) view.findViewById(R.id.text_staff_events_heading)).setText(
                Html.fromHtml(getString(R.string.staff_events_heading, name), Html.FROM_HTML_MODE_LEGACY));

        // A seta retorna ao detalhe; o X fecha as telas internas e volta à Home.
        view.findViewById(R.id.button_staff_events_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_staff_events_close).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(R.id.managerHomeFragment, false));
        view.findViewById(R.id.button_staff_events_filter).setOnClickListener(clicked -> showFilters(view, clicked));
        render(view);

        // Espera local de um segundo para demonstrar o futuro carregamento sem bloquear a UI.
        if (!state.isLoaded()) {
            View content = view.findViewById(R.id.container_staff_events_content);
            View skeleton = view.findViewById(R.id.skeleton_staff_events);
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
        // Usa o mesmo menu nativo das outras listas; os estados só filtram os mocks.
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        for (int index = 0; index < filterLabels.length; index++) {
            menu.getMenu().add(0, index, index, filterLabels[index]);
        }
        menu.setOnMenuItemClickListener(item -> {
            state.setFilter(StaffEventsViewModel.Filter.values()[item.getItemId()]);
            render(view);
            return true;
        });
        menu.show();
    }

    private void render(View view) {
        RecyclerView list = view.findViewById(R.id.list_staff_events);
        list.setAdapter(new StaffEventsAdapter(state.visibleEvents(), clicked ->
                Toast.makeText(requireContext(), R.string.staff_events_details_pending, Toast.LENGTH_SHORT).show()));
        ((TextView) view.findViewById(R.id.button_staff_events_filter)).setText(
                state.getFilter() == StaffEventsViewModel.Filter.ALL
                        ? R.string.manager_events_filter : filterLabels[state.getFilter().ordinal()]);
    }

    @Override
    public void onDestroyView() {
        // Cancela a espera quando o usuário sai antes de o mock terminar.
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
