package com.example.astro_mobile.shared.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.astro_mobile.R;

public final class ActionConfirmationDialog {
    private ActionConfirmationDialog() { }

    // A ação só é executada após a pessoa confirmar no modal.
    public static void show(Fragment fragment, @StringRes int title, Runnable action) {
        Dialog dialog = new Dialog(fragment.requireContext());
        LifecycleOwner owner = fragment.getViewLifecycleOwner();
        DefaultLifecycleObserver observer = new DefaultLifecycleObserver() {
            @Override public void onDestroy(LifecycleOwner ignored) { dialog.dismiss(); }
        };
        owner.getLifecycle().addObserver(observer);
        dialog.setOnDismissListener(ignored -> owner.getLifecycle().removeObserver(observer));
        dialog.setContentView(R.layout.dialog_action_confirmation);
        ((TextView) dialog.findViewById(R.id.text_action_confirmation_title)).setText(title);
        dialog.findViewById(R.id.button_action_confirmation_close).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.button_action_confirmation_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.button_action_confirmation_accept).setOnClickListener(v -> {
            dialog.dismiss();
            if (fragment.isResumed()) action.run();
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            float density = fragment.getResources().getDisplayMetrics().density;
            int width = Math.min(fragment.getResources().getDisplayMetrics().widthPixels - Math.round(60 * density), Math.round(370 * density));
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.8f);
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }
}
