package com.example.astro_mobile.shared.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Mantém filtros uniformes em uma linha, com rolagem quando necessário. */
public final class ResponsiveFilters {
    private ResponsiveFilters() { }

    public static void bind(HorizontalScrollView scroll) {
        LinearLayout row = (LinearLayout) scroll.getChildAt(0);
        Runnable resize = () -> {
            int count = row.getChildCount();
            int available = scroll.getWidth() - scroll.getPaddingLeft() - scroll.getPaddingRight();
            if (count == 0 || available <= 0) return;
            int margins = 0;
            int minWidth = 0;
            for (int i = 0; i < count; i++) {
                TextView label = (TextView) row.getChildAt(i);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) label.getLayoutParams();
                margins += params.getMarginStart() + params.getMarginEnd();
                minWidth = Math.max(minWidth, (int) Math.ceil(label.getPaint().measureText(label.getText().toString()))
                        + label.getPaddingLeft() + label.getPaddingRight() + 1);
            }
            int width = Math.max(minWidth, Math.max(0, available - margins) / count);
            for (int i = 0; i < count; i++) {
                View label = row.getChildAt(i);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) label.getLayoutParams();
                if (params.width != width || params.weight != 0 || params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                    params.width = width;
                    params.weight = 0;
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    label.setLayoutParams(params);
                }
            }
        };
        for (int i = 0; i < row.getChildCount(); i++) {
            TextView label = (TextView) row.getChildAt(i);
            label.setSingleLine(true);
            label.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence text, int start, int before, int count) { scroll.post(resize); }
                @Override public void afterTextChanged(Editable text) { }
            });
        }
        scroll.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft) resize.run();
        });
        scroll.post(resize);
    }
}
