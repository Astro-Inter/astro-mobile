package com.example.astro_mobile.shared.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/** Evita que gestos horizontais sejam interpretados como atualização. */
public final class DirectionalSwipeRefreshLayout extends SwipeRefreshLayout {
    private final int touchSlop;
    private float downX;
    private float downY;
    private boolean horizontalGesture;

    public DirectionalSwipeRefreshLayout(Context context) { this(context, null); }

    public DirectionalSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                horizontalGesture = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getX() - downX);
                float dy = Math.abs(event.getY() - downY);
                if (horizontalGesture || (dx > touchSlop && dx >= dy)) {
                    horizontalGesture = true;
                    return false;
                }
                if (dy < dx * 1.5f) return false;
                break;
            default:
                break;
        }
        return super.onInterceptTouchEvent(event);
    }
}
