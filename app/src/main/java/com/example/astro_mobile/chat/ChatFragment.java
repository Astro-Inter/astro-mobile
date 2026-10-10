package com.example.astro_mobile.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.data.ai.ChatRequest;
import com.example.astro_mobile.data.ai.ChatFailure;
import com.example.astro_mobile.auth.EmailVerificationViewModel;
import com.example.astro_mobile.auth.MockSession;
import com.example.astro_mobile.auth.SessionViewModel;
import com.example.astro_mobile.data.local.FlowPreferences;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

public final class ChatFragment extends Fragment {
    private ChatCalendarLinkHandler calendarLinks;
    private ChatViewModel model;
    public ChatFragment() { super(R.layout.fragment_chat); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navigation = NavHostFragment.findNavController(this);
        view.findViewById(R.id.button_chat_sessions).setOnClickListener(clicked -> {
            if (!navigation.popBackStack(R.id.aiSessionsFragment, false)) {
                navigation.navigate(R.id.action_chat_to_ai_sessions, AuthArgs.copy(getArguments()));
            }
        });
        model = new ViewModelProvider(navigation.getBackStackEntry(HomeNavigation.destination(requireContext())),
                new ChatViewModel.Factory()).get(ChatViewModel.class);
        EditText input = view.findViewById(R.id.input_chat_message);
        // O rascunho pertence à sessão no ViewModel; não restaura texto de outra tela.
        View sendButton = view.findViewById(R.id.button_chat_send);
        RecyclerView messages = view.findViewById(R.id.list_chat_messages);
        calendarLinks = new ChatCalendarLinkHandler(requireContext());
        ChatMessageAdapter adapter = new ChatMessageAdapter(ChatMarkdown.create(requireContext(), calendarLinks::connect));
        messages.setLayoutManager(new LinearLayoutManager(requireContext()));
        messages.setAdapter(adapter);
        messages.setItemAnimator(null);
        input.setFilters(new InputFilter[]{new CodePointLengthFilter(ChatRequest.MAX_MESSAGE_LENGTH)});
        input.setText(model.getDraft());
        input.setSelection(input.length());
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                model.setDraft(s.toString());
                ChatViewModel.State state = model.getState().getValue();
                boolean enabled = state != null && state.canSend() && !s.toString().trim().isEmpty();
                sendButton.setEnabled(enabled);
                sendButton.setAlpha(enabled ? 1f : 0.4f);
                int length = Character.codePointCount(s, 0, s.length());
                TextView countView = view.findViewById(R.id.text_chat_character_count);
                countView.setVisibility(length >= 3600 ? View.VISIBLE : View.GONE);
                countView.setText(getString(R.string.chat_character_count, length));
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        sendButton.setOnClickListener(clicked -> model.send(input.getText().toString()));
        input.setOnEditorActionListener((text, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEND) return false;
            sendButton.performClick();
            return true;
        });
        // As perguntas sugeridas enviam o mesmo texto exibido no botão.
        view.findViewById(R.id.button_chat_suggestion_nrs).setOnClickListener(clicked ->
                model.send(getString(R.string.chat_suggestion_nrs)));
        view.findViewById(R.id.button_chat_suggestion_dashboards).setOnClickListener(clicked ->
                model.send(getString(R.string.chat_suggestion_dashboards)));
        // O Figma mostra anexos, mas seu envio ainda não foi implementado.
        view.findViewById(R.id.button_chat_add).setOnClickListener(clicked ->
                Toast.makeText(requireContext(), R.string.employee_home_mock_unavailable,
                        Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.button_chat_close).setOnClickListener(clicked -> {
            if (ViewCompat.getWindowInsetsController(view) != null) {
                ViewCompat.getWindowInsetsController(view).hide(WindowInsetsCompat.Type.ime());
            }
            navigation.navigateUp();
        });
        view.findViewById(R.id.button_chat_retry).setOnClickListener(clicked -> {
            ChatViewModel.State current = model.getState().getValue();
            if (current != null && current.failure == ChatFailure.SESSION) {
                new ViewModelProvider(requireActivity(), SessionViewModel.Factory.createDefault())
                        .get(SessionViewModel.class).signOut();
                MockSession.clear(requireContext());
                FlowPreferences.clear(requireContext());
                new ViewModelProvider(requireActivity(), EmailVerificationViewModel.Factory.createDefault())
                        .get(EmailVerificationViewModel.class).clearForLogout();
                if (ViewCompat.getWindowInsetsController(view) != null) {
                    ViewCompat.getWindowInsetsController(view).hide(WindowInsetsCompat.Type.ime());
                }
                navigation.navigate(R.id.action_chat_to_email_identification);
            } else if (current != null && (current.failure == ChatFailure.CONFLICT || !model.hasPendingMessage())) {
                model.refreshCurrentHistory();
            } else if (current != null && (current.sessionId == null || model.isRetryChecked())) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setMessage(current.sessionId == null ? R.string.ai_sessions_unknown_outcome : R.string.ai_sessions_retry_confirm)
                        .setNegativeButton(R.string.ai_sessions_cancel, null)
                        .setNeutralButton(R.string.ai_sessions_open, (dialog, which) -> view.findViewById(R.id.button_chat_sessions).performClick())
                        .setPositiveButton(R.string.ai_sessions_resend, (dialog, which) -> model.retry()).show();
            } else {
                model.retry();
            }
        });
        model.getState().observe(getViewLifecycleOwner(), state -> {
            boolean atBottom = !messages.canScrollVertically(1);
            adapter.submitList(state.messages, () -> {
                if (atBottom && !state.messages.isEmpty()) messages.scrollToPosition(state.messages.size() - 1);
            });
            view.findViewById(R.id.container_chat_welcome).setVisibility(state.messages.isEmpty() ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.container_chat_loading).setVisibility(state.loading ? View.VISIBLE : View.GONE);
            ((TextView) view.findViewById(R.id.text_chat_loading)).setText(state.action == ChatViewModel.Action.HISTORY
                    ? R.string.ai_sessions_history_loading : state.action == ChatViewModel.Action.RESUME
                    ? R.string.ai_sessions_resuming : state.waitingLong ? R.string.chat_loading_slow : R.string.chat_loading);
            view.findViewById(R.id.container_chat_error).setVisibility(state.failure == null ? View.GONE : View.VISIBLE);
            if (state.failure != null) {
                ((TextView) view.findViewById(R.id.text_chat_error)).setText(errorMessage(state.failure));
            }
            TextView retry = view.findViewById(R.id.button_chat_retry);
            retry.setVisibility(state.canRetry() || state.failure == ChatFailure.SESSION || state.failure == ChatFailure.CONFLICT ? View.VISIBLE : View.GONE);
            retry.setEnabled(!state.loading);
            retry.setText(state.failure == ChatFailure.SESSION ? R.string.chat_sign_in
                    : state.failure == ChatFailure.CONFLICT || !model.hasPendingMessage() ? R.string.ai_sessions_refresh : R.string.chat_retry);
            view.findViewById(R.id.button_chat_suggestion_nrs).setEnabled(state.canSend());
            view.findViewById(R.id.button_chat_suggestion_dashboards).setEnabled(state.canSend());
            // Não sobrescreve uma próxima mensagem digitada enquanto espera a resposta.
            if (!input.getText().toString().equals(model.getDraft())) {
                input.setText(model.getDraft());
                input.setSelection(input.length());
            }
            boolean enabled = state.canSend() && !input.getText().toString().trim().isEmpty();
            sendButton.setEnabled(enabled);
            sendButton.setAlpha(enabled ? 1f : 0.4f);
        });
    }

    @Override public void onResume() {
        super.onResume();
        if (model != null) model.refreshCurrentHistory();
    }

    @Override public void onDestroyView() {
        if (calendarLinks != null) { calendarLinks.close(); calendarLinks = null; }
        super.onDestroyView();
    }

    private int errorMessage(ChatFailure failure) {
        switch (failure) {
            case CONNECTION: return R.string.chat_connection_error;
            case TIMEOUT: return R.string.chat_timeout_error;
            case SESSION: return R.string.chat_session_error;
            case FORBIDDEN: return R.string.chat_forbidden_error;
            case RATE_LIMIT: return R.string.chat_rate_limit_error;
            case INVALID_MESSAGE: return R.string.chat_invalid_message_error;
            case INVALID_RESPONSE: return R.string.chat_invalid_response_error;
            case NOT_FOUND: return R.string.ai_sessions_not_found;
            case CONFLICT: return R.string.ai_sessions_conflict;
            default: return R.string.chat_server_error;
        }
    }
}
