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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.AccessKeyViewModel;
import com.example.astro_mobile.auth.MockSession;
import com.example.astro_mobile.auth.EmailVerificationViewModel;
import com.example.astro_mobile.auth.SessionViewModel;
import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.example.astro_mobile.data.local.FlowPreferences;

public class SplashFragment extends Fragment {

    private static final long INITIAL_PAUSE_MS = 180L;
    private static final long MAIN_ANIMATION_MS = 920L;
    private static final long PLANET_DELAY_MS = 420L;
    private static final long PLANET_ANIMATION_MS = 520L;
    private static final long NAVIGATION_DELAY_MS = 180L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable navigationRunnable = this::navigateAfterSplash;
    private AnimatorSet splashAnimator;
    private SessionViewModel sessionViewModel;
    private EmailVerificationViewModel verificationViewModel;

    public SplashFragment() {
        super(R.layout.fragment_splash);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionViewModel = new ViewModelProvider(requireActivity(),
                SessionViewModel.Factory.createDefault()).get(SessionViewModel.class);
        verificationViewModel = new ViewModelProvider(requireActivity(),
                EmailVerificationViewModel.Factory.createDefault())
                .get(EmailVerificationViewModel.class);
        // Espera o layout medir as imagens antes de iniciar o movimento.
        view.post(() -> startAnimation(view));
    }

    private void startAnimation(View root) {
        if (getView() != root) {
            return;
        }

        ImageView stars = root.findViewById(R.id.image_splash_stars);
        ImageView logo = root.findViewById(R.id.image_splash_logo);
        ImageView planet = root.findViewById(R.id.image_splash_planet);

        // Posiciona o planeta abaixo da tela para ele entrar durante a animação.
        float planetStart = planet.getHeight() + dpToPx(24);
        planet.setTranslationY(planetStart);
        planet.setAlpha(0f);
        planet.setScaleX(0.94f);
        planet.setScaleY(0.94f);

        PathInterpolator motionInterpolator = new PathInterpolator(0.4f, 0f, 0.2f, 1f);
        OvershootInterpolator logoBounceInterpolator = new OvershootInterpolator(0.9f);
        OvershootInterpolator planetBounceInterpolator = new OvershootInterpolator(1.1f);
        AccelerateDecelerateInterpolator scaleInterpolator = new AccelerateDecelerateInterpolator();

        // Move e pulsa a logo até a posição usada na identificação de e-mail.
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

        // Desloca as estrelas e faz o planeta aparecer com movimento elástico.
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

        // Executa os movimentos juntos e navega após a animação terminar.
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

    private void navigateAfterSplash() {
        // Primeiro verifica a sessão Firebase, inclusive cadastros ainda não ativados.
        if (!isCurrentSplash()) {
            return;
        }
        sessionViewModel.checkCurrentUser(new SessionViewModel.SessionCallback() {
            @Override
            public void onUser(@Nullable String email) {
                if (!isCurrentSplash()) {
                    return;
                }
                if (email == null) {
                    navigateWithoutFirebaseSession();
                } else {
                    verifySignedInEmail(email);
                }
            }

            @Override
            public void onFailure(AuthFailureKind kind) {
                if (!isCurrentSplash()) {
                    return;
                }
                if (kind == AuthFailureKind.DISABLED
                        || kind == AuthFailureKind.INVALID_CREDENTIALS) {
                    sessionViewModel.signOut();
                    FlowPreferences.clear(requireContext());
                    navigateWithoutFirebaseSession();
                } else {
                    showStartupError(kind == AuthFailureKind.CONNECTION);
                }
            }
        });
    }

    private void verifySignedInEmail(String email) {
        // O backend confirma o tipo e o estado atuais antes de restaurar o fluxo.
        verificationViewModel.verifyEmail(email, (destination, message, userType) -> {
            if (!isCurrentSplash()) {
                return;
            }
            NavController navController = NavHostFragment.findNavController(this);
            if (destination == EmailVerificationViewModel.Destination.PASSWORD) {
                int homeAction = R.id.action_splash_to_flow_choice;
                if ("COLABORADOR".equals(userType) || FlowPreferences.wasEmployeeFlow(requireContext())) {
                    homeAction = R.id.action_splash_to_employee_home;
                    FlowPreferences.saveEmployeeFlow(requireContext());
                } else if (FlowPreferences.wasManagerFlow(requireContext())) {
                    homeAction = R.id.action_splash_to_manager_home;
                }
                navController.navigate(homeAction, AuthArgs.of(email, userType));
            } else if (destination == EmailVerificationViewModel.Destination.FIRST_ACCESS_KEY
                    && "COLABORADOR".equals(userType)) {
                // A conta Firebase já existe; retoma a ativação sem repetir a chave.
                new ViewModelProvider(requireActivity(), new AccessKeyViewModel.Factory())
                        .get(AccessKeyViewModel.class).setContext(email, userType);
                Bundle args = new Bundle();
                args.putString(AuthArgs.EMAIL, email);
                navController.navigate(R.id.action_splash_to_first_login_password, args);
            } else if (destination == EmailVerificationViewModel.Destination.CONNECTION_ERROR
                    || destination == EmailVerificationViewModel.Destination.INTERNAL_ERROR) {
                showStartupError(destination
                        == EmailVerificationViewModel.Destination.CONNECTION_ERROR);
            } else {
                // Conta desativada ou sem estado ativo não conserva a sessão local.
                sessionViewModel.signOut();
                FlowPreferences.clear(requireContext());
                MockSession.clear(requireContext());
                if (destination == EmailVerificationViewModel.Destination.DISABLED) {
                    verificationViewModel.setPendingInlineError(
                            getString(R.string.email_identification_disabled_error));
                } else if (destination == EmailVerificationViewModel.Destination.INLINE_ERROR
                        && message != null) {
                    verificationViewModel.setPendingInlineError(message);
                }
                navController.navigate(R.id.action_splash_to_email_identification);
            }
        });
    }

    private void navigateWithoutFirebaseSession() {
        // Uma sessão mock antiga não pode pular o cadastro ou liberar a home.
        MockSession.clear(requireContext());
        NavHostFragment.findNavController(this).navigate(R.id.action_splash_to_email_identification);
    }

    private void showStartupError(boolean connection) {
        NavHostFragment.findNavController(this).navigate(connection
                ? R.id.action_splash_to_connection_error
                : R.id.action_splash_to_generic_error);
    }

    private boolean isCurrentSplash() {
        if (!isAdded() || getView() == null
                || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            return false;
        }
        NavController navController = NavHostFragment.findNavController(this);
        return navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.splashFragment;
    }

    private float dpToPx(int value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    public void onDestroyView() {
        // Interrompe animação e navegação pendentes ao sair da splash.
        mainHandler.removeCallbacks(navigationRunnable);
        if (verificationViewModel != null && verificationViewModel.isLoading()) {
            verificationViewModel.cancelCurrentRequest();
        }
        if (splashAnimator != null) {
            splashAnimator.removeAllListeners();
            splashAnimator.cancel();
            splashAnimator = null;
        }
        super.onDestroyView();
    }
}
