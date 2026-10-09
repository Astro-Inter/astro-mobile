package com.example.astro_mobile.employee.home;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.AccessKeyViewModel;
import com.example.astro_mobile.auth.EmailVerificationViewModel;
import com.example.astro_mobile.auth.MockSession;
import com.example.astro_mobile.auth.SessionViewModel;
import com.example.astro_mobile.data.local.FlowPreferences;
import com.example.astro_mobile.shared.home.HomeAiBubble;

public class EmployeeHomeFragment extends Fragment {

    private static final String STATE_EMPTY_PREVIEW = "empty_preview";
    private static final long HOME_MOCK_LOAD_MS = 2_000L;
    private static final long SKELETON_PULSE_MS = 900L;

    private static final MockRow[] EVENT_ROWS = {
            new MockRow(R.string.employee_home_training, R.string.employee_home_due_tomorrow,
                    R.string.employee_home_urgent, R.drawable.home_warning),
            new MockRow(R.string.employee_home_training, R.string.employee_home_due_date,
                    R.string.employee_home_new, R.drawable.home_new),
            new MockRow(R.string.employee_home_training, R.string.employee_home_due_date,
                    R.string.employee_home_pending, 0)
    };

    private static final MockRow[] NOTIFICATION_ROWS = {
            new MockRow(R.string.employee_home_notification_item, 0,
                    R.string.employee_home_notification_time, R.drawable.home_new),
            new MockRow(R.string.employee_home_notification_item, 0,
                    R.string.employee_home_notification_time, 0),
            new MockRow(R.string.employee_home_notification_item, 0,
                    R.string.employee_home_notification_time, 0)
    };

    private boolean emptyPreview;
    private boolean homeReady;
    private View aiBubble;
    private TextView aiBubbleText;
    private View homeContent;
    private View homeSkeleton;
    private HomeAiBubble assistant;
    private Runnable finishHomeLoading;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator skeletonPulseAnimator;
    private long homeLoadingRemainingMs = HOME_MOCK_LOAD_MS;
    private long homeLoadingStartedAtMs;

    public EmployeeHomeFragment() {
        super(R.layout.fragment_employee_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyPreview = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_EMPTY_PREVIEW);

        // Preenche eventos e notificações com dados de demonstração.
        addRows(view.findViewById(R.id.list_employee_home_events), EVENT_ROWS);
        addRows(view.findViewById(R.id.list_employee_home_notifications), NOTIFICATION_ROWS);
        showPreview(view);

        // Mostra o skeleton antes do conteúdo enquanto simula o carregamento.
        homeContent = view.findViewById(R.id.scroll_employee_home);
        homeSkeleton = view.findViewById(R.id.container_employee_home_skeleton);
        homeReady = false;
        homeLoadingRemainingMs = HOME_MOCK_LOAD_MS;
        homeContent.setAlpha(0f);
        homeContent.setVisibility(View.INVISIBLE);
        homeSkeleton.setAlpha(1f);
        homeSkeleton.setVisibility(View.VISIBLE);

        aiBubble = view.findViewById(R.id.button_employee_home_ai_bubble);
        aiBubbleText = view.findViewById(R.id.text_employee_home_ai_bubble);
        aiBubble.setVisibility(View.GONE);

        // Sair encerra a sessão Firebase e remove qualquer estado local anterior.
        view.findViewById(R.id.button_employee_home_logout).setOnClickListener(clickedView -> {
            new ViewModelProvider(requireActivity(), SessionViewModel.Factory.createDefault())
                    .get(SessionViewModel.class).signOut();
            FlowPreferences.clear(requireContext());
            MockSession.clear(requireContext());
            new ViewModelProvider(requireActivity(),
                    EmailVerificationViewModel.Factory.createDefault())
                    .get(EmailVerificationViewModel.class).clearForLogout();
            new ViewModelProvider(requireActivity(), new AccessKeyViewModel.Factory())
                    .get(AccessKeyViewModel.class).clearForLogout();
            Navigation.findNavController(clickedView)
                    .navigate(R.id.action_employee_home_to_email_identification);
        });

        // Um toque longo no cumprimento alterna os estados de demonstração da Home.
        view.findViewById(R.id.text_employee_home_greeting).setOnLongClickListener(pressed -> {
            emptyPreview = !emptyPreview;
            showPreview(view);
            view.findViewById(R.id.scroll_employee_home).scrollTo(0, 0);
            return true;
        });

        // Os destinos ainda não implementados exibem a mesma mensagem provisória.
        int[] mockActions = {
                R.id.button_employee_home_notifications,
                R.id.button_employee_home_notifications_section,
                R.id.button_employee_home_notifications_more
        };
        for (int id : mockActions) {
            view.findViewById(id).setOnClickListener(clicked -> showMockMessage());
        }

        // Os atalhos de eventos abrem o calendário do colaborador.
        int[] eventsActions = { R.id.button_employee_home_events, R.id.button_employee_home_pending_card,
                R.id.button_employee_home_review_card, R.id.button_employee_home_completed_card,
                R.id.button_employee_home_events_more };
        for (int id : eventsActions) {
            view.findViewById(id).setOnClickListener(clicked -> Navigation.findNavController(clicked)
                    .navigate(R.id.action_global_employee_events, AuthArgs.copy(getArguments())));
        }

        // O mascote continua sendo um atalho direto para a IA; a Activity controla a navbar.
        aiBubble.setOnClickListener(clicked ->
                Navigation.findNavController(clicked).navigate(R.id.action_employee_home_to_chat,
                        AuthArgs.copy(getArguments())));

        // Restringe o arraste do assistente à área entre cabeçalho e navbar.
        assistant = new HomeAiBubble(aiBubble, aiBubbleText,
                view.findViewById(R.id.container_employee_home_content),
                view.findViewById(R.id.container_employee_home_header));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ao retornar, continua o carregamento ou reabre o balão se a Home já estiver pronta.
        if (homeReady) {
            assistant.showTemporarily();
        } else {
            startMockHomeLoad();
        }
    }

