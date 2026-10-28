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
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.api.FailureKind;

import java.util.List;

public final class StaffFragment extends Fragment {
    private StaffViewModel state;

    public StaffFragment() { super(R.layout.fragment_staff); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this, StaffViewModel.Factory.createDefault())
                .get(StaffViewModel.class);

        // Voltar preserva a Home; o X retorna diretamente à Home do Gestor.
        view.findViewById(R.id.button_staff_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_staff_close).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(R.id.managerHomeFragment, false));
        int[] filters = { R.id.filter_staff_all, R.id.filter_staff_pending, R.id.filter_staff_no_pending };
        for (int i = 0; i < filters.length; i++) {
            final int selected = i;
            View filterView = view.findViewById(filters[i]);
            filterView.setSelected(state.getFilter() == i);
            filterView.setOnClickListener(clicked -> {
                state.setFilter(selected);
                for (int id : filters) view.findViewById(id).setSelected(id == filters[selected]);
                render(view);
            });
        }

        // A busca acontece ao digitar e usa a lista já recebida.
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
            InputMethodManager keyboard = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
            input.clearFocus();
            return true;
        });

        render(view);
        if (state.isLoaded()) showContent(view);
        else loadCollaborators(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (state == null || getView() == null) return;
        Boolean retry = NavHostFragment.findNavController(this).getCurrentBackStackEntry()
                .getSavedStateHandle().remove("retry_staff_list");
        if (Boolean.TRUE.equals(retry)) loadCollaborators(getView());
    }

    private void loadCollaborators(View view) {
        if (state.isLoading()) return;
        view.findViewById(R.id.container_staff_content).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.skeleton_staff).setVisibility(View.VISIBLE);
        state.load(new StaffViewModel.ResultCallback() {
            @Override public void onSuccess() {
                if (getView() != view || !isResumed()) return;
                showContent(view);
                render(view);
            }

            @Override public void onFailure(FailureKind failure) {
                if (getView() != view || !isResumed()) return;
                view.findViewById(R.id.skeleton_staff).setVisibility(View.GONE);
                if (failure == FailureKind.SESSION) {
                    SessionNavigation.signOut(StaffFragment.this);
                } else {
                    NavHostFragment.findNavController(StaffFragment.this)
                            .navigate(failure == FailureKind.CONNECTION
                                    ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
                }
            }
        });
    }

    private void showContent(View view) {
        view.findViewById(R.id.skeleton_staff).setVisibility(View.GONE);
        view.findViewById(R.id.container_staff_content).setVisibility(View.VISIBLE);
    }

    private void render(View view) {
        // Passa a identidade retornada pelo endpoint; o detalhe completo é outra tarefa.
        List<StaffMember> members = state.visibleMembers();
        RecyclerView list = view.findViewById(R.id.list_staff);
        list.setAdapter(new StaffAdapter(members, member -> {
            Bundle args = AuthArgs.copy(getArguments());
            args.putLong(StaffDetailsFragment.ARG_COLLABORATOR_ID, member.id);
            args.putString(StaffDetailsFragment.ARG_NAME, member.name);
            args.putString(StaffDetailsFragment.ARG_ROLE, member.role);
            args.putString(StaffDetailsFragment.ARG_EMAIL, member.email);
            args.putString(StaffDetailsFragment.ARG_UNIT, member.unit);
            args.putString(StaffDetailsFragment.ARG_PHOTO_URL, member.photoUrl);
            NavHostFragment.findNavController(this).navigate(R.id.action_staff_to_details, args);
        }));
        list.setVisibility(members.isEmpty() ? View.GONE : View.VISIBLE);
        android.widget.TextView empty = view.findViewById(R.id.text_staff_empty);
        empty.setVisibility(members.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(state.hasMembers() ? R.string.staff_no_filter_results : R.string.staff_empty);
    }
}
