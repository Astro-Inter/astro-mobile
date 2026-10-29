package com.example.astro_mobile.manager.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.chat.human.HumanChatFragment;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.CollaboratorProfileData;
import com.example.astro_mobile.data.api.dto.UserProfileData;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class StaffDetailsFragment extends Fragment {
    public static final String ARG_COLLABORATOR_ID = "staff_collaborator_id";
    public static final String ARG_NAME = "staff_name";
    public static final String ARG_ROLE = "staff_role";
    public static final String ARG_EMAIL = "staff_email";
    public static final String ARG_UNIT = "staff_unit";
    public static final String ARG_PHOTO_URL = "staff_photo_url";
    private StaffDetailsViewModel state;

    public StaffDetailsFragment() { super(R.layout.fragment_staff_details); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this).get(StaffDetailsViewModel.class);
        view.findViewById(R.id.button_staff_details_back).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.button_staff_details_close).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack(R.id.managerHomeFragment, false));
        if (state.getProfile() == null) load(view);
        else showProfile(view, state.getProfile());
    }

    @Override public void onResume() {
        super.onResume();
        Boolean retry = NavHostFragment.findNavController(this).getCurrentBackStackEntry()
                .getSavedStateHandle().remove("retry_staff_details");
        if (Boolean.TRUE.equals(retry) && getView() != null) load(getView());
    }

    private void load(View view) {
        view.findViewById(R.id.scroll_staff_details).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.skeleton_staff_details).setVisibility(View.VISIBLE);
        long id = requireArguments().getLong(ARG_COLLABORATOR_ID);
        state.load(id, new StaffDetailsViewModel.Callback() {
            @Override public void onSuccess() {
                if (getView() == view && isAdded()) showProfile(view, state.getProfile());
            }
            @Override public void onFailure(FailureKind failure) {
                if (getView() != view || !isAdded()) return;
                if (failure == FailureKind.SESSION) SessionNavigation.signOut(StaffDetailsFragment.this);
                else NavHostFragment.findNavController(StaffDetailsFragment.this).navigate(
                        failure == FailureKind.CONNECTION ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
            }
        });
    }

    private void showProfile(View view, CollaboratorProfileData data) {
        // Preenche os campos com os dados retornados para o colaborador selecionado.
        setText(view, R.id.text_staff_details_name, data.name);
        setText(view, R.id.text_staff_details_role, data.roleName);
        setText(view, R.id.text_staff_details_unit, data.unitName);
        setText(view, R.id.text_staff_details_email, data.email);
        setText(view, R.id.text_staff_details_modality, formatWorkModel(data.workModel));
        setText(view, R.id.text_staff_details_pending, data.pendingCount == null ? null : data.pendingCount.toString());
        ((TextView) view.findViewById(R.id.text_staff_details_nrs_title)).setText(
                android.text.Html.fromHtml(getString(R.string.staff_detail_nrs,
                        android.text.TextUtils.htmlEncode(data.name == null ? "" : data.name)), android.text.Html.FROM_HTML_MODE_LEGACY));
        Glide.with(this).load(data.profilePhotoUrl).circleCrop()
                .placeholder(R.drawable.profile_avatar_placeholder).error(R.drawable.profile_avatar_placeholder)
                .fallback(R.drawable.profile_avatar_placeholder).into((ImageView) view.findViewById(R.id.image_staff_details_avatar));
        showNrs(view.findViewById(R.id.list_staff_details_nrs), data);
        view.findViewById(R.id.button_staff_details_chat).setOnClickListener(v -> {
            Bundle args = AuthArgs.copy(getArguments());
            args.putString(HumanChatFragment.PEER_NAME, data.name);
            args.putString(HumanChatFragment.PEER_ROLE, data.roleName);
            NavHostFragment.findNavController(this).navigate(R.id.action_staff_details_to_human_chat, args);
        });
        view.findViewById(R.id.button_staff_details_pending).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_staff_details_to_events, AuthArgs.copy(getArguments())));
        view.findViewById(R.id.skeleton_staff_details).setVisibility(View.GONE);
        view.findViewById(R.id.scroll_staff_details).setVisibility(View.VISIBLE);
    }

    private void showNrs(LinearLayout container, CollaboratorProfileData data) {
        // Cada NR expande seus próprios dados e acompanha a rolagem da tela.
        container.removeAllViews();
        if (data.nrs == null) return;
        for (UserProfileData.Nr nr : data.nrs) {
            if (nr == null) continue;
            View item = getLayoutInflater().inflate(R.layout.item_profile_nr, container, false);
            String title = getString(R.string.profile_nr_code, nr.getCode());
            if (nr.getValidade() != null && !nr.getValidade().isEmpty()) {
                String date = nr.getValidade();
                try { date = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
                catch (DateTimeParseException ignored) { }
                title = getString(R.string.profile_nr_validity, title, date);
            }
            ((TextView) item.findViewById(R.id.text_profile_nr_title)).setText(title);
            ((TextView) item.findViewById(R.id.text_profile_nr_description)).setText(getString(R.string.profile_nr_description, text(nr.getDescricao())));
            ((TextView) item.findViewById(R.id.text_profile_nr_objective)).setText(getString(R.string.profile_nr_objective, text(nr.getObjetivo())));
            ((TextView) item.findViewById(R.id.text_profile_nr_applicability)).setText(getString(R.string.profile_nr_applicability, text(nr.getAplicabilidade())));
            View details = item.findViewById(R.id.container_profile_nr_details);
            ImageView chevron = item.findViewById(R.id.image_profile_nr_chevron);
            item.findViewById(R.id.button_profile_nr).setOnClickListener(v -> {
                boolean open = details.getVisibility() != View.VISIBLE;
                details.setVisibility(open ? View.VISIBLE : View.GONE);
                chevron.setImageResource(open ? R.drawable.profile_chevron_up : R.drawable.profile_chevron_down);
            });
            container.addView(item);
        }
    }

    @Override public void onDestroyView() {
        state.cancel();
        super.onDestroyView();
    }

    private void setText(View view, int id, String value) {
        ((TextView) view.findViewById(id)).setText(value == null || value.trim().isEmpty()
                ? getString(R.string.staff_detail_value_unavailable) : value);
    }
    private String text(String value) { return value == null ? "" : value; }
    private String formatWorkModel(String value) {
        if (value == null) return null;
        switch (value) {
            case "PRESENCIAL": return getString(R.string.staff_detail_work_presencial);
            case "HIBRIDO": return getString(R.string.staff_detail_work_hybrid);
            case "REMOTO": return getString(R.string.staff_detail_work_remote);
            default: return value;
        }
    }
}
