package com.example.astro_mobile.shared.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.astro_mobile.R;

/** Exibe o conteúdo completo de campos resumidos com reticências. */
public final class FullTextPopup {
    private FullTextPopup() { }

    public static void attach(TextView anchor) {
        anchor.setOnClickListener(view -> show(anchor));
    }

    private static void show(TextView anchor) {
        if (anchor.getText().length() == 0) return;
        Context context = anchor.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        int padding = Math.round(16 * density);
        int maxWidth = context.getResources().getDisplayMetrics().widthPixels - 2 * padding;
        TextView content = new TextView(context);
        content.setText(anchor.getText());
        content.setTextColor(ContextCompat.getColor(context, R.color.astro_light_background));
        content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        content.setTypeface(anchor.getTypeface());
        content.setPadding(padding, padding, padding, padding);
        content.setBackgroundResource(R.drawable.bg_profile_dialog);
        content.setTextIsSelectable(true);
        content.setMaxWidth(maxWidth);
        content.measure(View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        PopupWindow popup = new PopupWindow(content, content.getMeasuredWidth(), content.getMeasuredHeight(), true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setElevation(8 * density);
        popup.showAsDropDown(anchor, 0, -anchor.getHeight() - content.getMeasuredHeight());
    }
}
