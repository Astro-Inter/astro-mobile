package com.example.astro_mobile;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Prepara a splash nativa e permite desenhar sob as barras do sistema.
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(
                this,
                SystemBarStyle.dark(getColor(R.color.purple_gray)),
                SystemBarStyle.dark(getColor(R.color.astro_dark_background))
        );
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        View content = findViewById(R.id.main_content);
        View bottomNavigation = findViewById(R.id.container_main_bottom_nav);
        ImageView authenticationPlanetOverlay = findViewById(R.id.authentication_planet_overlay);
        View statusBarScrim = findViewById(R.id.status_bar_scrim);
        View navigationBarScrim = findViewById(R.id.navigation_bar_scrim);

        // Mantém o planeta de fundo apenas nas telas de autenticação.
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment == null ? null : navHostFragment.getNavController();
        if (navController != null) {
            configureBottomNavigation(navController, bottomNavigation);
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                boolean isAuthenticationScreen = destinationId == R.id.emailIdentificationFragment
                        || destinationId == R.id.accessKeyInformationFragment
                        || destinationId == R.id.loginPasswordFragment
                        || destinationId == R.id.firstLoginAccessKeyFragment
                        || destinationId == R.id.firstLoginPasswordFragment
                        || destinationId == R.id.flowChoiceFragment;
                authenticationPlanetOverlay.setVisibility(
                        isAuthenticationScreen ? View.VISIBLE : View.GONE
                );
                updateBottomNavigation(bottomNavigation, destinationId, ViewCompat.getRootWindowInsets(root));
                ViewCompat.requestApplyInsets(root);
            });
        }

        // Ajusta o conteúdo e os fundos às áreas ocupadas pelas barras do aparelho.
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), root);
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            boolean chat = navController != null && navController.getCurrentDestination() != null
                    && (navController.getCurrentDestination().getId() == R.id.chatFragment
                    || navController.getCurrentDestination().getId() == R.id.aiSessionsFragment);
            int bottom = chat ? Math.max(systemBars.bottom,
                    insets.getInsets(WindowInsetsCompat.Type.ime()).bottom) : systemBars.bottom;
            content.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    bottom
            );
            if (navController != null && navController.getCurrentDestination() != null) {
                updateBottomNavigation(bottomNavigation, navController.getCurrentDestination().getId(), insets);
            }
            updateOverlayBottomMargin(authenticationPlanetOverlay, systemBars.bottom);
            updateScrimHeight(statusBarScrim, systemBars.top, Gravity.TOP);
            updateScrimHeight(navigationBarScrim, systemBars.bottom, Gravity.BOTTOM);
            return insets;
        });
    }

    private void configureBottomNavigation(NavController navigation, View navbar) {
        // As abas usam uma única barra e preservam as telas que já estão na pilha.
        navbar.findViewById(R.id.button_conversations_nav_home).setOnClickListener(clicked ->
                navigation.popBackStack(HomeNavigation.destination(this), false));
        navbar.findViewById(R.id.button_conversations_nav_chat).setOnClickListener(clicked -> {
            if (navigation.getCurrentDestination().getId() == R.id.conversationsFragment) return;
            Bundle args = AuthArgs.copy(navigation.getCurrentBackStackEntry().getArguments());
            if (!navigation.popBackStack(R.id.conversationsFragment, false)) {
                navigation.navigate(R.id.action_global_conversations, args, new NavOptions.Builder()
                        .setPopUpTo(HomeNavigation.destination(this), false)
                        .setLaunchSingleTop(true)
                        .setEnterAnim(R.anim.push_enter).setExitAnim(R.anim.push_exit)
                        .setPopEnterAnim(R.anim.push_pop_enter).setPopExitAnim(R.anim.push_pop_exit)
                        .build());
            }
        });
        // Perfil ainda pertence à PR separada; mantém o aviso já existente na main.
        navbar.findViewById(R.id.button_conversations_nav_profile).setOnClickListener(clicked ->
                Toast.makeText(this, R.string.employee_home_mock_unavailable, Toast.LENGTH_SHORT).show());
        navbar.findViewById(R.id.button_conversations_nav_events).setOnClickListener(clicked ->
                Toast.makeText(this, R.string.employee_home_mock_unavailable, Toast.LENGTH_SHORT).show());
    }

    private void updateBottomNavigation(View navbar, int destination, WindowInsetsCompat insets) {
        // Só o conteúdo muda nas telas com navbar. No chat, ela se oculta durante o teclado.
        boolean home = destination == R.id.employeeHomeFragment || destination == R.id.managerHomeFragment;
        boolean conversations = destination == R.id.conversationsFragment || destination == R.id.chatFragment
                || destination == R.id.aiSessionsFragment;
        boolean keyboard = insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
        navbar.setVisibility((home || conversations)
                && !((destination == R.id.chatFragment || destination == R.id.aiSessionsFragment) && keyboard)
                ? View.VISIBLE : View.GONE);
        navbar.findViewById(R.id.button_conversations_nav_home).setSelected(home);
        navbar.findViewById(R.id.button_conversations_nav_chat).setSelected(conversations);
        navbar.findViewById(R.id.button_conversations_nav_profile).setSelected(false);
    }

    private void updateScrimHeight(View scrim, int height, int gravity) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) scrim.getLayoutParams();
        if (params.height == height && params.gravity == gravity) {
            return;
        }
        params.height = height;
        params.gravity = gravity;
        scrim.setLayoutParams(params);
    }

    private void updateOverlayBottomMargin(View overlay, int bottomInset) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) overlay.getLayoutParams();
        if (params.bottomMargin == bottomInset) {
            return;
        }
        params.bottomMargin = bottomInset;
        overlay.setLayoutParams(params);
    }
}
