package com.example.astro_mobile.manager.events.validation;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

public final class EventValidationFragment extends Fragment {
    private Dialog rejectDialog;
    private Runnable finishLoading;

    public EventValidationFragment() {
        super(R.layout.fragment_event_validation);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();

        // Mostra o participante e o evento que foram selecionados na lista.
        int[] avatars = {R.drawable.participant_avatar_1,
                R.drawable.participant_avatar_2, R.drawable.participant_avatar_3};
        ((ImageView) view.findViewById(R.id.image_validation_participant))
                .setImageResource(avatars[args.getInt("participant_avatar_index")]);
        ((TextView) view.findViewById(R.id.text_validation_participant))
                .setText(args.getString("participant_name"));
        ((TextView) view.findViewById(R.id.text_validation_event))
                .setText(args.getString("event_title"));

        // Voltar preserva a lista; tocar no evento volta ao detalhe e o X abre a Home.
        view.findViewById(R.id.button_validation_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_validation_event).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(R.id.managerEventDetailsFragment, false));
        view.findViewById(R.id.button_validation_close).setOnClickListener(clicked -> returnHome());

        // O comprovante é apenas um exemplo. Sua visualização pertence à SCRUM-287.
        TextView filename = view.findViewById(R.id.text_validation_evidence);
        filename.setPaintFlags(filename.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        view.findViewById(R.id.button_validation_evidence).setOnClickListener(clicked ->
                Toast.makeText(requireContext(), R.string.event_validation_evidence_pending, Toast.LENGTH_SHORT).show());

        // Mostra a confirmação local; não altera conclusões nem envia dados à API.
        view.findViewById(R.id.button_validation_confirm).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).navigate(
                        R.id.action_event_validation_to_confirmation, AuthArgs.copy(getArguments())));
        view.findViewById(R.id.button_validation_reject).setOnClickListener(clicked -> showRejection());

        // Simula a espera pelos dados sem bloquear a tela; o XML continua mostrando o conteúdo.
        View content = view.findViewById(R.id.container_validation_content);
        View skeleton = view.findViewById(R.id.skeleton_event_validation);
        content.setVisibility(View.INVISIBLE);
        skeleton.setVisibility(View.VISIBLE);
        finishLoading = () -> {
            skeleton.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
            finishLoading = null;
        };
        view.postDelayed(finishLoading, 1_000L);
    }

    private void showRejection() {
        // Reproduz o pop-up do Figma: justificativa editável e botão ativo quando há texto.
        rejectDialog = new Dialog(requireContext());
        rejectDialog.setContentView(R.layout.dialog_event_validation_reject);
        EditText reason = rejectDialog.findViewById(R.id.input_validation_rejection_reason);
        View submit = rejectDialog.findViewById(R.id.button_validation_rejection_submit);
        reason.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable text) {
                submit.setEnabled(!text.toString().trim().isEmpty());
            }
        });
        rejectDialog.findViewById(R.id.button_validation_rejection_close)
                .setOnClickListener(clicked -> rejectDialog.dismiss());
        submit.setOnClickListener(clicked -> {
            // No mock, apenas fecha o pop-up e retorna à Home, sem enviar ou gravar dados.
            rejectDialog.dismiss();
            returnHome();
        });
        rejectDialog.show();

        // Limita o pop-up no tablet e mantém margem e fundo escuro no celular.
        Window window = rejectDialog.getWindow();
        if (window != null) {
            float density = getResources().getDisplayMetrics().density;
            int available = getResources().getDisplayMetrics().widthPixels;
            int width = Math.min(available - Math.round(60 * density), Math.round(370 * density));
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.8f);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void returnHome() {
        NavHostFragment.findNavController(this).popBackStack(HomeNavigation.destination(requireContext()), false);
    }

    @Override
    public void onDestroyView() {
        // Cancela a espera mock ao sair para não atualizar uma tela que já foi fechada.
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        // Fecha o pop-up junto com a tela para não manter referências à interface antiga.
        if (rejectDialog != null) rejectDialog.dismiss();
        rejectDialog = null;
        super.onDestroyView();
    }
}
