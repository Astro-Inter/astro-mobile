package com.example.astro_mobile.chat.human;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.astro_mobile.R;
import com.bumptech.glide.Glide;
import com.example.astro_mobile.data.local.FlowPreferences;

public final class HumanChatFragment extends Fragment {
    public static final String PEER_NAME = "human_peer_name";
    public static final String PEER_ROLE = "human_peer_role";
    public static final String PEER_AVATAR_INDEX = "human_peer_avatar_index";
    public static final String PEER_IS_MANAGER = "human_peer_is_manager";
    public static final String PEER_ID = "human_peer_id";
    public static final String CONVERSATION_ID = "human_conversation_id";
    private HumanConversationsViewModel state;
    private HumanConversation conversation;
    private HumanMessageSwipeToReply swipeToReply;

    public HumanChatFragment() { super(R.layout.fragment_human_chat); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean manager = FlowPreferences.wasManagerFlow(requireContext());
        state = new ViewModelProvider(requireActivity(), HumanConversationsViewModel.Factory.createDefault(requireContext()))
                .get(HumanConversationsViewModel.class);

        // Pesquisa e detalhe do funcionário recuperam o mesmo histórico durante a sessão.
        Bundle args = requireArguments();
        String id = args.getString(CONVERSATION_ID);
        conversation = id == null ? null : state.find(id);
        if (conversation == null) {
            conversation = state.open(args.containsKey(PEER_ID) ? args.getLong(PEER_ID) : null,
                args.getString(PEER_NAME), args.getString(PEER_ROLE),
                args.getBoolean(PEER_IS_MANAGER, !manager), args.getInt(PEER_AVATAR_INDEX));
            if (conversation != null && id != null) conversation.conversationId = id;
        }
        if (conversation == null) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }
        ((TextView) view.findViewById(R.id.text_human_chat_name)).setText(conversation.name);
        ((TextView) view.findViewById(R.id.text_human_chat_role)).setText(conversation.role);
        int avatar = R.drawable.profile_avatar_placeholder;
        ImageView peer = view.findViewById(R.id.image_human_chat_peer);
        peer.setImageResource(avatar);
        Glide.with(this).load(conversation.profilePhotoUrl).circleCrop()
                    .placeholder(R.drawable.profile_avatar_placeholder)
                    .fallback(R.drawable.profile_avatar_placeholder)
                    .error(R.drawable.profile_avatar_placeholder).into(peer);
        ViewGroup.LayoutParams peerSize = peer.getLayoutParams();
        peerSize.width = peerSize.height = Math.round((manager ? 48 : 50) * getResources().getDisplayMetrics().density);
        peer.setLayoutParams(peerSize);

        view.findViewById(R.id.button_human_chat_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_human_chat_add).setOnClickListener(clicked ->
                Toast.makeText(requireContext(), R.string.human_chat_attachment_pending, Toast.LENGTH_SHORT).show());

