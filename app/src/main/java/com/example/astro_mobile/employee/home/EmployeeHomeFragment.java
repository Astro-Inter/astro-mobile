package com.example.astro_mobile.employee.home;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.shared.navigation.HomeNavigation;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.CollaboratorHomeData;
import com.example.astro_mobile.notifications.NotificationsFragment;
import com.example.astro_mobile.notifications.NotificationTimeFormatter;
import com.example.astro_mobile.shared.home.HomeAiBubble;
import com.example.astro_mobile.employee.events.EmployeeEvent;
import java.time.Instant;
import java.time.ZoneId;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class EmployeeHomeFragment extends Fragment {

    private static final long SKELETON_PULSE_MS = 900L;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private EmployeeHomeViewModel model;
    private boolean homeReady;
    private View aiBubble;
    private TextView aiBubbleText;
    private View homeContent;
    private View homeSkeleton;
    private HomeAiBubble assistant;
    private ValueAnimator skeletonPulseAnimator;

    public EmployeeHomeFragment() {
        super(R.layout.fragment_employee_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        model = new ViewModelProvider(this, EmployeeHomeViewModel.Factory.createDefault())
                .get(EmployeeHomeViewModel.class);
        homeContent = view.findViewById(R.id.scroll_employee_home);
        homeSkeleton = view.findViewById(R.id.container_employee_home_skeleton);

        aiBubble = view.findViewById(R.id.button_employee_home_ai_bubble);
        aiBubbleText = view.findViewById(R.id.text_employee_home_ai_bubble);
        aiBubble.setVisibility(View.GONE);

        int[] notificationActions = {
                R.id.button_employee_home_notifications,
                R.id.button_employee_home_notifications_section,
                R.id.button_employee_home_notifications_more
        };
        for (int id : notificationActions) {
            view.findViewById(id).setOnClickListener(clicked ->
                    Navigation.findNavController(clicked).navigate(R.id.action_global_notifications));
        }

        // Os atalhos de eventos abrem o calendário do colaborador.
        int[] eventsActions = { R.id.button_employee_home_events, R.id.button_employee_home_pending_card,
                R.id.button_employee_home_review_card, R.id.button_employee_home_completed_card,
                R.id.button_employee_home_events_more };
        for (int id : eventsActions) {
            view.findViewById(id).setOnClickListener(clicked -> Navigation.findNavController(clicked)
                    .navigate(R.id.action_global_employee_events, AuthArgs.copy(getArguments())));
        }

        // O mascote continua sendo um atalho direto para a IA; a Activity controla a navbar.
        aiBubble.setOnClickListener(clicked ->
                Navigation.findNavController(clicked).navigate(R.id.action_employee_home_to_chat,
                        AuthArgs.copy(getArguments())));

        // Restringe o arraste do assistente à área entre cabeçalho e navbar.
        assistant = new HomeAiBubble(aiBubble, aiBubbleText,
                view.findViewById(R.id.container_employee_home_content),
                view.findViewById(R.id.container_employee_home_header));
    }

    @Override
    public void onResume() {
        super.onResume();
        Boolean restore = androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .getCurrentBackStackEntry().getSavedStateHandle().remove(HomeNavigation.RESTORE_LOADED_HOME);
        // Voltar de um erro restaura o último sucesso sem outra requisição ou skeleton.
        if (Boolean.TRUE.equals(restore) && model.getHome() != null) {
            bindHome(requireView(), model.getHome());
            restoreLoadedHome();
        } else loadHome();
    }

    @Override
    public void onPause() {
        model.cancel();
        stopSkeletonPulse();
        finishHomeRevealIfNeeded();
        if (assistant != null) assistant.hide();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        model.cancel();
        stopSkeletonPulse();
        if (homeContent != null) {
            homeContent.animate().cancel();
        }
        if (homeSkeleton != null) {
            homeSkeleton.animate().cancel();
        }
        if (assistant != null) assistant.hide();
        assistant = null;
        aiBubble = null;
        aiBubbleText = null;
        homeContent = null;
        homeSkeleton = null;
        homeReady = false;
        super.onDestroyView();
    }

    private void restoreLoadedHome() {
        homeReady = true;
        stopSkeletonPulse();
        homeContent.setAlpha(1f);
        homeContent.setVisibility(View.VISIBLE);
        homeSkeleton.setVisibility(View.GONE);
        assistant.showTemporarily();
    }

    private void loadHome() {
        View view = getView();
        if (view == null || model.isLoading()) return;
        homeReady = false;
        homeContent.animate().cancel();
        homeSkeleton.animate().cancel();
        homeContent.setVisibility(View.INVISIBLE);
        homeSkeleton.setAlpha(1f);
        homeSkeleton.setVisibility(View.VISIBLE);
        assistant.hide();
        startSkeletonPulse();
        model.load(new EmployeeHomeViewModel.ResultCallback() {
            @Override
            public void onSuccess(CollaboratorHomeData home) {
                if (getView() != view || !isResumed()) return;
                bindHome(view, home);
                revealHome();
            }

            @Override
            public void onFailure(FailureKind failure) {
                if (getView() != view || !isResumed()) return;
                stopSkeletonPulse();
                homeSkeleton.setVisibility(View.GONE);
                if (failure == FailureKind.SESSION) {
                    SessionNavigation.signOut(EmployeeHomeFragment.this);
                    return;
                }
                androidx.navigation.fragment.NavHostFragment.findNavController(EmployeeHomeFragment.this)
                        .navigate(failure == FailureKind.CONNECTION
                                ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
            }
        });
    }

    private void revealHome() {
        // Troca o skeleton pelo conteúdo com um dissolve curto.
        if (homeContent == null || homeSkeleton == null) {
            return;
        }
        homeReady = true;
        stopSkeletonPulse();
        homeContent.setAlpha(0f);
        homeContent.setVisibility(View.VISIBLE);
        if (!ValueAnimator.areAnimatorsEnabled()) {
            homeContent.setAlpha(1f);
            homeSkeleton.setVisibility(View.GONE);
            assistant.showTemporarily();
            return;
        }

        int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        homeContent.animate()
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        View skeleton = homeSkeleton;
        skeleton.animate()
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (homeSkeleton == skeleton) {
                        skeleton.setVisibility(View.GONE);
                        if (isResumed()) {
                            assistant.showTemporarily();
                        }
                    }
                })
                .start();
    }

    private void startSkeletonPulse() {
        // Faz as áreas de preenchimento pulsarem durante a consulta da API.
        if (homeSkeleton == null || !ValueAnimator.areAnimatorsEnabled()
                || skeletonPulseAnimator != null) {
            return;
        }
        skeletonPulseAnimator = ValueAnimator.ofFloat(0.84f, 1f);
        skeletonPulseAnimator.setDuration(SKELETON_PULSE_MS);
        skeletonPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        skeletonPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        skeletonPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        skeletonPulseAnimator.addUpdateListener(animation ->
                setSkeletonPlaceholderAlpha(homeSkeleton,
                        (float) animation.getAnimatedValue()));
        skeletonPulseAnimator.start();
    }

    private void stopSkeletonPulse() {
        if (skeletonPulseAnimator != null) {
            skeletonPulseAnimator.cancel();
            skeletonPulseAnimator = null;
        }
        if (homeSkeleton != null) {
            setSkeletonPlaceholderAlpha(homeSkeleton, 1f);
        }
    }

    private void setSkeletonPlaceholderAlpha(View view, float alpha) {
        // Altera só os itens do skeleton que têm fundo visível.
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                setSkeletonPlaceholderAlpha(group.getChildAt(index), alpha);
            }
        } else if (view.getBackground() != null) {
            view.setAlpha(alpha);
        }
    }

    private void finishHomeRevealIfNeeded() {
        // Finaliza a troca de telas se o app pausar no meio do dissolve.
        if (!homeReady || homeSkeleton == null || homeContent == null
                || homeSkeleton.getVisibility() != View.VISIBLE) {
            return;
        }
        homeSkeleton.animate().cancel();
        homeContent.animate().cancel();
        homeSkeleton.setVisibility(View.GONE);
        homeContent.setAlpha(1f);
        homeContent.setVisibility(View.VISIBLE);
    }

    private void bindHome(View view, CollaboratorHomeData home) {
        setNamedText(view, R.id.text_employee_home_greeting,
                R.string.employee_home_greeting_api, home.getUsername());
        setNamedText(view, R.id.text_employee_home_workspace,
                R.string.employee_home_workspace_api, home.getWorkspaceName());
        CollaboratorHomeData.EventsMetrics metrics = home.getEventsMetrics();
        setText(view, R.id.text_employee_home_pending_count, String.valueOf(metrics.getPending()));
        setText(view, R.id.text_employee_home_review_count, String.valueOf(metrics.getUnderAnalysis()));
        setText(view, R.id.text_employee_home_completed_count, String.valueOf(metrics.getDone()));

        // Aplica o texto, ícone e cor da mensagem definida pela API.
        int status;
        int icon;
        int background;
        int iconColor = R.color.astro_light_background;
        switch (home.getStatusMessage()) {
            case "URGENT":
                status = R.string.employee_home_urgent_pending;
                icon = R.drawable.home_warning;
                background = R.drawable.bg_home_urgent_message;
                break;
            case "PENDENT":
                status = R.string.employee_home_has_pending;
                icon = R.drawable.home_clock;
                background = R.drawable.bg_home_pending_message;
                iconColor = R.color.home_pending_icon;
                break;
            case "ANALYSIS":
                status = R.string.employee_home_under_analysis;
                icon = R.drawable.home_eye;
                background = R.drawable.bg_home_warning_message;
                break;
            default:
                status = R.string.employee_home_no_pending;
                icon = R.drawable.home_check;
                background = R.drawable.bg_home_positive_message;
                break;
        }
        ((TextView) view.findViewById(R.id.text_employee_home_message)).setText(status);
        ImageView messageIcon = view.findViewById(R.id.image_employee_home_message);
        messageIcon.setImageResource(icon);
        messageIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(iconColor, null)));
        view.findViewById(R.id.container_employee_home_message).setBackgroundResource(background);

        LinearLayout events = view.findViewById(R.id.list_employee_home_events);
        events.removeAllViews();
        Instant now = Instant.now();
        ZoneId zone = ZoneId.systemDefault();
        for (CollaboratorHomeData.PendingEvent event : home.getPendingEvents()) {
            // Um item que não existe mais na lista não recebe um status inventado.
            if (event == null || event.getEventData() == null) continue;
            EmployeeEvent item = EmployeeEvent.fromApi(event.getEventData(), zone, now);
            int label;
            int eventIcon = 0;
            boolean red = false;
            switch (item.displayStatus) {
                case URGENT: label = R.string.employee_events_urgent; eventIcon = R.drawable.ic_employee_event_warning; break;
                case NEW: label = R.string.employee_events_new; eventIcon = R.drawable.ic_employee_event_new; break;
                case DONE: label = R.string.employee_events_done; eventIcon = R.drawable.ic_employee_event_done; break;
                case ANALYSIS: label = R.string.employee_events_review; eventIcon = R.drawable.home_clock; break;
                case REJECTED: label = R.string.employee_events_rejected; red = true; break;
                case CANCELLED: label = R.string.employee_events_cancelled; red = true; break;
                case EXPIRED: label = R.string.employee_events_expired; red = true; break;
                default: label = R.string.employee_events_pending;
            }
            if (red) eventIcon = R.drawable.conversations_close;
            View row = addRow(events, event.getTitle(), formatDeadline(item.date.toString()), getString(label), eventIcon);
            if (red) ((ImageView) row.findViewById(R.id.image_home_feed_status)).setImageTintList(
                    ColorStateList.valueOf(requireContext().getColor(R.color.employee_event_status_red)));
            row.setOnClickListener(clicked -> {
                Bundle args = AuthArgs.copy(getArguments());
                args.putLong("event_id", event.getId());
                Navigation.findNavController(clicked).navigate(R.id.action_global_employee_event_details, args);
            });
        }
        showFeed(view, events.getChildCount() == 0, R.id.list_employee_home_events,
                R.id.container_employee_home_events_more, R.id.container_employee_home_events_empty);

        LinearLayout notifications = view.findViewById(R.id.list_employee_home_notifications);
        notifications.removeAllViews();
        for (CollaboratorHomeData.HomeNotification notification : home.getLastNotifications()) {
            if (notification == null) continue;
            long hours = Math.max(0, notification.getSentAtInHours());
            View row = addRow(notifications, notification.getTitle(), null,
                    NotificationTimeFormatter.format(requireContext(), hours),
                    notification.isViewed() ? 0 : R.drawable.home_new);
            row.setOnClickListener(clicked -> {
                Bundle args = AuthArgs.copy(getArguments());
                args.putString(NotificationsFragment.ARG_NOTIFICATION_ID, notification.getId());
                Navigation.findNavController(clicked).navigate(R.id.action_global_notifications, args);
            });
        }
        showFeed(view, notifications.getChildCount() == 0, R.id.list_employee_home_notifications,
                R.id.container_employee_home_notifications_more, R.id.container_employee_home_notifications_empty);
    }

    private void setText(View view, int id, String text) {
        ((TextView) view.findViewById(id)).setText(text);
    }

    private void setNamedText(View view, int id, int template, String name) {
        String text = getString(template, name);
        SpannableString styled = new SpannableString(text);
        styled.setSpan(new StyleSpan(Typeface.BOLD), text.length() - name.length(),
                text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) view.findViewById(id)).setText(styled);
    }

    private String formatDeadline(String date) {
        if (date != null) {
            try {
                return getString(R.string.employee_home_deadline, LocalDate.parse(date).format(DATE));
            } catch (DateTimeParseException ignored) {
                // Prazo ausente ou inválido não impede a exibição dos demais dados.
            }
        }
        return getString(R.string.employee_home_no_deadline);
    }

    private void showFeed(View view, boolean empty, int list, int more, int placeholder) {
        view.findViewById(list).setVisibility(empty ? View.GONE : View.VISIBLE);
        view.findViewById(more).setVisibility(empty ? View.GONE : View.VISIBLE);
        view.findViewById(placeholder).setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private View addRow(LinearLayout container, String title, String subtitle, String meta, int icon) {
        View item = LayoutInflater.from(requireContext()).inflate(R.layout.item_home_feed_row, container, false);
        setText(item, R.id.text_home_feed_title, title);
        setText(item, R.id.text_home_feed_meta, meta);
        setText(item, R.id.text_home_feed_subtitle, subtitle);
        item.findViewById(R.id.text_home_feed_subtitle).setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
        ImageView statusIcon = item.findViewById(R.id.image_home_feed_status);
        if (icon == 0) statusIcon.setVisibility(View.INVISIBLE);
        else statusIcon.setImageResource(icon);
        container.addView(item);
        return item.findViewById(R.id.button_home_feed_row);
    }
}
