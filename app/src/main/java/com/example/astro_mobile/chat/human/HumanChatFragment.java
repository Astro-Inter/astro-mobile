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
import com.example.astro_mobile.data.local.FlowPreferences;

public final class HumanChatFragment extends Fragment {
    public static final String PEER_NAME = "human_peer_name";
    public static final String PEER_ROLE = "human_peer_role";
    public static final String PEER_AVATAR_INDEX = "human_peer_avatar_index";
    private HumanChatViewModel state;
    private Runnable finishLoading;

    public HumanChatFragment() { super(R.layout.fragment_human_chat); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean manager = FlowPreferences.wasManagerFlow(requireContext());
        state = new ViewModelProvider(this).get(HumanChatViewModel.class);
        state.initialize(manager);

        // O fluxo ativo escolhe a variante visual; argumentos levam o contato mock selecionado.
        Bundle args = requireArguments();
        ((TextView) view.findViewById(R.id.text_human_chat_name)).setText(args.getString(PEER_NAME,
                getString(manager ? R.string.human_chat_employee_name : R.string.human_chat_manager_name)));
        ((TextView) view.findViewById(R.id.text_human_chat_role)).setText(args.getString(PEER_ROLE,
                getString(manager ? R.string.human_chat_employee_role : R.string.human_chat_manager_role)));
        int[] avatars = {R.drawable.participant_avatar_1, R.drawable.participant_avatar_2, R.drawable.participant_avatar_3};
        int avatar = manager ? avatars[args.getInt(PEER_AVATAR_INDEX)] : R.drawable.human_chat_manager_avatar;
        ImageView peer = view.findViewById(R.id.image_human_chat_peer);
        peer.setImageResource(avatar);
        ViewGroup.LayoutParams peerSize = peer.getLayoutParams();
        peerSize.width = peerSize.height = Math.round((manager ? 48 : 50) * getResources().getDisplayMetrics().density);
        peer.setLayoutParams(peerSize);

        view.findViewById(R.id.button_human_chat_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_human_chat_add).setOnClickListener(clicked ->
                Toast.makeText(requireContext(), R.string.human_chat_attachment_pending, Toast.LENGTH_SHORT).show());

        RecyclerView messages = view.findViewById(R.id.list_human_chat_messages);
        HumanMessagesAdapter adapter = new HumanMessagesAdapter(state.getMessages(), avatar, manager);
        messages.setAdapter(adapter);
        EditText input = view.findViewById(R.id.input_human_chat_message);
        input.setText(state.getDraft());
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { state.setDraft(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });
        view.findViewById(R.id.button_human_chat_send).setOnClickListener(clicked -> send(input, messages, adapter));
        input.setOnEditorActionListener((text, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEND) return false;
            send(input, messages, adapter);
            return true;
        });

        // Mostra um segundo de skeleton no protótipo; a prévia XML permanece visível.
        if (!state.isLoaded()) {
            View skeleton = view.findViewById(R.id.skeleton_human_chat);
            messages.setVisibility(View.INVISIBLE);
            skeleton.setVisibility(View.VISIBLE);
            view.findViewById(R.id.container_human_chat_composer).setVisibility(View.INVISIBLE);
            finishLoading = () -> {
                state.markLoaded();
                skeleton.setVisibility(View.GONE);
                messages.setVisibility(View.VISIBLE);
                view.findViewById(R.id.container_human_chat_composer).setVisibility(View.VISIBLE);
                finishLoading = null;
            };
            view.postDelayed(finishLoading, 1_000L);
        }
    }

    private void send(EditText input, RecyclerView messages, HumanMessagesAdapter adapter) {
        // A mensagem aparece somente nesta demonstração, sem chamar o chatbot ou enviar à rede.
        if (!state.send()) return;
        int last = state.getMessages().size() - 1;
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