    @Override
    public void onPause() {
        // Pausa o tempo restante do skeleton e esconde o balão ao deixar a Home.
        if (!homeReady && finishHomeLoading != null) {
            mainHandler.removeCallbacks(finishHomeLoading);
            homeLoadingRemainingMs = Math.max(0L, homeLoadingRemainingMs
                    - (SystemClock.uptimeMillis() - homeLoadingStartedAtMs));
            finishHomeLoading = null;
        }
        stopSkeletonPulse();
        finishHomeRevealIfNeeded();
        if (assistant != null) assistant.hide();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        // Cancela tarefas e animações ligadas às Views antes de descartá-las.
        if (finishHomeLoading != null) {
            mainHandler.removeCallbacks(finishHomeLoading);
            finishHomeLoading = null;
        }
        stopSkeletonPulse();
        if (homeContent != null) {
            homeContent.animate().cancel();
        }
        if (homeSkeleton != null) {
            homeSkeleton.animate().cancel();
        }
        if (assistant != null) assistant.hide();
        assistant = null;
        aiBubble = null;
        aiBubbleText = null;
        homeContent = null;
        homeSkeleton = null;
        homeReady = false;
        homeLoadingRemainingMs = HOME_MOCK_LOAD_MS;
        super.onDestroyView();
    }

    private void startMockHomeLoad() {
        // Espera o tempo restante da simulação antes de revelar a Home.
        if (homeReady || homeSkeleton == null || finishHomeLoading != null) {
            return;
        }
        startSkeletonPulse();
        homeLoadingStartedAtMs = SystemClock.uptimeMillis();
        finishHomeLoading = () -> {
            finishHomeLoading = null;
            homeLoadingRemainingMs = 0L;
            revealHome();
        };
        mainHandler.postDelayed(finishHomeLoading, homeLoadingRemainingMs);
    }

