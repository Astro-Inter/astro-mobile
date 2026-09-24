package com.example.astro_mobile.employee.home;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.astro_mobile.R;

public class EmployeeHomeFragment extends Fragment {

    private static final String STATE_EMPTY_PREVIEW = "empty_preview";
    private static final long HOME_MOCK_LOAD_MS = 2_000L;
    private static final long AI_BUBBLE_VISIBLE_MS = 3_000L;
    private static final long AI_BUBBLE_FADE_MS = 350L;
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
    private View aiButton;
    private View aiGlow;
    private View homeContent;
    private View homeSkeleton;
    private AiDragTouchListener aiDragTouchListener;
    private Runnable hideAiBubble;
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

        addRows(view.findViewById(R.id.list_employee_home_events), EVENT_ROWS);
        addRows(view.findViewById(R.id.list_employee_home_notifications), NOTIFICATION_ROWS);
        showPreview(view);

        homeContent = view.findViewById(R.id.scroll_employee_home);
        homeSkeleton = view.findViewById(R.id.container_employee_home_skeleton);
        homeReady = false;
        homeLoadingRemainingMs = HOME_MOCK_LOAD_MS;
        homeContent.setAlpha(0f);
        homeContent.setVisibility(View.INVISIBLE);
        homeSkeleton.setAlpha(1f);
        homeSkeleton.setVisibility(View.VISIBLE);

        aiBubble = view.findViewById(R.id.button_employee_home_ai_bubble);
        aiButton = view.findViewById(R.id.button_employee_home_ai);
        aiGlow = view.findViewById(R.id.glow_employee_home_ai);
        aiBubble.setVisibility(View.GONE);
        aiButton.setVisibility(View.GONE);
        aiGlow.setVisibility(View.GONE);

