package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.astro_mobile.R;

/** Apresenta o erro; cada fluxo conserva seu próprio retorno e recuperação. */
public final class ErrorFragment extends Fragment {
    private AuthErrorRecovery authRecovery;

    public ErrorFragment() { super(R.layout.fragment_error); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navigation = NavHostFragment.findNavController(this);
        int destination = navigation.getCurrentDestination().getId();
        NavBackStackEntry previous = navigation.getPreviousBackStackEntry();
        int source = previous == null ? 0 : previous.getDestination().getId();
        if (!AuthenticatedErrorNavigation.bind(this, view, navigation, previous, source, destination)) {
            authRecovery = new AuthErrorRecovery(this);
            authRecovery.bind(view, navigation, source, destination);
        }
    }

    static void showCopy(TextView title, TextView body, boolean connection) {
        title.setText(connection ? R.string.email_connection_error_title : R.string.email_generic_error_title);
        body.setText(connection ? R.string.email_connection_error_body : R.string.email_generic_error_body);
    }

    static void configureBack(Fragment fragment, View view, Runnable back) {
        view.findViewById(R.id.button_email_error_start).setOnClickListener(clicked -> back.run());
        fragment.requireActivity().getOnBackPressedDispatcher().addCallback(fragment.getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() { back.run(); }
                });
    }

    @Override public void onDestroyView() {
        if (authRecovery != null) authRecovery.close();
        authRecovery = null;
        super.onDestroyView();
    }
}