    private void revealHome() {
        // Troca o skeleton pelo conteúdo com um dissolve curto.
        if (homeContent == null || homeSkeleton == null) {
            return;
        }
        homeReady = true;
        stopSkeletonPulse();
        homeContent.setAlpha(0f);
        homeContent.setVisibility(View.VISIBLE);
        if (!ValueAnimator.areAnimatorsEnabled()) {
            homeContent.setAlpha(1f);
            homeSkeleton.setVisibility(View.GONE);
            assistant.showTemporarily();
            return;
        }

        int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        homeContent.animate()
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        View skeleton = homeSkeleton;
        skeleton.animate()
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (homeSkeleton == skeleton) {
                        skeleton.setVisibility(View.GONE);
                        if (isResumed()) {
                            assistant.showTemporarily();
                        }
                    }
                })
                .start();
    }

    private void startSkeletonPulse() {
        // Faz as áreas de preenchimento pulsarem durante a espera mock.
        if (homeSkeleton == null || !ValueAnimator.areAnimatorsEnabled()
                || skeletonPulseAnimator != null) {
            return;
        }
        skeletonPulseAnimator = ValueAnimator.ofFloat(0.84f, 1f);
        skeletonPulseAnimator.setDuration(SKELETON_PULSE_MS);
        skeletonPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        skeletonPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        skeletonPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        skeletonPulseAnimator.addUpdateListener(animation ->
                setSkeletonPlaceholderAlpha(homeSkeleton,
                        (float) animation.getAnimatedValue()));
        skeletonPulseAnimator.start();
    }

    private void stopSkeletonPulse() {
        if (skeletonPulseAnimator != null) {
            skeletonPulseAnimator.cancel();
            skeletonPulseAnimator = null;
        }
        if (homeSkeleton != null) {
            setSkeletonPlaceholderAlpha(homeSkeleton, 1f);
        }
    }

    private void setSkeletonPlaceholderAlpha(View view, float alpha) {
        // Altera só os itens do skeleton que têm fundo visível.
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                setSkeletonPlaceholderAlpha(group.getChildAt(index), alpha);
            }
        } else if (view.getBackground() != null) {
            view.setAlpha(alpha);
        }
    }

    private void finishHomeRevealIfNeeded() {
        // Finaliza a troca de telas se o app pausar no meio do dissolve.
        if (!homeReady || homeSkeleton == null || homeContent == null
                || homeSkeleton.getVisibility() != View.VISIBLE) {
            return;
        }
        homeSkeleton.animate().cancel();
        homeContent.animate().cancel();
        homeSkeleton.setVisibility(View.GONE);
        homeContent.setAlpha(1f);
        homeContent.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Mantém o estado de demonstração vazio ou preenchido ao recriar a tela.
        outState.putBoolean(STATE_EMPTY_PREVIEW, emptyPreview);
        super.onSaveInstanceState(outState);
    }

    private void addRows(LinearLayout container, MockRow[] rows) {
        // Monta as linhas dos feeds a partir dos dados locais de exemplo.
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (MockRow row : rows) {
            View item = inflater.inflate(R.layout.item_home_feed_row, container, false);
            TextView title = item.findViewById(R.id.text_home_feed_title);
            TextView subtitle = item.findViewById(R.id.text_home_feed_subtitle);
            TextView meta = item.findViewById(R.id.text_home_feed_meta);
            ImageView statusIcon = item.findViewById(R.id.image_home_feed_status);

            title.setText(row.title);
            if (row.subtitle == 0) {
                subtitle.setVisibility(View.GONE);
            } else {
                subtitle.setText(row.subtitle);
            }
            meta.setText(row.meta);
            if (row.statusIcon == 0) {
                statusIcon.setVisibility(View.INVISIBLE);
            } else {
                statusIcon.setImageResource(row.statusIcon);
            }
            item.findViewById(R.id.button_home_feed_row)
                    .setOnClickListener(clicked -> showMockMessage());
            container.addView(item);
        }
    }

    private void showPreview(View view) {
        // Alterna textos, ícones e listas entre Home vazia e preenchida.
        TextView subtitle = view.findViewById(R.id.text_employee_home_subtitle);
        TextView message = view.findViewById(R.id.text_employee_home_message);
        ImageView messageIcon = view.findViewById(R.id.image_employee_home_message);
        View messageContainer = view.findViewById(R.id.container_employee_home_message);

        subtitle.setText(emptyPreview ? R.string.employee_home_subtitle_empty
                : R.string.employee_home_subtitle_filled);
        message.setText(emptyPreview ? R.string.employee_home_urgent_pending
                : R.string.employee_home_no_pending);
        messageIcon.setImageResource(emptyPreview ? R.drawable.home_warning
                : R.drawable.home_check);
        messageContainer.setBackgroundResource(emptyPreview
                ? R.drawable.bg_home_warning_message
                : R.drawable.bg_home_positive_message);

        view.findViewById(R.id.container_employee_home_workspace)
                .setVisibility(emptyPreview ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.list_employee_home_events)
                .setVisibility(emptyPreview ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_employee_home_events_more)
                .setVisibility(emptyPreview ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_employee_home_events_empty)
                .setVisibility(emptyPreview ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.list_employee_home_notifications)
                .setVisibility(emptyPreview ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_employee_home_notifications_more)
                .setVisibility(emptyPreview ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_employee_home_notifications_empty)
                .setVisibility(emptyPreview ? View.VISIBLE : View.GONE);
    }

    private void showMockMessage() {
        Toast.makeText(requireContext(), R.string.employee_home_mock_unavailable,
                Toast.LENGTH_SHORT).show();
    }

    private static class MockRow {
        final int title;
        final int subtitle;
        final int meta;
        final int statusIcon;

        MockRow(int title, int subtitle, int meta, int statusIcon) {
            this.title = title;
            this.subtitle = subtitle;
            this.meta = meta;
            this.statusIcon = statusIcon;
        }
    }
}
