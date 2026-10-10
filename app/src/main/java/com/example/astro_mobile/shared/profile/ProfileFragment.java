package com.example.astro_mobile.shared.profile;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.bumptech.glide.Glide;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.api.AstroApiClient;
import com.example.astro_mobile.data.api.UserProfileRepository;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.UserProfileData;
import com.example.astro_mobile.data.local.NotificationPreferences;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class ProfileFragment extends Fragment {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private ProfileViewModel model;
    private Dialog logoutDialog;
    private Uri pendingPhoto;
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return;
                pendingPhoto = uri;
                if (getView() != null && model != null) uploadPhoto(uri);
            });

    public ProfileFragment() { super(R.layout.fragment_profile); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        NavController navigation = NavHostFragment.findNavController(this);
        model = new ViewModelProvider(navigation.getBackStackEntry(R.id.profileFragment),
                new ProfileViewModel.Factory()).get(ProfileViewModel.class);
        view.findViewById(R.id.button_profile_back).setOnClickListener(clicked -> navigation.navigateUp());
        view.findViewById(R.id.button_profile_change_password).setOnClickListener(clicked ->
                navigation.navigate(R.id.action_profile_to_change_password, AuthArgs.copy(getArguments())));
        view.findViewById(R.id.button_profile_logout).setOnClickListener(clicked -> showLogout());
        View.OnClickListener choosePhoto = clicked -> {
            if (model.isLoading()) return;
            photoPicker.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
        };
        view.findViewById(R.id.button_profile_change_photo).setOnClickListener(choosePhoto);
        view.findViewById(R.id.image_profile_photo).setOnClickListener(choosePhoto);
        view.findViewById(R.id.image_profile_photo).setContentDescription(getString(R.string.profile_change_photo));
        view.findViewById(R.id.image_profile_photo).setFocusable(true);

        // O papel da conta é preservado mesmo quando o Gestor usa a Home do Colaborador.
        String type = getArguments() == null ? null : getArguments().getString(AuthArgs.USER_TYPE);
        View switchFlow = view.findViewById(R.id.button_profile_switch_flow);
        switchFlow.setVisibility("GESTOR".equals(type) || "GESTOR_WORKSPACE".equals(type)
                ? View.VISIBLE : View.GONE);
        switchFlow.setOnClickListener(clicked -> {
            Bundle args = AuthArgs.copy(getArguments());
            args.putBoolean(AuthArgs.FROM_PROFILE, true);
            navigation.navigate(R.id.action_profile_to_flow_choice, args);
        });

        // A opção de notificações guarda somente uma preferência local por conta.
        SwitchCompat notifications = view.findViewById(R.id.switch_profile_notifications);
        String userId = model.getUserId();
        if (userId == null) {
            SessionNavigation.signOut(this);
            return;
        }
        notifications.setChecked(NotificationPreferences.isEnabled(requireContext(), userId));
        notifications.setOnCheckedChangeListener((button, enabled) ->
                NotificationPreferences.save(requireContext(), userId, enabled));
        boolean hasCachedProfile = model.getProfile() != null;
        if (hasCachedProfile) showProfile(view, model.getProfile(), false);
        view.findViewById(R.id.button_profile_change_photo).setEnabled(false);
        view.findViewById(R.id.image_profile_photo).setEnabled(false);
        // A URL da foto é temporária: cada retorno à tela consulta o perfil novamente.
        model.load(new UserProfileRepository.ResultCallback() {
            @Override
            public void onSuccess(UserProfileData profile) {
                if (getView() == view && isAdded()) {
                    showProfile(view, profile, !hasCachedProfile);
                    if (pendingPhoto != null) uploadPhoto(pendingPhoto);
                }
            }

            @Override
            public void onFailure(FailureKind failure) {
                if (getView() != view || !isAdded()) return;
                if (failure == FailureKind.SESSION) SessionNavigation.signOut(ProfileFragment.this);
                else navigation.navigate(R.id.action_profile_to_generic_error);
            }
        });
    }

    private void showProfile(View view, UserProfileData data, boolean animate) {
        // Todos os textos do perfil vêm da API; os exemplos existem apenas no preview XML.
        ((TextView) view.findViewById(R.id.text_profile_name)).setText(data.getNome());
        ((TextView) view.findViewById(R.id.text_profile_role)).setText(data.getCargo());
        ((TextView) view.findViewById(R.id.text_profile_unit)).setText(data.getUnidade());
        ((TextView) view.findViewById(R.id.text_profile_modality)).setText(data.getModalidade());
        ((TextView) view.findViewById(R.id.text_profile_email)).setText(data.getEmail());
        showPhoto(view.findViewById(R.id.image_profile_photo), data.getProfilePhotoUrl());
        showNrs(view.findViewById(R.id.list_profile_nrs), data.getNrs());
        view.findViewById(R.id.text_profile_nrs_empty).setVisibility(
                data.getNrs().isEmpty() ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.container_profile_skeleton).setVisibility(View.GONE);
        View content = view.findViewById(R.id.scroll_profile);
        content.setVisibility(View.VISIBLE);
        content.setAlpha(animate ? 0f : 1f);
        if (animate) content.animate().alpha(1f).setDuration(250).start();
        setPhotoBusy(view, false);
    }

    private void setPhotoBusy(View view, boolean busy) {
        View button = view.findViewById(R.id.button_profile_change_photo);
        button.setEnabled(!busy);
        button.setVisibility(busy ? View.INVISIBLE : View.VISIBLE);
        view.findViewById(R.id.image_profile_photo).setEnabled(!busy);
        view.findViewById(R.id.progress_profile_photo).setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void uploadPhoto(Uri uri) {
        View view = getView();
        if (view == null || model.isLoading()) return;
        pendingPhoto = null;
        setPhotoBusy(view, true);
        model.uploadPhoto(requireContext().getApplicationContext().getContentResolver(), uri,
                new UserProfileRepository.PhotoCallback() {
                    @Override
                    public void onSuccess() {
                        if (getView() != view || !isAdded()) return;
                        // Só apresenta a nova imagem depois de confirmar o envio e obter a URL atual.
                        model.load(new UserProfileRepository.ResultCallback() {
                            @Override
                            public void onSuccess(UserProfileData profile) {
                                if (getView() != view || !isAdded()) return;
                                showProfile(view, profile, false);
                                Toast.makeText(requireContext(), R.string.profile_photo_updated, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(FailureKind failure) {
                                if (getView() != view || !isAdded()) return;
                                setPhotoBusy(view, false);
                                if (failure == FailureKind.SESSION) SessionNavigation.signOut(ProfileFragment.this);
                                else Toast.makeText(requireContext(), R.string.profile_photo_refresh_error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(FailureKind failure) {
                        if (getView() != view || !isAdded()) return;
                        setPhotoBusy(view, false);
                        if (failure == FailureKind.SESSION) SessionNavigation.signOut(ProfileFragment.this);
                        else Toast.makeText(requireContext(), failure == FailureKind.BUSINESS
                                ? R.string.profile_photo_invalid : R.string.profile_photo_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showPhoto(ImageView photo, String url) {
        // A URL assinada já autoriza a imagem; não encaminha o token Firebase ao R2.
        Glide.with(this).asBitmap().load(url)
                .circleCrop()
                .timeout((int) (AstroApiClient.REQUEST_TIMEOUT_SECONDS * 1000))
                .placeholder(R.drawable.profile_avatar_placeholder)
                .fallback(R.drawable.profile_avatar_placeholder)
                .error(R.drawable.profile_avatar_placeholder)
                .into(photo);
    }

    private void showNrs(LinearLayout container, List<UserProfileData.Nr> nrs) {
        // A lista curta usa a própria rolagem do perfil, sem cortar a próxima NR ao expandir.
        container.removeAllViews();
        for (UserProfileData.Nr nr : nrs) {
            View item = getLayoutInflater().inflate(R.layout.item_profile_nr, container, false);
            String title = getString(R.string.profile_nr_code, nr.getCode());
            if (nr.getValidade() != null && !nr.getValidade().isEmpty()) {
                title = getString(R.string.profile_nr_validity, title, formatDate(nr.getValidade()));
            }
            ((TextView) item.findViewById(R.id.text_profile_nr_title)).setText(title);
            ((TextView) item.findViewById(R.id.text_profile_nr_description)).setText(
                    getString(R.string.profile_nr_description, text(nr.getDescricao())));
            ((TextView) item.findViewById(R.id.text_profile_nr_objective)).setText(
                    getString(R.string.profile_nr_objective, text(nr.getObjetivo())));
            ((TextView) item.findViewById(R.id.text_profile_nr_applicability)).setText(
                    getString(R.string.profile_nr_applicability, text(nr.getAplicabilidade())));
            View details = item.findViewById(R.id.container_profile_nr_details);
            ImageView chevron = item.findViewById(R.id.image_profile_nr_chevron);
            item.findViewById(R.id.button_profile_nr).setOnClickListener(clicked -> {
                // Cada cartão abre e fecha sozinho, preservando os demais.
                boolean open = details.getVisibility() != View.VISIBLE;
                details.setVisibility(open ? View.VISIBLE : View.GONE);
                chevron.setImageResource(open ? R.drawable.profile_chevron_up : R.drawable.profile_chevron_down);
            });
            container.addView(item);
        }
    }

    private String formatDate(String value) {
        // Apenas formata a data retornada; não calcula vencimento no aplicativo.
        try {
            return LocalDate.parse(value).format(DATE_FORMAT);
        } catch (DateTimeParseException error) {
            return value;
        }
    }

    private String text(String value) { return value == null ? "" : value; }

    private void showLogout() {
        // Cancelar e fechar preservam a sessão; apenas Sair confirma o logout.
        logoutDialog = new Dialog(requireContext());
        logoutDialog.setContentView(R.layout.dialog_profile_logout);
        logoutDialog.findViewById(R.id.button_profile_logout_close).setOnClickListener(v -> logoutDialog.dismiss());
        logoutDialog.findViewById(R.id.button_profile_logout_cancel).setOnClickListener(v -> logoutDialog.dismiss());
        logoutDialog.findViewById(R.id.button_profile_logout_confirm).setOnClickListener(v -> {
            logoutDialog.dismiss();
            SessionNavigation.signOut(this);
        });
        logoutDialog.show();
        if (logoutDialog.getWindow() != null) {
            logoutDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            logoutDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            logoutDialog.getWindow().setDimAmount(0.8f);
            int available = getResources().getDisplayMetrics().widthPixels;
            int width = Math.min(available - (int) (40 * getResources().getDisplayMetrics().density),
                    (int) (430 * getResources().getDisplayMetrics().density));
            logoutDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDestroyView() {
        if (model != null) model.cancel();
        View view = getView();
        if (view != null) Glide.with(this).clear((ImageView) view.findViewById(R.id.image_profile_photo));
        if (logoutDialog != null) {
            logoutDialog.dismiss();
            logoutDialog = null;
        }
        super.onDestroyView();
    }
}
