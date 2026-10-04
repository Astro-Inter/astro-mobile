package com.example.astro_mobile.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

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
import com.example.astro_mobile.data.ai.ChatRequest;

public final class ChatFragment extends Fragment {
    public ChatFragment() { super(R.layout.fragment_chat); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navigation = NavHostFragment.findNavController(this);
        ChatViewModel model = new ViewModelProvider(navigation.getBackStackEntry(R.id.employeeHomeFragment),
                new ChatViewModel.Factory()).get(ChatViewModel.class);
        EditText input = view.findViewById(R.id.input_chat_message);
        View sendButton = view.findViewById(R.id.button_chat_send);
        RecyclerView messages = view.findViewById(R.id.list_chat_messages);
        ChatMessageAdapter adapter = new ChatMessageAdapter(ChatMarkdown.create(requireContext()));
        messages.setLayoutManager(new LinearLayoutManager(requireContext()));
        messages.setAdapter(adapter);
        messages.setItemAnimator(null);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ChatRequest.MAX_MESSAGE_LENGTH)});
        input.setText(model.getDraft());
        input.setSelection(input.length());
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                model.setDraft(s.toString());
                ChatViewModel.State state = model.getState().getValue();
                boolean enabled = state != null && !state.loading && !s.toString().trim().isEmpty();
                sendButton.setEnabled(enabled);
                sendButton.setAlpha(enabled ? 1f : 0.4f);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        sendButton.setOnClickListener(clicked -> model.send(input.getText().toString()));
        input.setOnEditorActionListener((text, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEND) return false;
            sendButton.performClick();
            return true;
        });
        view.findViewById(R.id.button_chat_suggestion_nrs).setOnClickListener(clicked ->
                model.send(getString(R.string.chat_suggestion_nrs)));
        view.findViewById(R.id.button_chat_suggestion_dashboards).setOnClickListener(clicked ->
                model.send(getString(R.string.chat_suggestion_dashboards)));
        view.findViewById(R.id.button_chat_close).setOnClickListener(clicked -> {
            if (ViewCompat.getWindowInsetsController(view) != null) {
                ViewCompat.getWindowInsetsController(view).hide(WindowInsetsCompat.Type.ime());
            }
            navigation.navigateUp();
        });
        model.getState().observe(getViewLifecycleOwner(), state -> {
            boolean atBottom = !messages.canScrollVertically(1);
            adapter.submitList(state.messages, () -> {
                if (atBottom && !state.messages.isEmpty()) messages.scrollToPosition(state.messages.size() - 1);
            });
            view.findViewById(R.id.container_chat_welcome).setVisibility(state.messages.isEmpty() ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.container_chat_loading).setVisibility(state.loading ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.text_chat_error).setVisibility(state.failure == null ? View.GONE : View.VISIBLE);
            // Não sobrescreve uma próxima mensagem digitada enquanto espera a resposta.
            if (!input.getText().toString().equals(model.getDraft())) {
                input.setText(model.getDraft());
                input.setSelection(input.length());
            }
            boolean enabled = !state.loading && !input.getText().toString().trim().isEmpty();
            sendButton.setEnabled(enabled);
            sendButton.setAlpha(enabled ? 1f : 0.4f);
        });
        // O host já aplica as barras do sistema; aqui se soma apenas a área do teclado.
        ViewCompat.setOnApplyWindowInsetsListener(view, (target, insets) -> {
            int keyboard = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navigationBar = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            target.setPadding(0, 0, 0, Math.max(0, keyboard - navigationBar));
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
