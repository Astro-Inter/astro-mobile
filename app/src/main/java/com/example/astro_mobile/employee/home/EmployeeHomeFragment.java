package com.example.astro_mobile.employee.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
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
import com.example.astro_mobile.auth.AccessKeyViewModel;
import com.example.astro_mobile.auth.MockSession;
import com.example.astro_mobile.auth.EmailVerificationViewModel;
import com.example.astro_mobile.auth.SessionViewModel;
import com.example.astro_mobile.data.local.FlowPreferences;

public class EmployeeHomeFragment extends Fragment {

    private static final String STATE_EMPTY_PREVIEW = "empty_preview";
    private static final long HOME_MOCK_LOAD_MS = 2_000L;
    private static final long AI_BUBBLE_VISIBLE_MS = 3_000L;
    private static final long AI_BUBBLE_COLLAPSE_MS = 500L;
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
    private Runnable hideAiBubble;
    private Runnable finishHomeLoading;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator skeletonPulseAnimator;
    private ValueAnimator aiBubbleWidthAnimator;
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
                R.id.button_employee_home_events,
                R.id.button_employee_home_pending_card,
                R.id.button_employee_home_review_card,
                R.id.button_employee_home_completed_card,
                R.id.button_employee_home_events_more,
                R.id.button_employee_home_notifications_section,
                R.id.button_employee_home_notifications_more,
                R.id.button_employee_home_nav_events,
                R.id.button_employee_home_nav_profile
        };
        for (int id : mockActions) {
            view.findViewById(id).setOnClickListener(clicked -> showMockMessage());
        }

        View.OnClickListener openChat = clicked -> Navigation.findNavController(clicked)
                .navigate(R.id.action_employee_home_to_chat);
        view.findViewById(R.id.button_employee_home_nav_chat).setOnClickListener(openChat);
        aiBubble.setOnClickListener(openChat);

        // Restringe o arraste do assistente à área entre cabeçalho e navbar.
        aiBubble.setOnTouchListener(new AiDragTouchListener(
                aiBubble,
                view.findViewById(R.id.container_employee_home_content),
                view.findViewById(R.id.container_employee_home_header),
                view.findViewById(R.id.container_employee_home_bottom_nav)));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ao retornar, continua o carregamento ou reabre o balão se a Home já estiver pronta.
        if (homeReady) {
            showAiBubbleTemporarily();
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
        if (aiBubble != null) {
            if (hideAiBubble != null) {
                aiBubble.removeCallbacks(hideAiBubble);
            }
            if (aiBubbleWidthAnimator != null) {
                aiBubbleWidthAnimator.cancel();
                aiBubbleWidthAnimator = null;
            }
            aiBubble.setVisibility(View.GONE);
            aiBubble.setAlpha(1f);
            if (aiBubbleText != null) {
                aiBubbleText.setAlpha(1f);
            }
        }
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
        if (aiBubbleWidthAnimator != null) {
            aiBubbleWidthAnimator.cancel();
            aiBubbleWidthAnimator = null;
        }
        aiBubble = null;
        aiBubbleText = null;
        homeContent = null;
        homeSkeleton = null;
        hideAiBubble = null;
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
            showAiBubbleTemporarily();
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
                            showAiBubbleTemporarily();
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

    private void showAiBubbleTemporarily() {
        // Abre o texto do assistente e agenda seu recolhimento após três segundos.
        if (aiBubble == null || aiBubbleText == null) {
            return;
        }
        if (hideAiBubble != null) {
            aiBubble.removeCallbacks(hideAiBubble);
        }
        if (aiBubbleWidthAnimator != null) {
            aiBubbleWidthAnimator.cancel();
            aiBubbleWidthAnimator = null;
        }
        ViewGroup.LayoutParams layoutParams = aiBubble.getLayoutParams();
        layoutParams.width = 0;
        aiBubble.setLayoutParams(layoutParams);
        aiBubble.setAlpha(1f);
        aiBubbleText.setAlpha(1f);
        aiBubble.setVisibility(View.VISIBLE);
        View bubble = aiBubble;
        hideAiBubble = () -> {
            if (!ValueAnimator.areAnimatorsEnabled()) {
                setAiBubbleWidth(bubble,
                        getResources().getDimensionPixelSize(R.dimen.employee_home_ai_size));
                bubble.setAlpha(1f);
                aiBubbleText.setAlpha(0f);
                return;
            }
            animateAiBubbleClosed(bubble);
        };
        aiBubble.postDelayed(hideAiBubble, AI_BUBBLE_VISIBLE_MS);
    }

    private void animateAiBubbleClosed(View bubble) {
        // Encolhe o botão para a direita até restar apenas o mascote.
        int startWidth = bubble.getWidth();
        int collapsedWidth = getResources().getDimensionPixelSize(R.dimen.employee_home_ai_size);
        if (startWidth <= collapsedWidth) {
            setAiBubbleWidth(bubble, collapsedWidth);
            bubble.setAlpha(1f);
            aiBubbleText.setAlpha(0f);
            return;
        }

        float startTextAlpha = aiBubbleText.getAlpha();
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        aiBubbleWidthAnimator = animator;
        animator.setDuration(AI_BUBBLE_COLLAPSE_MS);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            int width = startWidth + Math.round((collapsedWidth - startWidth) * fraction);
            setAiBubbleWidth(bubble, width);
            aiBubbleText.setAlpha(startTextAlpha * (1f - fraction));
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (aiBubbleWidthAnimator == animator) {
                    aiBubbleWidthAnimator = null;
                    setAiBubbleWidth(bubble, collapsedWidth);
                    aiBubbleText.setAlpha(0f);
                    bubble.setAlpha(1f);
                }
            }
        });
        animator.start();
    }

    private void setAiBubbleWidth(View bubble, int width) {
        ViewGroup.LayoutParams layoutParams = bubble.getLayoutParams();
        if (layoutParams.width != width) {
            layoutParams.width = width;
            bubble.setLayoutParams(layoutParams);
        }
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

    // Distingue toque de arraste e mantém o assistente dentro da área útil.
    private class AiDragTouchListener implements View.OnTouchListener {
        private final View button;
        private final View content;
        private final View header;
        private final View bottomNav;
        private final int touchSlop;
        private final float edge;
        private float downX;
        private float downY;
        private float buttonStartX;
        private float buttonStartY;
        private boolean dragging;

        AiDragTouchListener(View button, View content, View header, View bottomNav) {
            this.button = button;
            this.content = content;
            this.header = header;
            this.bottomNav = bottomNav;
            touchSlop = ViewConfiguration.get(requireContext()).getScaledTouchSlop();
            float density = getResources().getDisplayMetrics().density;
            edge = 8f * density;
        }

        @Override
        public boolean onTouch(View touched, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // Guarda a posição inicial e mostra a reação ao toque.
                    downX = event.getRawX();
                    downY = event.getRawY();
                    buttonStartX = button.getX();
                    buttonStartY = button.getY();
                    dragging = false;
                    touched.setPressed(true);
                    if (ValueAnimator.areAnimatorsEnabled()) {
                        button.animate().cancel();
                        button.animate().scaleX(0.94f).scaleY(0.94f)
                                .setDuration(100).start();
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    // Só começa a arrastar após ultrapassar a margem de movimento do toque.
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;
                    if (!dragging && Math.hypot(dx, dy) > touchSlop) {
                        dragging = true;
                        touched.setPressed(false);
                        button.animate().cancel();
                        button.setScaleX(1f);
                        button.setScaleY(1f);
                        if (hideAiBubble != null) {
                            aiBubble.removeCallbacks(hideAiBubble);
                        }
                        if (aiBubbleWidthAnimator != null) {
                            aiBubbleWidthAnimator.cancel();
                            aiBubbleWidthAnimator = null;
                        }
                    }
                    if (dragging) {
                        // Impede que o botão passe pelo cabeçalho ou pela navbar.
                        float left = content.getLeft() + edge;
                        float right = content.getRight() - edge - button.getWidth();
                        float top = content.getTop() + header.getBottom() + edge;
                        float bottom = content.getTop() + bottomNav.getTop()
                                - edge - button.getHeight();
                        button.setX(clamp(buttonStartX + dx, left, right));
                        button.setY(clamp(buttonStartY + dy, top, bottom));
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Ao soltar, mantém a posição arrastada ou executa o clique normal.
                    touched.setPressed(false);
                    button.animate().cancel();
                    if (ValueAnimator.areAnimatorsEnabled()) {
                        button.animate().scaleX(1f).scaleY(1f)
                                .setDuration(150).start();
                    } else {
                        button.setScaleX(1f);
                        button.setScaleY(1f);
                    }
                    if (dragging) {
                        if (aiBubble.getVisibility() == View.VISIBLE && hideAiBubble != null) {
                            aiBubble.postDelayed(hideAiBubble, AI_BUBBLE_VISIBLE_MS);
                        }
                    } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                            && event.getX() >= 0 && event.getX() < touched.getWidth()
                            && event.getY() >= 0 && event.getY() < touched.getHeight()) {
                        touched.performClick();
                    }
                    return true;
                default:
                    return true;
            }
        }

        private float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(value, Math.max(min, max)));
        }
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
