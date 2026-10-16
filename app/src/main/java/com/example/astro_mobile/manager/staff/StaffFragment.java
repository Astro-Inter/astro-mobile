package com.example.astro_mobile.manager.staff;

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

import java.util.List;

public final class StaffFragment extends Fragment {
    private final int[] filterIds = {R.id.filter_staff_all,
            R.id.filter_staff_pending, R.id.filter_staff_no_pending};
    private StaffViewModel state;
    private Runnable finishLoading;

    public StaffFragment() { super(R.layout.fragment_staff); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this).get(StaffViewModel.class);

        // Voltar preserva a Home; o X retorna diretamente à Home do Gestor.
        view.findViewById(R.id.button_staff_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_staff_close).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(R.id.managerHomeFragment, false));

        // A pesquisa e os filtros só alteram a lista local, sem requisições.
        for (int index = 0; index < filterIds.length; index++) {
            StaffViewModel.Filter filter = StaffViewModel.Filter.values()[index];
            view.findViewById(filterIds[index]).setOnClickListener(clicked -> {
                state.setFilter(filter);
                render(view);
            });
        }
        EditText search = view.findViewById(R.id.input_staff_search);
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
            InputMethodManager keyboard = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
            input.clearFocus();
            return true;
        });
        render(view);

        // Simula um segundo de espera; a prévia XML continua mostrando a lista real.
        if (!state.isLoaded()) {
            View content = view.findViewById(R.id.container_staff_content);
            View skeleton = view.findViewById(R.id.skeleton_staff);
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
        // Abre o detalhe do exemplo escolhido sem perder a pesquisa e o filtro da lista.
        List<StaffMember> members = state.visibleMembers();
        RecyclerView list = view.findViewById(R.id.list_staff);
        list.setAdapter(new StaffAdapter(members, member -> {
            Bundle args = AuthArgs.copy(getArguments());
            args.putInt(StaffDetailsFragment.ARG_SAMPLE_INDEX, member.avatarIndex);
            NavHostFragment.findNavController(this).navigate(R.id.action_staff_to_details, args);
        }));
        list.setVisibility(members.isEmpty() ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.text_staff_empty).setVisibility(members.isEmpty() ? View.VISIBLE : View.GONE);
        for (int index = 0; index < filterIds.length; index++) {
            view.findViewById(filterIds[index]).setSelected(index == state.getFilter().ordinal());
        }
    }

    @Override
    public void onDestroyView() {
        // Cancela a espera se a tela for fechada antes de o mock terminar de carregar.
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
