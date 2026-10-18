package com.example.astro_mobile.notifications;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
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
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.dto.NotificationData;
import com.example.astro_mobile.data.local.FlowPreferences;
import com.example.astro_mobile.shared.navigation.HomeNavigation;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public final class NotificationsFragment extends Fragment {
    private NotificationsViewModel state;
    private NotificationsAdapter adapter;
    private BottomSheetDialog detailDialog;
    private boolean managerFlow;

    public NotificationsFragment() {
        super(R.layout.fragment_notifications);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this, NotificationsViewModel.Factory.createDefault())
                .get(NotificationsViewModel.class);
        managerFlow = FlowPreferences.wasManagerFlow(requireContext());
        adapter = new NotificationsAdapter(managerFlow, this::showDetails);
        view.findViewById(R.id.button_notifications_retry).setOnClickListener(clicked -> loadNotifications());

        // Liga a lista e executa a busca somente quando o botão ou teclado é acionado.
        RecyclerView list = view.findViewById(R.id.list_notifications);
        list.setAdapter(adapter);
        EditText search = view.findViewById(R.id.input_notifications_search);
        search.setText(state.getSearchQuery());
        view.findViewById(R.id.button_notifications_search).setOnClickListener(clicked -> {
            state.setSearchQuery(search.getText().toString());
            renderNotifications(view);
            hideKeyboard(search);
        });
        search.setOnEditorActionListener((input, action, event) -> {
            state.setSearchQuery(search.getText().toString());
            renderNotifications(view);
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
        view.findViewById(R.id.container_notifications_error).setVisibility(View.GONE);
        view.findViewById(R.id.skeleton_notifications).setVisibility(View.VISIBLE);
        state.load(new NotificationsViewModel.ResultCallback() {
            @Override
            public void onSuccess(List<NotificationData> notifications) {
                if (getView() != view || !isResumed()) return;
                view.findViewById(R.id.skeleton_notifications).setVisibility(View.GONE);
                view.findViewById(R.id.scroll_notifications_content).setVisibility(View.VISIBLE);
                renderNotifications(view);
            }

            @Override
            public void onFailure(FailureKind failure) {
                if (getView() != view || !isResumed()) return;
                view.findViewById(R.id.skeleton_notifications).setVisibility(View.GONE);
                if (failure == FailureKind.SESSION) {
                    SessionNavigation.signOut(NotificationsFragment.this);
                    return;
                }
                ((TextView) view.findViewById(R.id.text_notifications_error)).setText(
                        failure == FailureKind.CONNECTION ? R.string.notifications_connection_error
                                : failure == FailureKind.BUSINESS ? R.string.notifications_access_error
                                : R.string.notifications_load_error);
                view.findViewById(R.id.container_notifications_error).setVisibility(View.VISIBLE);
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

    // O contrato da listagem só fornece título, horas e estado de visualização.
    private void showDetails(NotificationData notification) {
        detailDialog = new BottomSheetDialog(requireContext());
        detailDialog.setContentView(R.layout.sheet_notification_detail);
        View sheet = detailDialog.findViewById(R.id.container_notification_detail_sheet);
        ((TextView) sheet.findViewById(R.id.text_notification_detail_title))
                .setText(notification.getTitle());
        ((TextView) sheet.findViewById(R.id.text_notification_detail_body)).setText(
                getString(notification.getHours() == 0 ? R.string.notifications_recent
                        : R.string.notifications_hours, notification.getHours()));
        sheet.findViewById(R.id.container_notification_open_event).setVisibility(View.GONE);
        if (managerFlow) {
            sheet.setPadding(0, sheet.getPaddingTop(), 0,
                    getResources().getDimensionPixelSize(R.dimen.notifications_manager_sheet_bottom_padding));
        }
        sheet.findViewById(R.id.button_notification_detail_close)
                .setOnClickListener(clicked -> detailDialog.dismiss());

        detailDialog.getBehavior().setMaxWidth(
                getResources().getDimensionPixelSize(R.dimen.notifications_detail_max_width));
        detailDialog.show();
        View bottomSheet = detailDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) bottomSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        if (detailDialog.getWindow() != null) detailDialog.getWindow().setDimAmount(0.65f);
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
