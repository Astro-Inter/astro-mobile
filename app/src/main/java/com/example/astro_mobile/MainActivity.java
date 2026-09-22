package com.example.astro_mobile;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

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
import androidx.navigation.fragment.NavHostFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(
                this,
                SystemBarStyle.dark(getColor(R.color.purple_gray)),
                SystemBarStyle.dark(getColor(R.color.astro_dark_background))
        );
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        View navigationHost = findViewById(R.id.nav_host_fragment);
        ImageView authenticationPlanetOverlay = findViewById(R.id.authentication_planet_overlay);
        View statusBarScrim = findViewById(R.id.status_bar_scrim);
        View navigationBarScrim = findViewById(R.id.navigation_bar_scrim);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                boolean isAuthenticationScreen = destinationId == R.id.mockDestinationFragment
                        || destinationId == R.id.accessKeyInformationFragment;
                authenticationPlanetOverlay.setVisibility(
                        isAuthenticationScreen ? View.VISIBLE : View.GONE
                );
            });
        }

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), root);
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            navigationHost.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            updateOverlayBottomMargin(authenticationPlanetOverlay, systemBars.bottom);
            updateScrimHeight(statusBarScrim, systemBars.top, Gravity.TOP);
            updateScrimHeight(navigationBarScrim, systemBars.bottom, Gravity.BOTTOM);
            return insets;
        });
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
