package com.example.astro_mobile.manager.staff;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.manager.staff.mock.StaffSamples;

public final class StaffDetailsFragment extends Fragment {
    public static final String ARG_SAMPLE_INDEX = "staff_sample_index";
    private StaffDetailsViewModel state;
    private Runnable finishLoading;

    public StaffDetailsFragment() { super(R.layout.fragment_staff_details); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this).get(StaffDetailsViewModel.class);

        // O índice identifica somente o exemplo local escolhido na lista, não um usuário real.
        int index = requireArguments().getInt(ARG_SAMPLE_INDEX);
        StaffMember member = StaffSamples.members().get(index);
        int[] avatars = {R.drawable.participant_avatar_1, R.drawable.participant_avatar_2,
                R.drawable.participant_avatar_3};
        ((ImageView) view.findViewById(R.id.image_staff_details_avatar)).setImageResource(avatars[member.avatarIndex]);
        ((TextView) view.findViewById(R.id.text_staff_details_name)).setText(member.name);
        ((TextView) view.findViewById(R.id.text_staff_details_role)).setText(member.role);
        ((TextView) view.findViewById(R.id.text_staff_details_pending)).setText(String.valueOf(member.pendingCount));
        ((TextView) view.findViewById(R.id.text_staff_details_nrs_title)).setText(
                Html.fromHtml(getString(R.string.staff_detail_nrs, member.name), Html.FROM_HTML_MODE_LEGACY));

        // Voltar retorna à lista; o X fecha o fluxo interno e retorna à Home do Gestor.
        view.findViewById(R.id.button_staff_details_back).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_staff_details_close).setOnClickListener(clicked ->
                NavHostFragment.findNavController(this).popBackStack(R.id.managerHomeFragment, false));

        // Conversa humana e eventos do funcionário pertencem a outras tarefas.
        view.findViewById(R.id.button_staff_details_chat).setOnClickListener(clicked ->
                Toast.makeText(requireContext(), R.string.staff_detail_chat_pending, Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.button_staff_details_pending).setOnClickListener(clicked ->
                Toast.makeText(requireContext(), R.string.staff_detail_events_pending, Toast.LENGTH_SHORT).show());
        showNrs(view.findViewById(R.id.list_staff_details_nrs));

        // Simula a futura consulta sem bloquear a interface nem esconder a prévia XML.
        if (!state.isLoaded()) {
            View content = view.findViewById(R.id.scroll_staff_details);
            View skeleton = view.findViewById(R.id.skeleton_staff_details);
            content.setVisibility(View.INVISIBLE);
            skeleton.setVisibility(View.VISIBLE);
            finishLoading = () -> {
                state.markLoaded();
                skeleton.setVisibility(View.GONE);
                content.setVisibility(View.VISIBLE);
                finishLoading = null;
            };
            view.postDelayed(finishLoading, 1_000L);
        }
    }

    private void showNrs(LinearLayout container) {
        // O Figma repete três cartões NR-06; o mock mantém essa composição.
        container.removeAllViews();
        for (int index = 0; index < 3; index++) {
            View item = getLayoutInflater().inflate(R.layout.item_profile_nr, container, false);
            ((TextView) item.findViewById(R.id.text_profile_nr_title)).setText(
                    Html.fromHtml(getString(R.string.staff_detail_nr_mock), Html.FROM_HTML_MODE_LEGACY));
            // Não inventa os dados normativos ausentes no frame; o conteúdo está sinalizado como pendente.
            ((TextView) item.findViewById(R.id.text_profile_nr_description)).setText(R.string.staff_detail_nr_content_pending);
            item.findViewById(R.id.text_profile_nr_objective).setVisibility(View.GONE);
            item.findViewById(R.id.text_profile_nr_applicability).setVisibility(View.GONE);
            int nrIndex = index;
            renderNr(item, nrIndex);
            item.findViewById(R.id.button_profile_nr).setOnClickListener(clicked -> {
                state.toggleNr(nrIndex);
                renderNr(item, nrIndex);
            });
            container.addView(item);
        }
    }

    private void renderNr(View item, int index) {
        boolean expanded = state.isExpanded(index);
        item.findViewById(R.id.container_profile_nr_details).setVisibility(expanded ? View.VISIBLE : View.GONE);
        ImageView chevron = item.findViewById(R.id.image_profile_nr_chevron);
        chevron.setImageResource(R.drawable.manager_events_chevron_down);
        chevron.setRotation(expanded ? 180f : 0f);
    }

    @Override
    public void onDestroyView() {
        // Evita concluir a espera depois de fechar a tela.
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        super.onDestroyView();
    }
}
