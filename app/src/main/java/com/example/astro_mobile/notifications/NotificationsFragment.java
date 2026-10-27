package com.example.astro_mobile.notifications;

import android.content.Context;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.SessionNavigation;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.NotificationsRepository;
import com.example.astro_mobile.data.api.dto.NotificationData;
import com.example.astro_mobile.data.api.dto.NotificationDetailsData;
import com.example.astro_mobile.data.local.FlowPreferences;
import com.example.astro_mobile.manager.events.ManagerEvent;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

import java.util.List;

public final class NotificationsFragment extends Fragment {
    public static final String ARG_NOTIFICATION_ID = "notification_id";

    private NotificationsViewModel state;
    private NotificationsAdapter adapter;
    private Dialog detailDialog;
    private NotificationData retryDetail;
    private boolean managerFlow;
    private String initialNotificationId;

    public NotificationsFragment() {
        super(R.layout.fragment_notifications);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this, NotificationsViewModel.Factory.createDefault())
                .get(NotificationsViewModel.class);
        initialNotificationId = getArguments() == null ? null
                : getArguments().getString(ARG_NOTIFICATION_ID);
        managerFlow = FlowPreferences.wasManagerFlow(requireContext());
        adapter = new NotificationsAdapter(managerFlow, this::showDetails);

