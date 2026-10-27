package com.example.astro_mobile;

import android.os.Bundle;
import android.os.Build;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
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
import com.example.astro_mobile.chat.human.HumanConversationsViewModel;
import com.example.astro_mobile.shared.navigation.HomeNavigation;
import com.example.astro_mobile.shared.notifications.PendingEventNotifications;
import com.example.astro_mobile.shared.notifications.PushRegistrationViewModel;

public class MainActivity extends AppCompatActivity {
    private PushRegistrationViewModel pushRegistration;
    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> { if (granted) pushRegistration.synchronize(); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Prepara a splash nativa e permite desenhar sob as barras do sistema.
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        pushRegistration = new ViewModelProvider(this, PushRegistrationViewModel.Factory.createDefault(this))
                .get(PushRegistrationViewModel.class);
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
                if (destinationId == R.id.employeeHomeFragment || destinationId == R.id.managerHomeFragment) {
                    registerPush();
                    // Aguarda a autenticação e a escolha do fluxo antes de abrir a lista de eventos.
                    root.post(() -> openPushIfReady(controller));
                }
                if (destinationId == R.id.emailIdentificationFragment) {
                    new ViewModelProvider(this).get(HumanConversationsViewModel.class).clearSession();
                }
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
            // Todo campo recebe espaço para o teclado, sem depender de uma lista de telas.
            int bottom = Math.max(systemBars.bottom, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
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

    @Override protected void onResume() {
        super.onResume();
        if (pushRegistration != null) pushRegistration.synchronize();
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (host != null) openPushIfReady(host.getNavController());
    }
    private void registerPush() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            pushRegistration.synchronize();
        } else if (pushRegistration.mayAskPermission()) {
            pushRegistration.permissionRequested();
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
    private void openPushIfReady(NavController controller) {
        String uid = getIntent().getStringExtra(PendingEventNotifications.RECIPIENT);
        if (uid == null || controller.getCurrentDestination() == null) return;
        int home = HomeNavigation.destination(this);
        Bundle authenticatedArgs;
        try {
            // Chat, perfil e detalhes também podem estar abertos ao tocar no aviso.
            // A Home na pilha comprova que o fluxo autenticado já foi estabelecido.
            authenticatedArgs = controller.getBackStackEntry(home).getArguments();
        } catch (IllegalArgumentException notAuthenticatedYet) { return; }
        getIntent().removeExtra(PendingEventNotifications.RECIPIENT);
        if (!pushRegistration.isCurrentUser(uid)) return;
        int events = home == R.id.managerHomeFragment ? R.id.managerEventsFragment : R.id.employeeEventsFragment;
        if (controller.getCurrentDestination().getId() == events) return;
        int action = home == R.id.managerHomeFragment
                ? R.id.action_global_manager_events : R.id.action_global_employee_events;
        controller.navigate(action, AuthArgs.copy(authenticatedArgs));
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
        navbar.findViewById(R.id.button_conversations_nav_profile).setOnClickListener(clicked -> {
            if (navigation.getCurrentDestination().getId() == R.id.profileFragment) return;
            Bundle args = AuthArgs.copy(navigation.getCurrentBackStackEntry().getArguments());
            if (!navigation.popBackStack(R.id.profileFragment, false)) {
                navigation.navigate(R.id.action_global_profile, args);
            }
        });
        navbar.findViewById(R.id.button_conversations_nav_events).setOnClickListener(clicked -> {
            // A mesma aba abre a lista correspondente ao fluxo ativo.
            int home = HomeNavigation.destination(this);
            boolean manager = home == R.id.managerHomeFragment;
            int events = manager ? R.id.managerEventsFragment : R.id.employeeEventsFragment;
            if (navigation.getCurrentDestination().getId() == events) return;
            Bundle args = AuthArgs.copy(navigation.getCurrentBackStackEntry().getArguments());
            navigation.navigate(manager ? R.id.action_global_manager_events : R.id.action_global_employee_events,
                    args, new NavOptions.Builder()
                    .setPopUpTo(home, false).setLaunchSingleTop(true)
                    .setEnterAnim(R.anim.push_enter).setExitAnim(R.anim.push_exit)
                    .setPopEnterAnim(R.anim.push_pop_enter).setPopExitAnim(R.anim.push_pop_exit).build());
        });
    }

    private void updateBottomNavigation(View navbar, int destination, WindowInsetsCompat insets) {
        // O teclado oculta a navbar em qualquer tela; ao fechar, ela volta à posição original.
        boolean home = destination == R.id.employeeHomeFragment || destination == R.id.managerHomeFragment;
        boolean conversations = destination == R.id.conversationsFragment || destination == R.id.chatFragment
                || destination == R.id.newConversationFragment
                || destination == R.id.aiSessionsFragment || destination == R.id.humanChatFragment;
        boolean profile = destination == R.id.profileFragment;
        boolean events = destination == R.id.employeeEventsFragment
                || destination == R.id.employeeEventDetailsFragment || destination == R.id.managerEventsFragment
                || destination == R.id.managerEventDetailsFragment || destination == R.id.eventParticipantsFragment
                || destination == R.id.eventAssignmentFragment || destination == R.id.eventValidationFragment
                || destination == R.id.eventEvidenceFragment;
        boolean notifications = destination == R.id.notificationsFragment;
        boolean keyboard = insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
        navbar.setVisibility((home || conversations || profile || events || notifications
                || destination == R.id.staffFragment || destination == R.id.staffDetailsFragment
                || destination == R.id.staffEventsFragment || destination == R.id.managerFormsFragment)
                && !keyboard
                ? View.VISIBLE : View.GONE);
        navbar.findViewById(R.id.button_conversations_nav_home).setSelected(home);
        navbar.findViewById(R.id.button_conversations_nav_chat).setSelected(conversations);
        navbar.findViewById(R.id.button_conversations_nav_profile).setSelected(profile);
        navbar.findViewById(R.id.button_conversations_nav_events).setSelected(events);
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
