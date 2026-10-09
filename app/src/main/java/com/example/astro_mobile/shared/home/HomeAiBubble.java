package com.example.astro_mobile.shared.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.example.astro_mobile.R;

// Mantém o mesmo arraste e recolhimento do assistente nas duas Homes.
public final class HomeAiBubble {
    private static final long AI_BUBBLE_VISIBLE_MS = 3_000L;
    private static final long AI_BUBBLE_COLLAPSE_MS = 500L;
    private final View aiBubble;
    private final TextView aiBubbleText;
    private Runnable hideAiBubble;
    private ValueAnimator aiBubbleWidthAnimator;

    public HomeAiBubble(View bubble, TextView text, View content, View header) {
        aiBubble = bubble;
        aiBubbleText = text;
        bubble.setOnTouchListener(new AiDragTouchListener(bubble, content, header));
    }

    public void hide() {
        // Cancela o recolhimento e o feedback ao sair da Home.
        if (hideAiBubble != null) aiBubble.removeCallbacks(hideAiBubble);
        if (aiBubbleWidthAnimator != null) {
            aiBubbleWidthAnimator.cancel();
            aiBubbleWidthAnimator = null;
        }
        aiBubble.animate().cancel();
        aiBubble.setScaleX(1f);
        aiBubble.setScaleY(1f);
        aiBubble.setVisibility(View.GONE);
    }

    public void showTemporarily() {
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
                        aiBubble.getResources().getDimensionPixelSize(R.dimen.employee_home_ai_size));
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
        int collapsedWidth = aiBubble.getResources().getDimensionPixelSize(R.dimen.employee_home_ai_size);
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

    private class AiDragTouchListener implements View.OnTouchListener {
        private final View button;
        private final View content;
        private final View header;
        private final int touchSlop;
        private final float edge;
        private float downX;
        private float downY;
        private float buttonStartX;
        private float buttonStartY;
        private boolean dragging;

        AiDragTouchListener(View button, View content, View header) {
            this.button = button;
            this.content = content;
            this.header = header;
            touchSlop = ViewConfiguration.get(button.getContext()).getScaledTouchSlop();
            float density = button.getResources().getDisplayMetrics().density;
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
                        float contentLeft = button.getParent() == content ? 0 : content.getLeft();
                        float contentTop = button.getParent() == content ? 0 : content.getTop();
                        float left = contentLeft + edge;
                        float right = contentLeft + content.getWidth() - edge - button.getWidth();
                        float top = contentTop + header.getBottom() + edge;
                        float bottom = contentTop + content.getHeight() - edge - button.getHeight();
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

}
