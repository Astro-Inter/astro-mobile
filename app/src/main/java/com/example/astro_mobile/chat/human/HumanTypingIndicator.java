package com.example.astro_mobile.chat.human;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

/** Animação local; não envia eventos de rede. */
final class HumanTypingIndicator {
    private final ViewGroup dots;
    private AnimatorSet animation;

    HumanTypingIndicator(ViewGroup dots) { this.dots = dots; }

    void show(boolean typing) {
        dots.setVisibility(typing ? View.VISIBLE : View.GONE);
        if (!typing) { stop(); return; }
        if (animation != null || !ValueAnimator.areAnimatorsEnabled()) return;
        List<Animator> steps = new ArrayList<>();
        float lift = -3 * dots.getResources().getDisplayMetrics().density;
        for (int index = 0; index < dots.getChildCount(); index++) {
            View dot = dots.getChildAt(index);
            ObjectAnimator opacity = ObjectAnimator.ofFloat(dot, View.ALPHA, 0.35f, 1f, 0.35f);
            ObjectAnimator bounce = ObjectAnimator.ofFloat(dot, View.TRANSLATION_Y, 0f, lift, 0f);
            for (ObjectAnimator animator : new ObjectAnimator[]{opacity, bounce}) {
                animator.setDuration(900); animator.setStartDelay(index * 150L);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                steps.add(animator);
            }
        }
        animation = new AnimatorSet(); animation.playTogether(steps); animation.start();
    }

    private void stop() {
        if (animation != null) { animation.cancel(); animation = null; }
        for (int index = 0; index < dots.getChildCount(); index++) {
            dots.getChildAt(index).setAlpha(1f);
            dots.getChildAt(index).setTranslationY(0f);
        }
    }
}
