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

import com.example.astro_mobile.R;
import com.bumptech.glide.Glide;
import com.example.astro_mobile.data.local.FlowPreferences;

public final class HumanChatFragment extends Fragment {
    public static final String PEER_NAME = "human_peer_name";
    public static final String PEER_ROLE = "human_peer_role";
    public static final String PEER_AVATAR_INDEX = "human_peer_avatar_index";
    public static final String PEER_IS_MANAGER = "human_peer_is_manager";
    public static final String CONVERSATION_ID = "human_conversation_id";
    private HumanConversationsViewModel state;
    private HumanConversation conversation;
    private Runnable finishLoading;

    public HumanChatFragment() { super(R.layout.fragment_human_chat); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean manager = FlowPreferences.wasManagerFlow(requireContext());
        state = new ViewModelProvider(requireActivity()).get(HumanConversationsViewModel.class);

        // Pesquisa e detalhe do funcionário recuperam o mesmo histórico durante a sessão.
        Bundle args = requireArguments();
        String id = args.getString(CONVERSATION_ID);
        conversation = id == null ? state.open(args.getString(PEER_NAME), args.getString(PEER_ROLE),
                args.getBoolean(PEER_IS_MANAGER, !manager), args.getInt(PEER_AVATAR_INDEX)) : state.find(id);
        if (conversation == null) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }
        ((TextView) view.findViewById(R.id.text_human_chat_name)).setText(conversation.name);
        ((TextView) view.findViewById(R.id.text_human_chat_role)).setText(conversation.role);
        int[] avatars = {R.drawable.participant_avatar_1, R.drawable.participant_avatar_2, R.drawable.participant_avatar_3};
        int avatar = conversation.manager ? R.drawable.human_chat_manager_avatar : avatars[conversation.avatarIndex];
        ImageView peer = view.findViewById(R.id.image_human_chat_peer);
        peer.setImageResource(avatar);
        if (conversation.conversationId != null) {
            Glide.with(this).load(conversation.profilePhotoUrl).circleCrop()
                    .placeholder(R.drawable.profile_avatar_placeholder)
                    .fallback(R.drawable.profile_avatar_placeholder)
                    .error(R.drawable.profile_avatar_placeholder).into(peer);
        }
        ViewGroup.LayoutParams peerSize = peer.getLayoutParams();
        peerSize.width = peerSize.height = Math.round((manager ? 48 : 50) * getResources().getDisplayMetrics().density);
        peer.setLayoutParams(peerSize);

        view.findViewById(R.id.button_human_chat_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_human_chat_add).setOnClickListener(clicked ->
                Toast.makeText(requireContext(), R.string.human_chat_attachment_pending, Toast.LENGTH_SHORT).show());

        RecyclerView messages = view.findViewById(R.id.list_human_chat_messages);
        HumanMessagesAdapter adapter = new HumanMessagesAdapter(conversation.messages, avatar, manager);
        messages.setAdapter(adapter);
        EditText input = view.findViewById(R.id.input_human_chat_message);
        input.setText(conversation.draft);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { conversation.draft = s.toString(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        view.findViewById(R.id.button_human_chat_send).setOnClickListener(clicked -> send(input, messages, adapter));
        input.setOnEditorActionListener((text, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEND) return false;
            send(input, messages, adapter);
            return true;
        });

        // Mostra um segundo de skeleton no protótipo; a prévia XML permanece visível.
        if (!conversation.loaded) {
            View skeleton = view.findViewById(R.id.skeleton_human_chat);
            messages.setVisibility(View.INVISIBLE);
            skeleton.setVisibility(View.VISIBLE);
            view.findViewById(R.id.container_human_chat_composer).setVisibility(View.INVISIBLE);
            finishLoading = () -> {
                conversation.loaded = true;
                skeleton.setVisibility(View.GONE);
                messages.setVisibility(View.VISIBLE);
                view.findViewById(R.id.container_human_chat_composer).setVisibility(View.VISIBLE);
                finishLoading = null;
            };
            view.postDelayed(finishLoading, 1_000L);
        }
    }

    private void send(EditText input, RecyclerView messages, HumanMessagesAdapter adapter) {
        // A lista real não simula envio nem transforma sua prévia em histórico completo.
        if (conversation.conversationId != null) {
            Toast.makeText(requireContext(), R.string.human_chat_integration_pending, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!state.send(conversation)) return;
        int last = conversation.messages.size() - 1;
        adapter.notifyItemInserted(last);
        input.setText("");
        messages.scrollToPosition(last);
    }

    @Override
    public void onDestroyView() {
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
