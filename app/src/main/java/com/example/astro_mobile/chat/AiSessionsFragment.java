package com.example.astro_mobile.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
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
    private String selectedFilter = "all";
    public AiSessionsFragment() { super(R.layout.fragment_ai_sessions); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) selectedFilter = savedInstanceState.getString("sessions_filter", "all");
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
        for (int id : new int[]{R.id.button_ai_sessions_filter_all, R.id.button_ai_sessions_filter_active,
                R.id.button_ai_sessions_filter_closed, R.id.button_ai_sessions_new}) {
            ViewCompat.setAccessibilityDelegate(view.findViewById(id), buttonAccessibility);
        }
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
                boolean matchesStatus = "all".equals(selectedFilter)
                        || ("active".equals(selectedFilter) && !"encerrada".equals(conversation.status))
                        || ("closed".equals(selectedFilter) && "encerrada".equals(conversation.status));
                if (matchesStatus && normalize(conversation.title + " " + conversation.preview).contains(query)) filtered.add(conversation);
            }
            adapter.submitList(filtered, () -> list.post(loadNextPage));
            view.findViewById(R.id.button_ai_sessions_filter_all).setSelected("all".equals(selectedFilter));
            view.findViewById(R.id.button_ai_sessions_filter_active).setSelected("active".equals(selectedFilter));
            view.findViewById(R.id.button_ai_sessions_filter_closed).setSelected("closed".equals(selectedFilter));
            ChatViewModel.ListState state = model.getListState().getValue();
            view.findViewById(R.id.container_ai_sessions_empty).setVisibility(filtered.isEmpty()
                    && state != null && !state.loading && !state.loadingMore && !state.hasMore
                    && state.failure == null ? View.VISIBLE : View.GONE);
            boolean searching = !query.isEmpty() || !"all".equals(selectedFilter);
            ((TextView) view.findViewById(R.id.text_ai_sessions_empty_title)).setText(searching
                    ? R.string.ai_sessions_no_results_title : R.string.ai_sessions_empty_title);
            ((TextView) view.findViewById(R.id.text_ai_sessions_empty_body)).setText(searching
                    ? R.string.ai_sessions_no_results_body : R.string.ai_sessions_empty_body);
        };
        view.findViewById(R.id.button_ai_sessions_filter_all).setOnClickListener(clicked -> { selectedFilter = "all"; filter.run(); });
        view.findViewById(R.id.button_ai_sessions_filter_active).setOnClickListener(clicked -> { selectedFilter = "active"; filter.run(); });
        view.findViewById(R.id.button_ai_sessions_filter_closed).setOnClickListener(clicked -> { selectedFilter = "closed"; filter.run(); });
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
                Toast.makeText(requireContext(), state.failure == com.example.astro_mobile.data.ai.ChatFailure.NOT_FOUND
                        ? R.string.ai_sessions_not_found : R.string.ai_sessions_load_error, Toast.LENGTH_LONG).show();
                model.consumeOpened();
            }
        });
        view.findViewById(R.id.button_ai_sessions_page_retry).setOnClickListener(clicked -> model.retryConversations());
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
        view.findViewById(R.id.progress_ai_sessions).setVisibility(list.loading || open.loading ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.container_ai_sessions_paging).setVisibility(list.loadingMore ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.button_ai_sessions_page_retry).setVisibility(!loading && list.failure != null
                && list.failure != com.example.astro_mobile.data.ai.ChatFailure.SESSION
                && list.failure != com.example.astro_mobile.data.ai.ChatFailure.FORBIDDEN ? View.VISIBLE : View.GONE);
        TextView notice = view.findViewById(R.id.text_ai_sessions_notice);
        notice.setVisibility(list.loading || open.loading || list.failure != null ? View.VISIBLE : View.GONE);
        notice.setTextColor(requireContext().getColor(!loading && list.failure != null ? R.color.astro_error : R.color.astro_input_hint));
        notice.setText(open.loading ? R.string.ai_sessions_history_loading : loading ? R.string.ai_sessions_loading
                : list.failure == com.example.astro_mobile.data.ai.ChatFailure.SESSION ? R.string.chat_session_error : R.string.ai_sessions_load_error);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("sessions_filter", selectedFilter);
    }

    @Override public void onResume() {
        super.onResume();
        if (model != null) model.refreshConversations();
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
                return before.title.equals(after.title) && before.preview.equals(after.preview)
                        && before.status.equals(after.status) && before.updatedAt == after.updatedAt
                        && before.active == after.active && before.responding == after.responding;
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
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_session, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            ChatViewModel.Conversation conversation = getItem(position);
            holder.title.setText(conversation.title.isEmpty() ? holder.itemView.getContext().getString(R.string.ai_sessions_draft)
                    : conversation.title.replaceAll("\\s+", " "));
            holder.preview.setText(conversation.preview.replaceAll("\\s+", " "));
            int statusLabel = conversation.responding ? R.string.ai_sessions_responding
                    : "encerrada".equals(conversation.status) ? R.string.ai_sessions_closed
                    : "encerrando".equals(conversation.status) ? R.string.ai_sessions_ending
                    : conversation.active ? R.string.ai_sessions_active : R.string.ai_sessions_status_active;
            String status = holder.itemView.getContext().getString(statusLabel);
            String date = DateUtils.formatDateTime(holder.itemView.getContext(), conversation.updatedAt,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH);
            holder.status.setText(conversation.updatedAt == 0 ? status
                    : holder.itemView.getContext().getString(R.string.ai_sessions_metadata, status, date));
            holder.itemView.setSelected(conversation.active);
            holder.itemView.setEnabled(enabled);
            holder.status.setTextColor(holder.itemView.getContext().getColor(conversation.active
                    ? R.color.astro_light_background : R.color.astro_input_hint));
            holder.itemView.setOnClickListener(clicked -> open.open(conversation));
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView title, preview, status;
            Holder(View view) {
                super(view);
                title = view.findViewById(R.id.text_ai_session_title);
                preview = view.findViewById(R.id.text_ai_session_preview);
                status = view.findViewById(R.id.text_ai_session_status);
            }
        }
    }
}
