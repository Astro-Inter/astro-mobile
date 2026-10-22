package com.example.astro_mobile.manager.home;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.ManagerHomeData;
import com.example.astro_mobile.shared.home.HomeAiBubble;

public final class ManagerHomeFragment extends Fragment {
    private ManagerHomeViewModel model;
    private View content;
    private View skeleton;
    private HomeAiBubble aiBubble;
    private ObjectAnimator skeletonPulse;
    private boolean loaded;

    public ManagerHomeFragment() { super(R.layout.fragment_manager_home); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        model = new ViewModelProvider(this, ManagerHomeViewModel.Factory.createDefault())
                .get(ManagerHomeViewModel.class);
        content = view.findViewById(R.id.scroll_manager_home);
        skeleton = view.findViewById(R.id.container_manager_home_skeleton);
        View assistant = view.findViewById(R.id.button_manager_home_ai);
        content.setVisibility(View.INVISIBLE);
        skeleton.setVisibility(View.VISIBLE);
        assistant.setVisibility(View.GONE);
        aiBubble = new HomeAiBubble(assistant,
                view.findViewById(R.id.text_manager_home_ai_bubble),
                view.findViewById(R.id.container_manager_home),
                view.findViewById(R.id.header_manager_home));
        NavController navigation = NavHostFragment.findNavController(this);
        view.findViewById(R.id.button_manager_home_conversations).setOnClickListener(clicked ->
                navigation.navigate(R.id.action_global_conversations, AuthArgs.copy(getArguments())));
        view.findViewById(R.id.button_manager_home_notifications).setOnClickListener(clicked ->
                navigation.navigate(R.id.action_global_notifications));
        assistant.setOnClickListener(clicked ->
                navigation.navigate(R.id.action_manager_home_to_chat, AuthArgs.copy(getArguments())));
        view.findViewById(R.id.button_manager_home_employees).setOnClickListener(clicked ->
                navigation.navigate(R.id.action_manager_home_to_staff, AuthArgs.copy(getArguments())));
        View.OnClickListener openForms = clicked -> openForms();
        view.findViewById(R.id.button_manager_home_forms_action).setOnClickListener(openForms);
        view.findViewById(R.id.button_manager_home_forms).setOnClickListener(openForms);
        view.findViewById(R.id.button_manager_home_forms_more).setOnClickListener(openForms);
        // A validação ainda usa exemplos locais e não recebe o ID real desta prévia.
        int[] pendingActions = {R.id.button_manager_home_validate,
                R.id.button_manager_home_validations, R.id.button_manager_home_validations_more};
        for (int id : pendingActions) view.findViewById(id).setOnClickListener(clicked -> showPending());
    }

    private void bindHome(View view, ManagerHomeData home) {
        String greeting = getString(R.string.manager_home_greeting, home.getUsername());
        SpannableString styled = new SpannableString(greeting);
        styled.setSpan(new StyleSpan(Typeface.BOLD), greeting.length() - home.getUsername().length(),
                greeting.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) view.findViewById(R.id.text_manager_home_greeting)).setText(styled);
        setText(view, R.id.text_manager_home_workspace, home.getWorkspaceName());
        ManagerHomeData.Indicators indicators = home.getIndicators();
        setText(view, R.id.text_manager_home_event_count, String.valueOf(indicators.getCurrentEvents()));
        setText(view, R.id.text_manager_home_validation_count, String.valueOf(indicators.getPendingValidations()));
        setText(view, R.id.text_manager_home_employee_count,
                String.valueOf(indicators.getCollaboratorsWithPendingEvents()));

        LinearLayout validations = view.findViewById(R.id.list_manager_home_validations);
        validations.removeAllViews();
        for (ManagerHomeData.PendingValidation validation : home.getPendingValidations()) {
            if (validation == null) continue;
            View row = addRow(validations, validation.getCollaboratorName(), validation.getEventTitle(),
                    getString(validation.isViewed() ? R.string.manager_home_seen : R.string.manager_home_new),
                    validation.isViewed() ? 0 : R.drawable.home_new);
            row.setOnClickListener(clicked -> showPending());
        }
        showFeed(view, validations.getChildCount() == 0, R.id.list_manager_home_validations,
                R.id.button_manager_home_validations_more, R.id.text_manager_home_validations_empty);

