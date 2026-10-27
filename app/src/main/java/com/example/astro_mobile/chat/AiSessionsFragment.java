package com.example.astro_mobile.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Sessões da conta, com paginação e leitura do histórico antes da navegação. */
public final class AiSessionsFragment extends Fragment {
    private ChatViewModel model;
    public AiSessionsFragment() { super(R.layout.fragment_ai_sessions); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navigation = NavHostFragment.findNavController(this);
        model = new ViewModelProvider(navigation.getBackStackEntry(HomeNavigation.destination(requireContext())),
                new ChatViewModel.Factory()).get(ChatViewModel.class);
        view.findViewById(R.id.button_ai_sessions_back).setOnClickListener(clicked -> navigation.navigateUp());
        AccessibilityDelegateCompat buttonAccessibility = new AccessibilityDelegateCompat() {
            @Override public void onInitializeAccessibilityNodeInfo(@NonNull View target, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(target, info);
                info.setClassName("android.widget.Button");
            }
        };
        ViewCompat.setAccessibilityDelegate(view.findViewById(R.id.button_ai_sessions_new), buttonAccessibility);
        EditText search = view.findViewById(R.id.input_ai_sessions_search);
        RecyclerView list = view.findViewById(R.id.list_ai_sessions);
        SessionAdapter adapter = new SessionAdapter(conversation -> {
            if (!model.openConversation(conversation.id))
                Toast.makeText(requireContext(), R.string.ai_sessions_wait, Toast.LENGTH_SHORT).show();
        });
        LinearLayoutManager layout = new LinearLayoutManager(requireContext());
        list.setLayoutManager(layout);
        list.setAdapter(adapter);
        list.setItemAnimator(null);
        Runnable loadNextPage = () -> {
            if (getView() != view || !isResumed() || model.isBusy()) return;
            if (list.hasPendingAdapterUpdates()) return;
            ChatViewModel.ListState page = model.getListState().getValue();
            if (page == null || page.loading || page.loadingMore || !page.hasMore || page.failure != null) return;
            int count = adapter.getItemCount();
            // Preenche uma tela curta e continua ao alcançar as últimas linhas visíveis.
            if (count == 0 || layout.findLastVisibleItemPosition() >= count - 2) model.loadMore();
        };
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView recycler, int dx, int dy) {
                recycler.post(loadNextPage);
            }
        });
        Runnable filter = () -> {
            List<ChatViewModel.Conversation> all = model.getConversations().getValue();
            String query = normalize(search.getText().toString().trim());
            List<ChatViewModel.Conversation> filtered = new ArrayList<>();
            if (all != null) for (ChatViewModel.Conversation conversation : all) {
                if (normalize(conversation.title + " " + conversation.preview).contains(query)) filtered.add(conversation);
            }
            adapter.submitList(filtered, () -> list.post(loadNextPage));
            ChatViewModel.ListState state = model.getListState().getValue();
            view.findViewById(R.id.container_ai_sessions_empty).setVisibility(filtered.isEmpty()
                    && state != null && !state.loading && !state.loadingMore && !state.hasMore
                    && state.failure == null ? View.VISIBLE : View.GONE);
            boolean searching = !query.isEmpty();
            ((TextView) view.findViewById(R.id.text_ai_sessions_empty_title)).setText(searching
                    ? R.string.ai_sessions_no_results_title : R.string.ai_sessions_empty_title);
            ((TextView) view.findViewById(R.id.text_ai_sessions_empty_body)).setText(searching
                    ? R.string.ai_sessions_no_results_body : R.string.ai_sessions_empty_body);
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { filter.run(); }
            @Override public void afterTextChanged(Editable text) { }
        });
        model.getConversations().observe(getViewLifecycleOwner(), ignored -> filter.run());
        model.getState().observe(getViewLifecycleOwner(), state -> {
            View newConversation = view.findViewById(R.id.button_ai_sessions_new);
            newConversation.setEnabled(!state.loading);
            newConversation.setAlpha(state.loading ? 0.4f : 1f);
            adapter.setEnabled(!state.loading);
        });
        model.getListState().observe(getViewLifecycleOwner(), state -> {
            renderProgress(view);
            filter.run();
        });
        model.getOpenState().observe(getViewLifecycleOwner(), state -> {
            renderProgress(view);
            if (state.opened) { model.consumeOpened(); openChat(navigation); }
            else if (state.failure != null) {
                Bundle errorArgs = new Bundle();
                errorArgs.putString("retry_ai_conversation", state.id);
                com.example.astro_mobile.data.ai.ChatFailure failure = state.failure;
                model.consumeOpened();
                showLoadError(failure, errorArgs);
            }
        });
        view.findViewById(R.id.button_ai_sessions_new).setOnClickListener(clicked -> {
            if (model.startNewConversation()) openChat(navigation);
        });
        // Navbar e espaço do teclado ficam na Activity, como nas demais telas autenticadas.
    }

    private void renderProgress(View view) {
        ChatViewModel.ListState list = model.getListState().getValue();
        ChatViewModel.OpenState open = model.getOpenState().getValue();
        if (list == null || open == null) return;
        boolean loading = list.loading || list.loadingMore || open.loading;
        // O skeleton acompanha a consulta/abertura real, sem atraso artificial.
        boolean showSkeleton = list.loading || open.loading;
        view.findViewById(R.id.skeleton_ai_sessions).setVisibility(showSkeleton ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.list_ai_sessions).setVisibility(showSkeleton ? View.INVISIBLE : View.VISIBLE);
        view.findViewById(R.id.container_ai_sessions_paging).setVisibility(list.loadingMore ? View.VISIBLE : View.GONE);
        if (!loading && list.failure != null) showLoadError(list.failure, null);
    }

    private void showLoadError(com.example.astro_mobile.data.ai.ChatFailure failure, Bundle args) {
        if (!isResumed()) return;
        NavController navigation = NavHostFragment.findNavController(this);
        if (navigation.getCurrentDestination().getId() != R.id.aiSessionsFragment) return;
        if (failure == com.example.astro_mobile.data.ai.ChatFailure.SESSION) {
            com.example.astro_mobile.auth.SessionNavigation.signOut(this);
            return;
        }
        navigation.navigate(failure == com.example.astro_mobile.data.ai.ChatFailure.CONNECTION
                ? R.id.connectionErrorFragment : R.id.genericErrorFragment, args);
    }

    @Override public void onResume() {
        super.onResume();
        String retry = NavHostFragment.findNavController(this).getCurrentBackStackEntry()
                .getSavedStateHandle().remove("retry_ai_conversation");
        if (model != null) {
            // Repete a abertura específica quando o erro veio do histórico dessa conversa.
            if (retry != null) model.openConversation(retry);
            else model.refreshConversations();
        }
    }

    @Override public void onDestroyView() {
        if (model != null) model.cancelOpening();
        super.onDestroyView();
    }

    private void openChat(NavController navigation) {
        if (!navigation.popBackStack(R.id.chatFragment, false)) {
            navigation.navigate(R.id.action_ai_sessions_to_chat, AuthArgs.copy(getArguments()));
        }
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private interface OpenConversation { void open(ChatViewModel.Conversation conversation); }

    private static final class SessionAdapter extends ListAdapter<ChatViewModel.Conversation, SessionAdapter.Holder> {
        private static final DiffUtil.ItemCallback<ChatViewModel.Conversation> DIFF = new DiffUtil.ItemCallback<>() {
            @Override public boolean areItemsTheSame(@NonNull ChatViewModel.Conversation before, @NonNull ChatViewModel.Conversation after) {
                return before.id.equals(after.id);
            }
            @Override public boolean areContentsTheSame(@NonNull ChatViewModel.Conversation before, @NonNull ChatViewModel.Conversation after) {
                return before.title.equals(after.title) && before.preview.equals(after.preview);
            }
        };
        private final OpenConversation open;
        private boolean enabled = true;
        SessionAdapter(OpenConversation open) { super(DIFF); this.open = open; }

        void setEnabled(boolean enabled) {
            if (this.enabled == enabled) return;
            this.enabled = enabled;
            notifyItemRangeChanged(0, getItemCount());
        }

        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_human_conversation, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            ChatViewModel.Conversation conversation = getItem(position);
            holder.title.setText(conversation.title.isEmpty() ? holder.itemView.getContext().getString(R.string.ai_sessions_draft)
                    : conversation.title.replaceAll("\\s+", " "));
            holder.preview.setText(conversation.preview.replaceAll("\\s+", " "));
            // Compartilha o card humano; somente o avatar e os dados vêm da sessão da IA.
            holder.button.setEnabled(enabled);
            holder.button.setOnClickListener(clicked -> open.open(conversation));
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView title, preview;
            final View button;
            Holder(View view) {
                super(view);
                title = view.findViewById(R.id.text_human_conversation_name);
                title.setMaxLines(1);
                title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                preview = view.findViewById(R.id.text_human_conversation_preview);
                button = view.findViewById(R.id.button_human_conversation);
                ImageView avatar = view.findViewById(R.id.image_human_conversation_avatar);
                avatar.setImageResource(R.drawable.home_mascot);
                avatar.setBackgroundResource(R.drawable.bg_conversations_ai_avatar);
                int padding = Math.round(7 * view.getResources().getDisplayMetrics().density);
                avatar.setPadding(padding, padding, padding, padding);
            }
        }
    }
}
