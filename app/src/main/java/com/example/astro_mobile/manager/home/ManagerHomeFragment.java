package com.example.astro_mobile.manager.home;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.DrawableRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.shared.home.HomeAiBubble;

public final class ManagerHomeFragment extends Fragment {
    private static final long MOCK_LOADING_MS = 1000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable revealHome = this::showHome;
    private View content;
    private View skeleton;
    private View assistant;
    private HomeAiBubble aiBubble;
    private ObjectAnimator skeletonPulse;
    private boolean loaded;

    public ManagerHomeFragment() { super(R.layout.fragment_manager_home); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        content = view.findViewById(R.id.scroll_manager_home);
        skeleton = view.findViewById(R.id.container_manager_home_skeleton);
        assistant = view.findViewById(R.id.button_manager_home_ai);
        // O XML mostra a Home no editor; em execução o skeleton simula a espera pelos dados.
        content.setVisibility(loaded ? View.VISIBLE : View.INVISIBLE);
        skeleton.setVisibility(loaded ? View.GONE : View.VISIBLE);
        assistant.setVisibility(View.GONE);
        aiBubble = new HomeAiBubble(assistant,
                view.findViewById(R.id.text_manager_home_ai_bubble),
                view.findViewById(R.id.container_manager_home),
                view.findViewById(R.id.header_manager_home));
        bindMockPreviews(view);
        NavController navigation = NavHostFragment.findNavController(this);
        view.findViewById(R.id.button_manager_home_conversations).setOnClickListener(clicked ->
                navigation.navigate(R.id.action_global_conversations, AuthArgs.copy(getArguments())));
        assistant.setOnClickListener(clicked ->
                navigation.navigate(R.id.action_manager_home_to_chat, AuthArgs.copy(getArguments())));
        // Os demais atalhos têm feedback de toque, mas seus destinos ainda não existem.
        int[] pendingActions = {R.id.button_manager_home_notifications, R.id.button_manager_home_validate,
                R.id.button_manager_home_employees, R.id.button_manager_home_forms_action,
                R.id.button_manager_home_validations, R.id.button_manager_home_validations_more,
                R.id.button_manager_home_forms, R.id.button_manager_home_forms_more};
        for (int id : pendingActions) view.findViewById(id).setOnClickListener(clicked -> showPending());
    }

    private void bindMockPreviews(View view) {
        // São prévias curtas do Figma, não uma lista de dados recebidos da API.
        bindRow(view.findViewById(R.id.row_manager_validations_1), R.string.manager_home_mock_kevin,
                R.string.manager_home_mock_training, R.string.manager_home_mock_new, R.drawable.home_new);
        bindRow(view.findViewById(R.id.row_manager_validations_2), R.string.manager_home_mock_kevin,
                R.string.manager_home_mock_training, R.string.manager_home_mock_new, R.drawable.home_new);
        bindRow(view.findViewById(R.id.row_manager_validations_3), R.string.manager_home_mock_camilly,
                R.string.manager_home_mock_training, R.string.manager_home_mock_seen, 0);
        bindRow(view.findViewById(R.id.row_manager_forms_1), R.string.manager_home_mock_form,
                0, R.string.manager_home_mock_deadline, R.drawable.home_warning);
        bindRow(view.findViewById(R.id.row_manager_forms_2), R.string.manager_home_mock_form,
                0, R.string.manager_home_mock_deadline, R.drawable.home_new);
        bindRow(view.findViewById(R.id.row_manager_forms_3), R.string.manager_home_mock_form,
                0, R.string.manager_home_mock_deadline, 0);
    }

    private void bindRow(View row, @StringRes int title, @StringRes int subtitle,
                         @StringRes int meta, @DrawableRes int icon) {
        ((TextView) row.findViewById(R.id.text_home_feed_title)).setText(title);
        TextView detail = row.findViewById(R.id.text_home_feed_subtitle);
        detail.setVisibility(subtitle == 0 ? View.GONE : View.VISIBLE);
        if (subtitle != 0) detail.setText(subtitle);
        ((TextView) row.findViewById(R.id.text_home_feed_meta)).setText(meta);
        ImageView status = row.findViewById(R.id.image_home_feed_status);
        // Sem ícone, mantém a coluna reservada para alinhar Visto e os prazos.
        status.setVisibility(icon == 0 ? View.INVISIBLE : View.VISIBLE);
        if (icon != 0) status.setImageResource(icon);
        row.findViewById(R.id.button_home_feed_row).setOnClickListener(clicked -> showPending());
    }

    private void showPending() {
        Toast.makeText(requireContext(), R.string.employee_home_mock_unavailable, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (loaded) return;
        // Simula um segundo sem bloquear a interface; a integração real substituirá esse atraso.
        skeletonPulse = ObjectAnimator.ofFloat(skeleton, View.ALPHA, 0.65f, 1f);
        skeletonPulse.setDuration(700);
        skeletonPulse.setRepeatCount(ValueAnimator.INFINITE);
        skeletonPulse.setRepeatMode(ValueAnimator.REVERSE);
        skeletonPulse.start();
        handler.postDelayed(revealHome, MOCK_LOADING_MS);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reabre o texto do assistente ao retornar à Home já carregada.
        if (loaded) aiBubble.showTemporarily();
    }

    @Override
    public void onPause() {
        aiBubble.hide();
        super.onPause();
    }

    private void showHome() {
        loaded = true;
        skeletonPulse.cancel();
        skeletonPulse = null;
        skeleton.setAlpha(1f);
        content.setAlpha(0f);
        content.setVisibility(View.VISIBLE);
        if (isResumed()) aiBubble.showTemporarily();
        // O dissolve curto evita trocar o placeholder pelo conteúdo de forma brusca.
        content.animate().alpha(1f).setDuration(250).start();
        skeleton.animate().alpha(0f).setDuration(250).withEndAction(() -> skeleton.setVisibility(View.GONE)).start();
    }

    @Override
    public void onStop() {
        // Interrompe callbacks e animações quando a Home deixa de estar visível.
        handler.removeCallbacks(revealHome);
        if (skeletonPulse != null) {
            skeletonPulse.cancel();
            skeletonPulse = null;
        }
        content.animate().cancel();
        skeleton.animate().cancel();
        content.setAlpha(1f);
        skeleton.setAlpha(1f);
        skeleton.setVisibility(loaded ? View.GONE : View.VISIBLE);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        content = null;
        skeleton = null;
        aiBubble.hide();
        aiBubble = null;
        assistant = null;
        super.onDestroyView();
    }
}