        LinearLayout forms = view.findViewById(R.id.list_manager_home_forms);
        forms.removeAllViews();
        for (ManagerHomeData.PendingForm form : home.getPendingForms()) {
            if (form == null) continue;
            Long days = form.getDaysLeft();
            int icon = days != null && days <= 2 ? R.drawable.home_warning
                    : form.isViewed() ? 0 : R.drawable.home_new;
            View row = addRow(forms, form.getTitle(), null, formatDeadline(days), icon);
            row.setOnClickListener(clicked -> openForms());
        }
        showFeed(view, forms.getChildCount() == 0, R.id.list_manager_home_forms,
                R.id.button_manager_home_forms_more, R.id.text_manager_home_forms_empty);
    }

    private String formatDeadline(Long days) {
        if (days == null) return getString(R.string.manager_home_no_deadline);
        if (days < 0) return getString(R.string.manager_home_overdue);
        if (days == 0) return getString(R.string.manager_home_due_today);
        return getResources().getQuantityString(R.plurals.manager_home_days_left, days == 1 ? 1 : 2, days);
    }

    private void setText(View view, int id, String text) {
        ((TextView) view.findViewById(id)).setText(text);
    }

    private View addRow(LinearLayout container, String title, String subtitle, String meta, int icon) {
        View item = LayoutInflater.from(requireContext()).inflate(R.layout.item_manager_home_feed_row, container, false);
        setText(item, R.id.text_home_feed_title, title);
        setText(item, R.id.text_home_feed_meta, meta);
        setText(item, R.id.text_home_feed_subtitle, subtitle);
        item.findViewById(R.id.text_home_feed_subtitle).setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
        ImageView status = item.findViewById(R.id.image_home_feed_status);
        status.setVisibility(icon == 0 ? View.INVISIBLE : View.VISIBLE);
        if (icon != 0) status.setImageResource(icon);
        container.addView(item);
        return item.findViewById(R.id.button_home_feed_row);
    }

    private void showFeed(View view, boolean empty, int list, int more, int placeholder) {
        view.findViewById(list).setVisibility(empty ? View.GONE : View.VISIBLE);
        view.findViewById(more).setVisibility(empty ? View.GONE : View.VISIBLE);
        view.findViewById(placeholder).setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void openForms() {
        NavHostFragment.findNavController(this).navigate(R.id.action_manager_home_to_forms,
                AuthArgs.copy(getArguments()));
    }

    private void showPending() {
        Toast.makeText(requireContext(), R.string.employee_home_mock_unavailable, Toast.LENGTH_SHORT).show();
    }

    private void startSkeletonPulse() {
        if (skeletonPulse != null || !ValueAnimator.areAnimatorsEnabled()) return;
        skeletonPulse = ObjectAnimator.ofFloat(skeleton, View.ALPHA, 0.65f, 1f);
        skeletonPulse.setDuration(700);
        skeletonPulse.setRepeatCount(ValueAnimator.INFINITE);
        skeletonPulse.setRepeatMode(ValueAnimator.REVERSE);
        skeletonPulse.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHome();
    }

    private void loadHome() {
        View view = getView();
        if (view == null || model.isLoading()) return;
        loaded = false;
        content.animate().cancel();
        skeleton.animate().cancel();
        content.setVisibility(View.INVISIBLE);
        skeleton.setAlpha(1f);
        skeleton.setVisibility(View.VISIBLE);
        aiBubble.hide();
        startSkeletonPulse();
        model.load(new ManagerHomeViewModel.ResultCallback() {
            @Override
            public void onSuccess(ManagerHomeData home) {
                if (getView() != view || !isResumed()) return;
                bindHome(view, home);
                showHome();
            }

            @Override
            public void onFailure(FailureKind failure) {
                if (getView() != view || !isResumed()) return;
                stopSkeletonPulse();
                skeleton.setVisibility(View.GONE);
                if (failure == FailureKind.SESSION) {
                    SessionNavigation.signOut(ManagerHomeFragment.this);
                    return;
                }
                androidx.navigation.fragment.NavHostFragment.findNavController(ManagerHomeFragment.this)
                        .navigate(R.id.genericErrorFragment);
            }
        });
    }

    @Override
    public void onPause() {
        model.cancel();
        stopSkeletonPulse();
        content.animate().cancel();
        skeleton.animate().cancel();
        content.setAlpha(1f);
        skeleton.setVisibility(loaded ? View.GONE : View.VISIBLE);
        aiBubble.hide();
        super.onPause();
    }

    private void showHome() {
        loaded = true;
        stopSkeletonPulse();
        content.setAlpha(0f);
        content.setVisibility(View.VISIBLE);
        if (!ValueAnimator.areAnimatorsEnabled()) {
            content.setAlpha(1f);
            skeleton.setVisibility(View.GONE);
            aiBubble.showTemporarily();
            return;
        }
        // O dissolve curto evita trocar o placeholder pelo conteúdo de forma brusca.
        content.animate().alpha(1f).setDuration(250).start();
        View currentSkeleton = skeleton;
        currentSkeleton.animate().alpha(0f).setDuration(250).withEndAction(() -> {
            if (skeleton != currentSkeleton) return;
            currentSkeleton.setVisibility(View.GONE);
            if (isResumed()) aiBubble.showTemporarily();
        }).start();
    }

    private void stopSkeletonPulse() {
        if (skeletonPulse != null) {
            skeletonPulse.cancel();
            skeletonPulse = null;
        }
        if (skeleton != null) skeleton.setAlpha(1f);
    }

    @Override
    public void onDestroyView() {
        model.cancel();
        stopSkeletonPulse();
        content.animate().cancel();
        skeleton.animate().cancel();
        content = null;
        skeleton = null;
        aiBubble.hide();
        aiBubble = null;
        loaded = false;
        super.onDestroyView();
    }
}
