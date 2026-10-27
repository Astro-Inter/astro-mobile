package com.example.astro_mobile.manager.forms;

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
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.manager.forms.details.FormDetailsFragment;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

import java.util.List;

public final class ManagerFormsFragment extends Fragment {
    private final int[] filterIds = {R.id.filter_manager_forms_all,
            R.id.filter_manager_forms_progress, R.id.filter_manager_forms_finished};
    private ManagerFormsViewModel state;


    public ManagerFormsFragment() { super(R.layout.fragment_manager_forms); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this, new ManagerFormsViewModel.Factory()).get(ManagerFormsViewModel.class);
        View.OnClickListener returnHome = clicked -> NavHostFragment.findNavController(this)
                .popBackStack(HomeNavigation.destination(requireContext()), false);
        view.findViewById(R.id.button_manager_forms_back).setOnClickListener(returnHome);
        view.findViewById(R.id.button_manager_forms_close).setOnClickListener(returnHome);
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

    }

    @Override public void onResume() {
        super.onResume();
        loadForms();
    }

    private void loadForms() {
        View view = getView();
        if (view == null || state.isLoading()) return;
        view.findViewById(R.id.container_manager_forms_content).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.skeleton_manager_forms).setVisibility(View.VISIBLE);
        state.load(new ManagerFormsViewModel.ResultCallback() {
            @Override public void onSuccess() {
                if (getView() != view || !isResumed()) return;
                view.findViewById(R.id.skeleton_manager_forms).setVisibility(View.GONE);
                view.findViewById(R.id.container_manager_forms_content).setVisibility(View.VISIBLE);
                render(view);
            }
            @Override public void onFailure(FailureKind failure) {
                if (getView() != view || !isResumed()) return;
                view.findViewById(R.id.skeleton_manager_forms).setVisibility(View.GONE);
                if (failure == FailureKind.SESSION) SessionNavigation.signOut(ManagerFormsFragment.this);
                else NavHostFragment.findNavController(ManagerFormsFragment.this).navigate(failure == FailureKind.CONNECTION
                                ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
            }
        });
    }

    private void render(View view) {
        List<ManagerForm> forms = state.visibleForms();
        RecyclerView list = view.findViewById(R.id.list_manager_forms);
        list.setAdapter(new ManagerFormsAdapter(forms, this::openDetails));
        list.setVisibility(forms.isEmpty() ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.text_manager_forms_empty).setVisibility(forms.isEmpty() ? View.VISIBLE : View.GONE);
        for (int index = 0; index < filterIds.length; index++) {
            view.findViewById(filterIds[index]).setSelected(index == state.getFilter().ordinal());
        }
    }

    private void openDetails(ManagerForm form) {
        // Leva o formulário selecionado e informa se ele exige um endereço específico.
        Bundle args = AuthArgs.copy(getArguments());
        args.putString("form_id", form.id);
        args.putString(FormDetailsFragment.FORM_TITLE, form.title);
        args.putString(FormDetailsFragment.DEADLINE, form.deadline);
        args.putBoolean(FormDetailsFragment.REQUIRES_UNIT_LOCATION, form.requiresUnitLocation);
        NavHostFragment.findNavController(this).navigate(R.id.action_manager_forms_to_details, args);
    }

    @Override public void onPause() {
        state.cancel();
        super.onPause();
    }
}
