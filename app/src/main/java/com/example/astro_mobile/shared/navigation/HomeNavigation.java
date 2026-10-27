package com.example.astro_mobile.shared.navigation;

import android.content.Context;

import com.example.astro_mobile.R;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.astro_mobile.data.local.FlowPreferences;

public final class HomeNavigation {
    public static final String RESTORE_LOADED_HOME = "restore_loaded_home";

    private HomeNavigation() { }

    public static void returnToLoadedHome(Fragment fragment) {
        NavController navigation = NavHostFragment.findNavController(fragment);
        int home = destination(fragment.requireContext());
        navigation.getBackStackEntry(home).getSavedStateHandle().set(RESTORE_LOADED_HOME, true);
        navigation.popBackStack(home, false);
    }

    public static int destination(Context context) {
        // As telas compartilhadas retornam à Home do fluxo escolhido, não ao tipo da conta.
        return FlowPreferences.wasManagerFlow(context)
                ? R.id.managerHomeFragment : R.id.employeeHomeFragment;
    }
}
