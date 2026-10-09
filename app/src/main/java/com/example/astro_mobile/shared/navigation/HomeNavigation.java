package com.example.astro_mobile.shared.navigation;

import android.content.Context;

import com.example.astro_mobile.R;
import com.example.astro_mobile.data.local.FlowPreferences;

public final class HomeNavigation {
    private HomeNavigation() { }

    public static int destination(Context context) {
        // As telas compartilhadas retornam à Home do fluxo escolhido, não ao tipo da conta.
        return FlowPreferences.wasManagerFlow(context)
                ? R.id.managerHomeFragment : R.id.employeeHomeFragment;
    }
}