        view.findViewById(R.id.text_employee_home_greeting).setOnLongClickListener(pressed -> {
            emptyPreview = !emptyPreview;
            showPreview(view);
            view.findViewById(R.id.scroll_employee_home).scrollTo(0, 0);
            return true;
        });

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
                R.id.button_employee_home_nav_chat,
                R.id.button_employee_home_nav_profile,
                R.id.button_employee_home_ai,
                R.id.button_employee_home_ai_bubble
        };
        for (int id : mockActions) {
            view.findViewById(id).setOnClickListener(clicked -> showMockMessage());
        }

        aiDragTouchListener = new AiDragTouchListener(
                aiButton,
                view.findViewById(R.id.glow_employee_home_ai),
                view.findViewById(R.id.container_employee_home_content),
                view.findViewById(R.id.container_employee_home_header),
                view.findViewById(R.id.container_employee_home_bottom_nav));
        aiButton.setOnTouchListener(aiDragTouchListener);
        aiBubble.setOnTouchListener(aiDragTouchListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (homeReady) {
            showAiBubbleTemporarily();
        } else {
            startMockHomeLoad();
        }
    }

    @Override
    public void onPause() {
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
            aiBubble.animate().cancel();
            aiBubble.setVisibility(View.GONE);
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
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
        aiBubble = null;
        aiButton = null;
        aiGlow = null;
        homeContent = null;
        homeSkeleton = null;
        aiDragTouchListener = null;
        hideAiBubble = null;
        homeReady = false;
        homeLoadingRemainingMs = HOME_MOCK_LOAD_MS;
        super.onDestroyView();
    }

    private void startMockHomeLoad() {
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
        if (homeContent == null || homeSkeleton == null) {
            return;
        }
        homeReady = true;
        stopSkeletonPulse();
        homeContent.setAlpha(0f);
        homeContent.setVisibility(View.VISIBLE);
        if (aiButton != null) {
            aiButton.setVisibility(View.VISIBLE);
        }
        if (aiGlow != null) {
            aiGlow.setVisibility(View.VISIBLE);
        }

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
        if (aiBubble == null) {
            return;
        }
        if (hideAiBubble != null) {
            aiBubble.removeCallbacks(hideAiBubble);
        }
        aiBubble.animate().cancel();
        aiBubble.setAlpha(1f);
        aiBubble.setVisibility(View.VISIBLE);
        View bubble = aiBubble;
        bubble.post(() -> {
            if (aiBubble == bubble && aiDragTouchListener != null
                    && bubble.getVisibility() == View.VISIBLE) {
                aiDragTouchListener.positionBubble();
            }
        });
        hideAiBubble = () -> {
            if (!ValueAnimator.areAnimatorsEnabled()) {
                bubble.setVisibility(View.GONE);
                return;
            }
            bubble.animate()
                    .alpha(0f)
                    .setDuration(AI_BUBBLE_FADE_MS)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> bubble.setVisibility(View.GONE))
                    .start();
        };
        aiBubble.postDelayed(hideAiBubble, AI_BUBBLE_VISIBLE_MS);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_EMPTY_PREVIEW, emptyPreview);
        super.onSaveInstanceState(outState);
    }

    private void addRows(LinearLayout container, MockRow[] rows) {
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

    private class AiDragTouchListener implements View.OnTouchListener {
        private final View button;
        private final View glow;
        private final View content;
        private final View header;
        private final View bottomNav;
        private final int touchSlop;
        private final float gap;
        private final float edge;
        private float downX;
        private float downY;
        private float buttonStartX;
        private float buttonStartY;
        private boolean dragging;

        AiDragTouchListener(View button, View glow, View content, View header,
                View bottomNav) {
            this.button = button;
            this.glow = glow;
            this.content = content;
            this.header = header;
            this.bottomNav = bottomNav;
            touchSlop = ViewConfiguration.get(requireContext()).getScaledTouchSlop();
            float density = getResources().getDisplayMetrics().density;
            gap = 10f * density;
            edge = 8f * density;
        }

        @Override
        public boolean onTouch(View touched, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    buttonStartX = button.getX();
                    buttonStartY = button.getY();
                    dragging = false;
                    touched.setPressed(true);
                    if (touched == button && ValueAnimator.areAnimatorsEnabled()) {
                        button.animate().cancel();
                        button.animate().scaleX(0.94f).scaleY(0.94f)
                                .setDuration(100).start();
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
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
                        aiBubble.animate().cancel();
                        aiBubble.setAlpha(1f);
                    }
                    if (dragging) {
                        float left = content.getLeft() + edge;
                        float right = content.getRight() - edge - button.getWidth();
                        float top = content.getTop() + header.getBottom() + edge;
                        float bottom = content.getTop() + bottomNav.getTop()
                                - edge - button.getHeight();
                        button.setX(clamp(buttonStartX + dx, left, right));
                        button.setY(clamp(buttonStartY + dy, top, bottom));
                        glow.setX(button.getX() + (button.getWidth() - glow.getWidth()) / 2f);
                        glow.setY(button.getY() + (button.getHeight() - glow.getHeight()) / 2f);
                        positionBubble();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
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

        private void positionBubble() {
            if (aiBubble.getVisibility() != View.VISIBLE) {
                return;
            }
            float bubbleWidth = aiBubble.getWidth();
            float bubbleHeight = aiBubble.getHeight();
            float buttonX = button.getX();
            float buttonY = button.getY();
            float left = content.getLeft() + edge;
            float right = content.getRight() - edge;
            float leftX = buttonX - gap - bubbleWidth;
            float rightX = buttonX + button.getWidth() + gap;
            boolean fitsLeft = leftX >= left;
            boolean fitsRight = rightX + bubbleWidth <= right;

            if (fitsLeft || fitsRight) {
                boolean useLeft = fitsLeft && (!fitsRight
                        || buttonX + button.getWidth() / 2f >= (left + right) / 2f);
                aiBubble.setX(useLeft ? leftX : rightX);
                float minY = content.getTop() + header.getBottom() + edge;
                float maxY = content.getTop() + bottomNav.getTop() - edge - bubbleHeight;
                aiBubble.setY(clamp(buttonY + (button.getHeight() - bubbleHeight) / 2f,
                        minY, maxY));
                return;
            }

            aiBubble.setX(clamp(buttonX + (button.getWidth() - bubbleWidth) / 2f,
                    left, right - bubbleWidth));
            float above = buttonY - gap - bubbleHeight;
            float below = buttonY + button.getHeight() + gap;
            float minY = content.getTop() + header.getBottom() + edge;
            float maxY = content.getTop() + bottomNav.getTop() - edge - bubbleHeight;
            aiBubble.setY(above >= minY ? above : clamp(below, minY, maxY));
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
