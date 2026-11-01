package com.example.astro_mobile.chat;

import android.text.Layout;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Permite ler títulos que não couberam na lista de conversas. */
public final class ConversationTitlePopup {
    private ConversationTitlePopup() { }

    public static void attach(View row, TextView title) {
        row.setOnLongClickListener(view -> {
            Layout layout = title.getLayout();
            if (layout == null) return false;
            boolean truncated = false;
            for (int line = 0; line < layout.getLineCount(); line++) {
                if (layout.getEllipsisCount(line) > 0) {
                    truncated = true;
                    break;
                }
            }
            if (!truncated) return false;
            new MaterialAlertDialogBuilder(view.getContext())
                    .setMessage(title.getText())
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        });
    }
}
