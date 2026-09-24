package com.example.astro_mobile.navigation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;

public class SplashFragment extends Fragment {

    private static final long INITIAL_PAUSE_MS = 180L;
    private static final long MAIN_ANIMATION_MS = 920L;
    private static final long PLANET_DELAY_MS = 420L;
    private static final long PLANET_ANIMATION_MS = 520L;
    private static final long NAVIGATION_DELAY_MS = 180L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable navigationRunnable = this::navigateToMockDestination;
    private AnimatorSet splashAnimator;

    public SplashFragment() {
        super(R.layout.fragment_splash);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.post(() -> startAnimation(view));
    }

    private void startAnimation(View root) {
        if (getView() != root) {
            return;
        }

        ImageView stars = root.findViewById(R.id.image_splash_stars);
        ImageView logo = root.findViewById(R.id.image_splash_logo);
        ImageView planet = root.findViewById(R.id.image_splash_planet);

        float planetStart = planet.getHeight() + dpToPx(24);
        planet.setTranslationY(planetStart);
        planet.setAlpha(0f);
        planet.setScaleX(0.94f);
        planet.setScaleY(0.94f);

        PathInterpolator motionInterpolator = new PathInterpolator(0.4f, 0f, 0.2f, 1f);
        OvershootInterpolator logoBounceInterpolator = new OvershootInterpolator(0.9f);
        OvershootInterpolator planetBounceInterpolator = new OvershootInterpolator(1.1f);
        AccelerateDecelerateInterpolator scaleInterpolator = new AccelerateDecelerateInterpolator();

        ObjectAnimator logoMovement = ObjectAnimator.ofFloat(
                logo,
                View.Y,
                logo.getY(),
                getResources().getDimension(R.dimen.auth_logo_top_spacing)
        );
        logoMovement.setDuration(MAIN_ANIMATION_MS);
        logoMovement.setInterpolator(logoBounceInterpolator);

        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(
                logo,
                View.SCALE_X,
                1f,
                1.05f,
                0.985f,
                1f
        );
        logoScaleX.setDuration(MAIN_ANIMATION_MS);
        logoScaleX.setInterpolator(scaleInterpolator);

        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(
                logo,
                View.SCALE_Y,
                1f,
                1.05f,
                0.985f,
                1f
        );
        logoScaleY.setDuration(MAIN_ANIMATION_MS);
        logoScaleY.setInterpolator(scaleInterpolator);

        ObjectAnimator starsMovement = ObjectAnimator.ofFloat(
                stars,
                View.TRANSLATION_Y,
                0f,
                -stars.getHeight()
        );
        starsMovement.setDuration(MAIN_ANIMATION_MS);
        starsMovement.setInterpolator(motionInterpolator);

        ObjectAnimator planetMovement = ObjectAnimator.ofFloat(
                planet,
                View.TRANSLATION_Y,
                planetStart,
                0f
        );
        planetMovement.setStartDelay(PLANET_DELAY_MS);
        planetMovement.setDuration(PLANET_ANIMATION_MS);
        planetMovement.setInterpolator(planetBounceInterpolator);

        ObjectAnimator planetFade = ObjectAnimator.ofFloat(planet, View.ALPHA, 0f, 1f);
        planetFade.setStartDelay(PLANET_DELAY_MS);
        planetFade.setDuration(PLANET_ANIMATION_MS);

        ObjectAnimator planetScaleX = ObjectAnimator.ofFloat(
                planet,
                View.SCALE_X,
                0.94f,
                1.035f,
                1f
        );
        planetScaleX.setStartDelay(PLANET_DELAY_MS);
        planetScaleX.setDuration(PLANET_ANIMATION_MS);
        planetScaleX.setInterpolator(scaleInterpolator);

        ObjectAnimator planetScaleY = ObjectAnimator.ofFloat(
                planet,
                View.SCALE_Y,
                0.94f,
                1.035f,
                1f
        );
        planetScaleY.setStartDelay(PLANET_DELAY_MS);
        planetScaleY.setDuration(PLANET_ANIMATION_MS);
        planetScaleY.setInterpolator(scaleInterpolator);

        splashAnimator = new AnimatorSet();
        splashAnimator.playTogether(
                logoMovement,
                logoScaleX,
                logoScaleY,
                starsMovement,
                planetMovement,
                planetFade,
                planetScaleX,
                planetScaleY
        );
        splashAnimator.setStartDelay(INITIAL_PAUSE_MS);
        splashAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                mainHandler.postDelayed(navigationRunnable, NAVIGATION_DELAY_MS);
            }
        });
        splashAnimator.start();
    }

    private void navigateToMockDestination() {
        if (!isAdded()
                || getView() == null
                || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            return;
        }

        NavController navController = NavHostFragment.findNavController(this);
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.splashFragment) {
            navController.navigate(R.id.action_splash_to_mock_destination);
        }
    }

    private float dpToPx(int value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    public void onDestroyView() {
        mainHandler.removeCallbacks(navigationRunnable);
        if (splashAnimator != null) {
            splashAnimator.cancel();
            splashAnimator = null;
        }
        super.onDestroyView();
    }
}
