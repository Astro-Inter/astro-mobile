package com.example.astro_mobile.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.chat.human.HumanChatFragment;
import com.example.astro_mobile.chat.human.HumanConversationAdapter;
import com.example.astro_mobile.chat.human.HumanConversationsViewModel;
import com.example.astro_mobile.data.local.FlowPreferences;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

public final class ConversationsFragment extends Fragment {
    private String query = "";
    private Runnable finishLoading;
    private boolean contactsLoaded;
    private boolean contacts;
    private boolean loadRequested;
    private HumanConversationsViewModel state;
    private Runnable renderList;

    public ConversationsFragment() { super(R.layout.fragment_conversations); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navigation = NavHostFragment.findNavController(this);
        contacts = navigation.getCurrentDestination().getId() == R.id.newConversationFragment;
        boolean manager = FlowPreferences.wasManagerFlow(requireContext());
        state = new ViewModelProvider(requireActivity())
                .get(HumanConversationsViewModel.class);

        // Os dois destinos compartilham o layout; a pesquisa não tem introdução nem IA fixada.
        view.findViewById(R.id.container_conversations_heading).setVisibility(contacts ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.button_conversations_new).setVisibility(contacts ? View.GONE : View.VISIBLE);
        ((TextView) view.findViewById(R.id.text_conversations_subtitle)).setText(manager
                ? R.string.conversations_manager_subtitle : R.string.conversations_subtitle);
        view.findViewById(R.id.button_conversations_back).setOnClickListener(clicked -> navigation.popBackStack());
        view.findViewById(R.id.button_conversations_back).setContentDescription(getString(contacts
                ? R.string.conversations_return_list : R.string.conversations_back));
        view.findViewById(R.id.button_conversations_close).setOnClickListener(clicked ->
                navigation.popBackStack(HomeNavigation.destination(requireContext()), false));
        view.findViewById(R.id.button_conversations_new_action).setOnClickListener(clicked ->
                navigation.navigate(R.id.action_conversations_to_new, AuthArgs.copy(getArguments())));
        view.findViewById(R.id.button_conversations_ai).setOnClickListener(clicked ->
                navigation.navigate(R.id.action_conversations_to_ai_sessions, AuthArgs.copy(getArguments())));

        RecyclerView list = view.findViewById(R.id.list_conversations);
        HumanConversationAdapter adapter = new HumanConversationAdapter(contacts, conversation -> {
            Bundle args = AuthArgs.copy(getArguments());
            args.putString(HumanChatFragment.PEER_NAME, conversation.name);
            args.putString(HumanChatFragment.PEER_ROLE, conversation.role);
            args.putInt(HumanChatFragment.PEER_AVATAR_INDEX, conversation.avatarIndex);
            args.putBoolean(HumanChatFragment.PEER_IS_MANAGER, conversation.manager);
            if (conversation.conversationId != null) {
                args.putString(HumanChatFragment.CONVERSATION_ID, conversation.conversationId);
            }
            navigation.navigate(contacts ? R.id.action_new_conversation_to_human_chat
                    : R.id.action_conversations_to_human_chat, args);
        });
        list.setAdapter(adapter);
        list.setItemAnimator(null);
        EditText search = view.findViewById(R.id.input_conversations_search);
        search.setHint(contacts ? (manager ? R.string.conversations_search_employees
                : R.string.conversations_search_managers) : R.string.conversations_search_hint);
        search.setText(query);
        renderList = () -> {
            adapter.submit(state.visible(manager, contacts, query));
            boolean showAi = !contacts && HumanConversationsViewModel.matches(
                    getString(R.string.conversations_ai_title), query);
            view.findViewById(R.id.button_conversations_ai).setVisibility(showAi ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.divider_conversations_ai).setVisibility(showAi ? View.VISIBLE : View.GONE);
            if (!contacts) {
                boolean firstLoad = state.isLoading() && !state.isLoaded();
                view.findViewById(R.id.container_conversations_skeleton)
                        .setVisibility(firstLoad ? View.VISIBLE : View.GONE);
                list.setVisibility(firstLoad ? View.INVISIBLE : View.VISIBLE);
                showLoadError();
            }
        };
        // Cada edição atualiza a lista imediatamente, sem timer ou botão de pesquisa.
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString();
                renderList.run();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        renderList.run();

        // Apenas a pesquisa de contatos simula a futura carga com um segundo de skeleton.
        if (contacts && !contactsLoaded) {
            View skeleton = view.findViewById(R.id.container_conversations_skeleton);
            list.setVisibility(View.INVISIBLE);
            skeleton.setVisibility(View.VISIBLE);
            finishLoading = () -> {
                contactsLoaded = true;
                skeleton.setVisibility(View.GONE);
                list.setVisibility(View.VISIBLE);
                finishLoading = null;
            };
            view.postDelayed(finishLoading, 1_000L);
        }
    }

    @Override public void onResume() {
        super.onResume();
        if (contacts) return;
        NavController navigation = NavHostFragment.findNavController(this);
        boolean retry = navigation.getCurrentBackStackEntry().getSavedStateHandle()
                .remove("retry_human_chat_list") != null;
        // Voltar do erro ou do chat conserva os dados; só uma nova entrada ou retry consulta a API.
        if (retry || !loadRequested) {
            loadRequested = true;
            state.loadConversations(renderList);
        } else renderList.run();
    }

    private void showLoadError() {
        if (!isResumed() || state.getFailure() == null) return;
        FailureKind failure = state.getFailure();
        state.clearFailure();
        if (failure == FailureKind.SESSION) SessionNavigation.signOut(this);
        else NavHostFragment.findNavController(this).navigate(failure == FailureKind.CONNECTION
                ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
    }

    @Override public void onDestroyView() {
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        if (!contacts) {
            if (state.isLoading()) loadRequested = false;
            state.cancelLoad();
        }
        renderList = null;
        super.onDestroyView();
    }
}
