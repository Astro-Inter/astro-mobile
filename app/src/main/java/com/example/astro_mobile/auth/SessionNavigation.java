package com.example.astro_mobile.auth;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.data.local.FlowPreferences;

public final class SessionNavigation {
    private SessionNavigation() { }

    public static void signOut(Fragment fragment) {
        // Encerra Firebase, limpa os estados antigos e remove toda a pilha autenticada.
        new ViewModelProvider(fragment.requireActivity(), SessionViewModel.Factory.createDefault())
                .get(SessionViewModel.class).signOut();
        FlowPreferences.clear(fragment.requireContext());
        MockSession.clear(fragment.requireContext());
        new ViewModelProvider(fragment.requireActivity(), EmailVerificationViewModel.Factory.createDefault())
                .get(EmailVerificationViewModel.class).clearForLogout();
        new ViewModelProvider(fragment.requireActivity(), new AccessKeyViewModel.Factory())
                .get(AccessKeyViewModel.class).clearForLogout();
        NavHostFragment.findNavController(fragment).navigate(R.id.emailIdentificationFragment,
                null, new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build());
    }
}