        RecyclerView messages = view.findViewById(R.id.list_human_chat_messages);
        HumanMessagesAdapter adapter = new HumanMessagesAdapter(conversation, new HumanMessagesAdapter.Listener() {
            @Override public void onOptions(HumanMessage message, View anchor) {
                android.widget.PopupMenu menu = new android.widget.PopupMenu(requireContext(), anchor);
                if (conversation.pending == null && message.id != null) menu.getMenu().add(0, 1, 0, R.string.human_reply_action);
                menu.getMenu().add(0, 2, 1, R.string.human_reply_copy);
                menu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        selectReply(view, message);
                    } else {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                                requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("", message.text));
                    }
                    return true;
                });
                menu.show();
            }
            @Override public void onQuote(String id) {
                HumanMessage target = conversation.findMessage(id);
                if (target != null) messages.smoothScrollToPosition(conversation.messages.indexOf(target));
                else Toast.makeText(requireContext(), R.string.human_reply_load_older, Toast.LENGTH_SHORT).show();
            }
        });
        messages.setAdapter(adapter);
        swipeToReply = new HumanMessageSwipeToReply(conversation, message -> selectReply(view, message));
        swipeToReply.attach(messages);
        LinearLayoutManager layoutManager = (LinearLayoutManager) messages.getLayoutManager();
        messages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (layoutManager != null && layoutManager.findFirstVisibleItemPosition() == 0
                        && dy < 0 && conversation.hasMore && !conversation.loadingOlder) {
                    loadOlder(messages, layoutManager, adapter);
                }
            }
        });
        EditText input = view.findViewById(R.id.input_human_chat_message);
        input.setText(conversation.draft);
        view.findViewById(R.id.button_human_reply_cancel).setOnClickListener(clicked -> {
            if (conversation.pending != null) return;
            conversation.replyToMessageId = null;
            renderReply(view);
        });
        renderReply(view);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { conversation.draft = s.toString(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        view.findViewById(R.id.button_human_chat_send).setOnClickListener(clicked -> state.send(conversation));
        input.setOnEditorActionListener((text, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEND) return false;
            state.send(conversation);
            return true;
        });

        if (conversation.conversationId != null) {
            if (!conversation.loaded) showSkeleton(view, messages);
            startHistoryLoad(view, messages, adapter);
        } else conversation.loaded = true;
        int[] renderedCount = {conversation.messages.size()};
        state.changes().observe(getViewLifecycleOwner(), ignored -> {
            boolean atEnd = layoutManager == null || layoutManager.findLastVisibleItemPosition() >= renderedCount[0] - 2;
            adapter.notifyDataSetChanged();
            if (atEnd && conversation.messages.size() > renderedCount[0]) messages.scrollToPosition(conversation.messages.size() - 1);
            renderedCount[0] = conversation.messages.size();
            if (!input.getText().toString().equals(conversation.draft)) input.setText(conversation.draft);
            if (conversation.loaded) {
                view.findViewById(R.id.skeleton_human_chat).setVisibility(View.GONE);
                messages.setVisibility(View.VISIBLE);
                view.findViewById(R.id.container_human_chat_composer).setVisibility(View.VISIBLE);
            }
            renderConnection(view, input);
            renderReply(view);
        });
        state.connectionState().observe(getViewLifecycleOwner(), value -> {
            if (value == com.example.astro_mobile.data.chat.ChatConnectionState.SESSION_EXPIRED) {
                com.example.astro_mobile.auth.SessionNavigation.signOut(this);
            } else renderConnection(view, input);
        });
        view.findViewById(R.id.text_human_chat_connection).setOnClickListener(clicked -> state.retryConnection());
    }

    private void showSkeleton(View view, RecyclerView messages) {
        messages.setVisibility(View.INVISIBLE);
        view.findViewById(R.id.skeleton_human_chat).setVisibility(View.VISIBLE);
        view.findViewById(R.id.container_human_chat_composer).setVisibility(View.INVISIBLE);
    }

    private void startHistoryLoad(View view, RecyclerView messages, HumanMessagesAdapter adapter) {
        if (view == null || !isAdded()) return;
        int previousCount = conversation.messages.size();
        state.loadHistory(conversation, new HumanConversationsViewModel.HistoryCallback() {
            @Override public void onChanged() {
                if (getView() != view) return;
                adapter.notifyHistoryReplaced(previousCount);
                renderReply(view);
                view.findViewById(R.id.skeleton_human_chat).setVisibility(View.GONE);
                messages.setVisibility(View.VISIBLE);
                view.findViewById(R.id.container_human_chat_composer).setVisibility(View.VISIBLE);
                if (!conversation.messages.isEmpty()) messages.scrollToPosition(conversation.messages.size() - 1);
            }

            @Override public void onFailure(com.example.astro_mobile.data.api.FailureKind failure) {
                showHistoryFailure(failure, view);
            }
        });
    }

    private void loadOlder(RecyclerView messages, LinearLayoutManager layoutManager,
                           HumanMessagesAdapter adapter) {
        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        View firstChild = layoutManager.findViewByPosition(firstVisible);
        int offset = firstChild == null ? 0 : firstChild.getTop();
        int previousCount = conversation.messages.size();
        state.loadOlder(conversation, new HumanConversationsViewModel.HistoryCallback() {
            @Override public void onChanged() {
                if (getView() == null) return;
                int inserted = conversation.messages.size() - previousCount;
                adapter.notifyDataSetChanged();
                renderReply(getView());
                layoutManager.scrollToPositionWithOffset(firstVisible + inserted, offset);
            }

            @Override public void onFailure(com.example.astro_mobile.data.api.FailureKind failure) {
                showHistoryFailure(failure, getView());
            }
        });
    }

    private void showHistoryFailure(com.example.astro_mobile.data.api.FailureKind failure, View view) {
        if (view == null || !isAdded()) return;
        if (failure == com.example.astro_mobile.data.api.FailureKind.SESSION) {
            com.example.astro_mobile.auth.SessionNavigation.signOut(this);
            return;
        }
        NavHostFragment.findNavController(this).navigate(
                failure == com.example.astro_mobile.data.api.FailureKind.CONNECTION
                        ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
    }

    @Override public void onResume() {
        super.onResume();
        if (getView() == null || conversation == null || conversation.conversationId == null) return;
        androidx.navigation.NavBackStackEntry entry = NavHostFragment.findNavController(this)
                .getCurrentBackStackEntry();
        Boolean retry = entry.getSavedStateHandle().get("retry_human_chat_history");
        if (Boolean.TRUE.equals(retry)) {
            entry.getSavedStateHandle().remove("retry_human_chat_history");
            RecyclerView messages = getView().findViewById(R.id.list_human_chat_messages);
            startHistoryLoad(getView(), messages, (HumanMessagesAdapter) messages.getAdapter());
        }
    }

    private void renderConnection(View view, EditText input) {
        com.example.astro_mobile.data.chat.ChatConnectionState connection = state.connectionState().getValue();
        int label;
        if (connection == com.example.astro_mobile.data.chat.ChatConnectionState.CONNECTED) {
            label = conversation.pending == null ? (conversation.sendErrorCode == null
                    ? 0 : R.string.human_chat_send_error)
                    : conversation.pending.awaiting ? R.string.human_chat_sending : R.string.human_chat_uncertain;
        } else if (connection == com.example.astro_mobile.data.chat.ChatConnectionState.CONNECTING) {
            label = R.string.human_chat_connecting;
        } else if (connection == com.example.astro_mobile.data.chat.ChatConnectionState.RETRYING) {
            label = R.string.human_chat_reconnecting;
        } else if (connection == com.example.astro_mobile.data.chat.ChatConnectionState.REJECTED) {
            label = R.string.human_chat_rejected;
        } else if (connection == com.example.astro_mobile.data.chat.ChatConnectionState.TLS_FAILED) {
            label = R.string.human_chat_tls_failed;
        } else label = R.string.human_chat_disconnected;
        TextView status = view.findViewById(R.id.text_human_chat_connection);
        status.setVisibility(label == 0 ? View.GONE : View.VISIBLE);
        if (label != 0) status.setText(label);
        input.setEnabled(conversation.pending == null);
        view.findViewById(R.id.button_human_chat_send).setEnabled(
                connection == com.example.astro_mobile.data.chat.ChatConnectionState.CONNECTED
                        && (conversation.pending == null || !conversation.pending.awaiting));
    }

    private void selectReply(View view, HumanMessage message) {
        if (conversation.pending != null || message.id == null || getView() != view) return;
        conversation.replyToMessageId = message.id;
        renderReply(view);
        EditText input = view.findViewById(R.id.input_human_chat_message);
        input.requestFocus();
        android.view.inputmethod.InputMethodManager keyboard = (android.view.inputmethod.InputMethodManager)
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        keyboard.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
    }

    private void renderReply(View view) {
        HumanMessage target = conversation.findMessage(conversation.replyToMessageId);
        view.findViewById(R.id.container_human_reply).setVisibility(conversation.replyToMessageId == null ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.button_human_reply_cancel).setEnabled(conversation.pending == null);
        if (conversation.replyToMessageId == null) return;
        if (target == null) {
            ((TextView) view.findViewById(R.id.text_human_reply_author)).setText(R.string.human_reply_action);
            ((TextView) view.findViewById(R.id.text_human_reply_content)).setText(R.string.human_reply_missing);
            return;
        }
        ((TextView) view.findViewById(R.id.text_human_reply_author)).setText(getString(R.string.human_reply_to,
                target.sent ? getString(R.string.human_reply_you) : conversation.name));
        ((TextView) view.findViewById(R.id.text_human_reply_content)).setText(target.text);
    }

    @Override public void onStart() {
        super.onStart();
        if (conversation != null) state.setActiveConversation(conversation);
    }

    @Override public void onStop() {
        if (state != null) state.setActiveConversation(null);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (swipeToReply != null) {
            swipeToReply.detach();
            swipeToReply = null;
        }
        if (conversation != null && conversation.conversationId != null && state != null) {
            state.cancelHistory(conversation);
        }
        super.onDestroyView();
    }
}