        // Filtra a lista já carregada a cada mudança no texto, sem novas requisições.
        RecyclerView list = view.findViewById(R.id.list_notifications);
        list.setAdapter(adapter);
        EditText search = view.findViewById(R.id.input_notifications_search);
        search.setText(state.getSearchQuery());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                state.setSearchQuery(text.toString());
                renderNotifications(view);
            }
            @Override public void afterTextChanged(Editable text) { }
        });
        search.setOnEditorActionListener((input, action, event) -> {
            // O botão do teclado apenas fecha o teclado; a pesquisa já foi aplicada.
            hideKeyboard(search);
            return true;
        });
        view.findViewById(R.id.button_notifications_back).setOnClickListener(clicked ->
                Navigation.findNavController(clicked)
                        .popBackStack(HomeNavigation.destination(requireContext()), false));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
    }

    @Override
    public void onPause() {
        state.cancel();
        if (adapter != null) adapter.submit(java.util.Collections.emptyList());
        if (detailDialog != null) detailDialog.dismiss();
        super.onPause();
    }

    private void loadNotifications() {
        View view = getView();
        if (view == null || state.isLoading()) return;
        adapter.submit(java.util.Collections.emptyList());
        view.findViewById(R.id.scroll_notifications_content).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.skeleton_notifications).setVisibility(View.VISIBLE);
        state.load(new NotificationsViewModel.ResultCallback() {
            @Override
            public void onSuccess(List<NotificationData> notifications) {
                if (getView() != view || !isResumed()) return;
                view.findViewById(R.id.skeleton_notifications).setVisibility(View.GONE);
                view.findViewById(R.id.scroll_notifications_content).setVisibility(View.VISIBLE);
                renderNotifications(view);
                if (retryDetail != null) {
                    NotificationData notification = retryDetail;
                    retryDetail = null;
                    showDetails(notification);
                } else {
                    openInitialNotification();
                }
            }

            @Override
            public void onFailure(FailureKind failure) {
                if (getView() != view || !isResumed()) return;
                view.findViewById(R.id.skeleton_notifications).setVisibility(View.GONE);
                if (failure == FailureKind.SESSION) {
                    SessionNavigation.signOut(NotificationsFragment.this);
                    return;
                }
                androidx.navigation.fragment.NavHostFragment.findNavController(NotificationsFragment.this)
                        .navigate(failure == FailureKind.CONNECTION
                                ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
            }
        });
    }

    // Exibe o estado correspondente ao texto pesquisado, incluindo o resultado vazio.
    private void renderNotifications(View view) {
        List<NotificationData> visible = state.visibleNotifications();
        adapter.submit(visible);
        boolean empty = visible.isEmpty();
        view.findViewById(R.id.container_notifications_empty).setVisibility(empty ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.list_notifications).setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void openInitialNotification() {
        String notificationId = initialNotificationId;
        initialNotificationId = null;
        if (notificationId == null) return;
        for (NotificationData notification : state.getNotifications()) {
            if (notificationId.equals(notification.getId())) {
                showDetails(notification);
                return;
            }
        }
    }

    // Abre o popup centralizado do Figma e busca a mensagem completa.
    private void showDetails(NotificationData notification) {
        if (detailDialog != null) detailDialog.dismiss();
        detailDialog = new Dialog(requireContext());
        detailDialog.setContentView(R.layout.dialog_notification_detail);
        View sheet = detailDialog.findViewById(R.id.container_notification_detail_sheet);
        ((TextView) sheet.findViewById(R.id.text_notification_detail_title))
                .setText(notification.getTitle());
        sheet.findViewById(R.id.container_notification_open_event).setVisibility(View.GONE);
        Dialog dialog = detailDialog;
        sheet.findViewById(R.id.button_notification_detail_close).setOnClickListener(clicked -> dialog.dismiss());
        dialog.setOnDismissListener(ignored -> state.cancelDetail());

        detailDialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.8f);
            int margin = Math.round(30 * getResources().getDisplayMetrics().density);
            int width = Math.min(getResources().getDisplayMetrics().widthPixels - 2 * margin,
                    getResources().getDimensionPixelSize(R.dimen.notifications_detail_max_width));
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        loadDetail(dialog, sheet, notification);
    }

    private boolean isDetailActive(Dialog dialog) {
        return getView() != null && isResumed() && detailDialog == dialog && dialog.isShowing();
    }

    private void setDetailLoading(View sheet) {
        sheet.findViewById(R.id.progress_notification_detail).setVisibility(View.VISIBLE);
        sheet.findViewById(R.id.text_notification_detail_error).setVisibility(View.GONE);
        sheet.findViewById(R.id.button_notification_detail_retry).setVisibility(View.GONE);
    }

    private void loadDetail(Dialog dialog, View sheet, NotificationData notification) {
        setDetailLoading(sheet);
        sheet.findViewById(R.id.container_notification_open_event).setVisibility(View.GONE);
        sheet.findViewById(R.id.text_notification_detail_body).setVisibility(View.GONE);
        state.loadDetail(notification.getId(), new NotificationsRepository.OperationCallback<NotificationDetailsData>() {
            @Override public void onSuccess(NotificationDetailsData detail) {
                if (!isDetailActive(dialog)) return;
                String title = detail.getTitle();
                ((TextView) sheet.findViewById(R.id.text_notification_detail_title)).setText(
                        title == null || title.trim().isEmpty() ? notification.getTitle() : title);
                TextView body = sheet.findViewById(R.id.text_notification_detail_body);
                body.setText(detail.getMessage());
                body.setVisibility(View.VISIBLE);
                sheet.findViewById(R.id.container_notification_open_event)
                        .setVisibility(detail.getEventId() == null ? View.GONE : View.VISIBLE);
                sheet.findViewById(R.id.button_notification_open_event).setOnClickListener(clicked -> {
                    dialog.dismiss();
                    openEvent(detail.getEventId());
                });
                sheet.findViewById(R.id.progress_notification_detail).setVisibility(View.GONE);
                if (!notification.isViewed()) markViewed(dialog, sheet, notification);
            }
            @Override public void onFailure(FailureKind failure) {
                if (!isDetailActive(dialog)) return;
                showDetailError(sheet, notification, failure,
                        () -> loadDetail(dialog, sheet, notification));
            }
        });
    }

    // A mensagem permanece visível mesmo se a confirmação de leitura falhar.
    private void markViewed(Dialog dialog, View sheet, NotificationData notification) {
        setDetailLoading(sheet);
        state.markViewed(notification.getId(), new NotificationsRepository.OperationCallback<Void>() {
            @Override public void onSuccess(Void result) {
                if (!isDetailActive(dialog)) return;
                sheet.findViewById(R.id.progress_notification_detail).setVisibility(View.GONE);
                renderNotifications(requireView());
            }
            @Override public void onFailure(FailureKind failure) {
                if (!isDetailActive(dialog)) return;
                showDetailError(sheet, notification, failure,
                        () -> markViewed(dialog, sheet, notification));
            }
        });
    }

    private void openEvent(long eventId) {
        // Colaborador abre o detalhe real pelo ID; o detalhe administrativo segue na sua tarefa.
        Bundle args = AuthArgs.copy(getArguments());
        args.putLong("event_id", eventId);
        if (managerFlow) {
            args.putString("event_title", getString(R.string.employee_home_training));
            args.putString("event_completion_mode", ManagerEvent.CompletionMode.SELF_DECLARATION.name());
        }
        Navigation.findNavController(requireView()).navigate(managerFlow
                ? R.id.action_notifications_to_manager_event : R.id.action_notifications_to_employee_event, args);
    }

    private void showDetailError(View sheet, NotificationData notification, FailureKind failure, Runnable retry) {
        sheet.findViewById(R.id.progress_notification_detail).setVisibility(View.GONE);
        if (failure == FailureKind.SESSION) {
            detailDialog.dismiss();
            SessionNavigation.signOut(this);
            return;
        }
        if (failure != FailureKind.BUSINESS) {
            retryDetail = notification;
            detailDialog.dismiss();
            Navigation.findNavController(requireView()).navigate(failure == FailureKind.CONNECTION
                    ? R.id.connectionErrorFragment : R.id.genericErrorFragment);
            return;
        }
        TextView error = sheet.findViewById(R.id.text_notification_detail_error);
        error.setText(R.string.notifications_access_error);
        error.setVisibility(View.VISIBLE);
        View button = sheet.findViewById(R.id.button_notification_detail_retry);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(clicked -> retry.run());
    }

    // Fecha o teclado após aplicar a busca.
    private void hideKeyboard(EditText input) {
        InputMethodManager keyboard = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
        input.clearFocus();
    }

    @Override
    public void onDestroyView() {
        state.cancel();
        if (detailDialog != null) detailDialog.dismiss();
        RecyclerView list = getView() == null ? null : getView().findViewById(R.id.list_notifications);
        if (list != null) list.setAdapter(null);
        detailDialog = null;
        adapter = null;
        super.onDestroyView();
    }
}
