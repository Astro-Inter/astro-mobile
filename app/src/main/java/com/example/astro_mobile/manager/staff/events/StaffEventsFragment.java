package com.example.astro_mobile.manager.staff.events;

import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

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
import com.example.astro_mobile.manager.staff.StaffDetailsFragment;
import com.example.astro_mobile.manager.events.ManagerEventDetailsFragment;

public final class StaffEventsFragment extends Fragment {
    private final int[] filterIds = {R.id.filter_staff_events_0, R.id.filter_staff_events_1,
            R.id.filter_staff_events_2, R.id.filter_staff_events_3, R.id.filter_staff_events_4,
            R.id.filter_staff_events_5, R.id.filter_staff_events_6};
    private StaffEventsViewModel state;

    public StaffEventsFragment() { super(R.layout.fragment_staff_events); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ResponsiveFilters.bind(view.findViewById(R.id.container_staff_events_dropdown));
        PullToRefresh.attach(this, view.findViewById(R.id.container_staff_events_content),
                view.findViewById(R.id.list_staff_events), () -> load(view));
        state = new ViewModelProvider(this, new StaffEventsViewModel.Factory())
                .get(StaffEventsViewModel.class);

        // A seta retorna ao detalhe; o X fecha as telas internas e volta à Home.
        view.findViewById(R.id.button_staff_events_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_staff_events_close).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(R.id.managerHomeFragment, false));
        for (int index = 0; index < filterIds.length; index++) {
            final StaffEventsViewModel.Filter filter = StaffEventsViewModel.Filter.values()[index];
            view.findViewById(filterIds[index]).setOnClickListener(clicked -> {
                state.setFilter(filter);
                render(view);
            });
        }
        render(view);
        if (!state.isLoaded()) load(view);
        else showContent(view);
    }

    @Override public void onResume() {
        super.onResume();
        Boolean retry = NavHostFragment.findNavController(this).getCurrentBackStackEntry()
                .getSavedStateHandle().remove("retry_staff_events");
        if (Boolean.TRUE.equals(retry) && getView() != null) load(getView());
    }

    private void load(View view) {
        // O skeleton acompanha a consulta real, sem espera artificial.
        view.findViewById(R.id.container_staff_events_content).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.skeleton_staff_events).setVisibility(View.VISIBLE);
        long collaboratorId = requireArguments().getLong(StaffDetailsFragment.ARG_COLLABORATOR_ID);
        state.load(collaboratorId, new StaffEventsViewModel.ResultCallback() {
            @Override public void onSuccess() {
                if (getView() != view || !isAdded()) return;
                render(view);
                showContent(view);
            }

            @Override public void onFailure(FailureKind failure) {
                if (getView() != view || !isAdded()) return;
                if (failure == FailureKind.SESSION) {
                    SessionNavigation.signOut(StaffEventsFragment.this);
                } else {
                    NavHostFragment.findNavController(StaffEventsFragment.this).navigate(
                            failure == FailureKind.CONNECTION
                                    ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
                }
            }
        });
    }

    private void showContent(View view) {
        view.findViewById(R.id.skeleton_staff_events).setVisibility(View.GONE);
        view.findViewById(R.id.container_staff_events_content).setVisibility(View.VISIBLE);
    }

    private void render(View view) {
        String name = state.getCollaboratorName();
        if (name == null) name = requireArguments().getString(StaffDetailsFragment.ARG_NAME, "");
        ((TextView) view.findViewById(R.id.text_staff_events_heading)).setText(
                Html.fromHtml(getString(R.string.staff_events_heading, TextUtils.htmlEncode(name)),
                        Html.FROM_HTML_MODE_LEGACY));
        RecyclerView list = view.findViewById(R.id.list_staff_events);
        java.util.List<StaffEvent> visibleEvents = state.visibleEvents();
        list.setAdapter(new StaffEventsAdapter(visibleEvents, this::openDetails));
        list.setVisibility(visibleEvents.isEmpty() ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.text_staff_events_empty).setVisibility(
                visibleEvents.isEmpty() ? View.VISIBLE : View.GONE);
        for (int index = 0; index < filterIds.length; index++)
            view.findViewById(filterIds[index]).setSelected(index == state.getFilter().ordinal());
    }

    private void openDetails(StaffEvent event) {
        // Reutiliza o detalhe do Gestor e sua escolha de endpoint por modalidade.
        Bundle args = AuthArgs.copy(getArguments());
        args.putLong("event_id", event.id);
        args.putString(ManagerEventDetailsFragment.EVENT_TITLE, event.title);
        args.putString(ManagerEventDetailsFragment.COMPLETION_MODE, event.completionMode.name());
        NavHostFragment.findNavController(this).navigate(R.id.action_staff_events_to_details, args);
    }

    @Override
    public void onDestroyView() {
        state.cancel();
        super.onDestroyView();
    }
}
