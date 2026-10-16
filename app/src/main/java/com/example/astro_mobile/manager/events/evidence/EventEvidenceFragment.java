package com.example.astro_mobile.manager.events.evidence;

import android.graphics.Matrix;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;

public final class EventEvidenceFragment extends Fragment {
    private Runnable finishLoading;

    public EventEvidenceFragment() {
        super(R.layout.fragment_event_evidence);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.button_event_evidence_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());

        // Mantém o recorte do Figma em qualquer largura, sem modificar a imagem original.
        ImageView image = view.findViewById(R.id.image_event_evidence);
        image.addOnLayoutChangeListener((changed, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            float width = right - left;
            float scale = (width * 1.4695f) / image.getDrawable().getIntrinsicWidth();
            Matrix crop = new Matrix();
            crop.setScale(scale, scale);
            // No frame, a imagem começa sob o cabeçalho e ocupa 146,95% da largura.
            crop.postTranslate(-0.2452f * width, -(130f / 430f) * width);
            image.setImageMatrix(crop);
        });

        // Os três números usam a mesma imagem no mock, conforme aprovado pelo solicitante.
        int[] pages = {R.id.button_event_evidence_page_one,
                R.id.button_event_evidence_page_two, R.id.button_event_evidence_page_three};
        view.findViewById(pages[0]).setSelected(true);
        for (int page : pages) {
            view.findViewById(page).setOnClickListener(clicked -> {
                for (int candidate : pages) {
                    view.findViewById(candidate).setSelected(candidate == clicked.getId());
                }
            });
        }

        // Simula um segundo de carregamento sem bloquear a interface ou a seta de voltar.
        View content = view.findViewById(R.id.scroll_event_evidence);
        View skeleton = view.findViewById(R.id.skeleton_event_evidence);
        content.setVisibility(View.INVISIBLE);
        skeleton.setVisibility(View.VISIBLE);
        finishLoading = () -> {
            skeleton.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
            finishLoading = null;
        };
        view.postDelayed(finishLoading, 1_000L);
    }

    @Override
    public void onDestroyView() {
        // Cancela a espera ao sair para não atualizar uma interface que foi fechada.
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
