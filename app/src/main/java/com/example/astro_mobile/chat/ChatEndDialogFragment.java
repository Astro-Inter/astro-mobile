package com.example.astro_mobile.chat;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.shared.navigation.HomeNavigation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Confirmação vinculada à sessão que o usuário escolheu encerrar. */
public final class ChatEndDialogFragment extends DialogFragment {
    static final String TAG = "chat_end_confirmation";
    private static final String SESSION_ID = "session_id";

    static ChatEndDialogFragment create(String sessionId) {
        ChatEndDialogFragment dialog = new ChatEndDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(SESSION_ID, sessionId);
        dialog.setArguments(arguments);
        return dialog;
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String sessionId = requireArguments().getString(SESSION_ID);
        NavController navigation = NavHostFragment.findNavController(requireParentFragment());
        ChatViewModel model = new ViewModelProvider(navigation.getBackStackEntry(HomeNavigation.destination(requireContext())),
                new ChatViewModel.Factory()).get(ChatViewModel.class);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_chat_end, null);
        TextView title = content.findViewById(R.id.text_chat_end_title);
        TextView confirm = content.findViewById(R.id.button_chat_end_confirm);
        content.findViewById(R.id.button_chat_end_close).setOnClickListener(clicked -> dismiss());
        content.findViewById(R.id.button_chat_end_cancel).setOnClickListener(clicked -> dismiss());
        confirm.setOnClickListener(clicked -> {
            ChatViewModel.State current = model.getState().getValue();
            if (matches(current, sessionId) && !current.loading) {
                confirm.setEnabled(false);
                model.endSession();
            }
            dismiss();
        });
        model.getState().observe(this, current -> {
            if (!matches(current, sessionId)) {
                dismiss();
                return;
            }
            boolean ending = "encerrando".equals(current.status);
            title.setText(ending ? R.string.ai_sessions_finish_end_title : R.string.ai_sessions_end_title);
            confirm.setText(ending ? R.string.ai_sessions_finish_end : R.string.ai_sessions_end);
            confirm.setEnabled(!current.loading);
        });
        return new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_chat_end_dialog))
                .setBackgroundInsetStart(0).setBackgroundInsetEnd(0)
                .setBackgroundInsetTop(0).setBackgroundInsetBottom(0)
                .create();
    }

    private static boolean matches(ChatViewModel.State state, String sessionId) {
        return state != null && sessionId != null && sessionId.equals(state.sessionId)
                && !state.unavailable && !"encerrada".equals(state.status);
    }

    @Override public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window == null) return;
        float density = getResources().getDisplayMetrics().density;
        int available = requireActivity().getWindowManager().getCurrentWindowMetrics().getBounds().width();
        int width = Math.min(Math.round(370 * density), available - Math.round(48 * density));
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setDimAmount(0.65f);
    }
}
