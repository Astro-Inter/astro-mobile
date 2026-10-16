package com.example.astro_mobile.manager.forms;

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

public final class ManagerFormsFragment extends Fragment {
    private final int[] filterIds = {R.id.filter_manager_forms_all,
            R.id.filter_manager_forms_progress, R.id.filter_manager_forms_finished};
    private ManagerFormsViewModel state;
    private Runnable finishLoading;

    public ManagerFormsFragment() { super(R.layout.fragment_manager_forms); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this).get(ManagerFormsViewModel.class);
        View.OnClickListener returnHome = clicked -> NavHostFragment.findNavController(this)
                .popBackStack(HomeNavigation.destination(requireContext()), false);
        view.findViewById(R.id.button_manager_forms_back).setOnClickListener(returnHome);
        view.findViewById(R.id.button_manager_forms_close).setOnClickListener(returnHome);
        view.findViewById(R.id.button_manager_forms_nr_filter).setOnClickListener(clicked -> showNrFilters(view, clicked));
        for (int index = 0; index < filterIds.length; index++) {
            ManagerFormsViewModel.Filter filter = ManagerFormsViewModel.Filter.values()[index];
            view.findViewById(filterIds[index]).setOnClickListener(clicked -> {
                state.setFilter(filter);
                render(view);
            });
        }

        EditText search = view.findViewById(R.id.input_manager_forms_search);
        search.setText(state.getQuery());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                state.setQuery(text.toString());
                render(view);
            }
            @Override public void afterTextChanged(Editable text) { }
        });
        search.setOnEditorActionListener((input, action, event) -> {
            InputMethodManager keyboard = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
            input.clearFocus();
            return true;
        });
        render(view);

        // O atraso de 1s representa a espera pelos dados enquanto a API não está integrada.
        if (!state.isLoaded()) {
            View content = view.findViewById(R.id.container_manager_forms_content);
            View skeleton = view.findViewById(R.id.skeleton_manager_forms);
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

    private void showNrFilters(View view, View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 0, 0, R.string.manager_forms_all_nrs);
        menu.getMenu().add(0, 1, 1, R.string.manager_forms_nr_5);
        menu.setOnMenuItemClickListener(item -> {
            state.setSelectedNr(item.getItemId() == 0 ? "" : "NR - 5");
            render(view);
            return true;
        });
        menu.show();
    }

    private void render(View view) {
        List<ManagerForm> forms = state.visibleForms();
        RecyclerView list = view.findViewById(R.id.list_manager_forms);
        list.setAdapter(new ManagerFormsAdapter(forms, this::showDetailsPending));
        list.setVisibility(forms.isEmpty() ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.text_manager_forms_empty).setVisibility(forms.isEmpty() ? View.VISIBLE : View.GONE);
        for (int index = 0; index < filterIds.length; index++) {
            view.findViewById(filterIds[index]).setSelected(index == state.getFilter().ordinal());
        }
        TextView nrFilter = view.findViewById(R.id.button_manager_forms_nr_filter);
        nrFilter.setText(state.getSelectedNr().isEmpty() ? R.string.manager_forms_filter_nr
                : R.string.manager_forms_nr_5);
    }

    private void showDetailsPending(ManagerForm form) {
        Toast.makeText(requireContext(), R.string.manager_forms_details_pending, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        if (finishLoading != null && getView() != null) getView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
