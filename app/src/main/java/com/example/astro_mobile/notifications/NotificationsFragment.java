package com.example.astro_mobile.notifications;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;
import com.example.astro_mobile.data.local.FlowPreferences;
import com.example.astro_mobile.shared.navigation.HomeNavigation;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NotificationsFragment extends Fragment {
    private NotificationsViewModel state;
    private NotificationsAdapter adapter;
    private BottomSheetDialog detailDialog;
    private Runnable finishLoading;

    public NotificationsFragment() {
        super(R.layout.fragment_notifications);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this).get(NotificationsViewModel.class);
        state.configure(FlowPreferences.wasManagerFlow(requireContext()));
        adapter = new NotificationsAdapter(state.isManagerFlow(), notification -> showDetails());

        // Liga a lista e executa a busca somente quando o botão ou teclado é acionado.
        RecyclerView list = view.findViewById(R.id.list_notifications);
        list.setAdapter(adapter);
        EditText search = view.findViewById(R.id.input_notifications_search);
        search.setText(state.getSearchQuery());
        view.findViewById(R.id.button_notifications_search).setOnClickListener(clicked -> {
            state.setSearchQuery(search.getText().toString().trim());
            renderNotifications(view);
            hideKeyboard(search);
        });
        search.setOnEditorActionListener((input, action, event) -> {
            state.setSearchQuery(search.getText().toString().trim());
            renderNotifications(view);
            hideKeyboard(search);
            return true;
        });
        view.findViewById(R.id.button_notifications_back).setOnClickListener(clicked ->
                Navigation.findNavController(clicked)
                        .popBackStack(HomeNavigation.destination(requireContext()), false));
        renderNotifications(view);
        showMockLoading(view);
    }

    // Exibe o estado correspondente ao texto pesquisado, incluindo o resultado vazio.
    private void renderNotifications(View view) {
        String query = normalize(state.getSearchQuery());
        List<NotificationItem> visible = new ArrayList<>();
        for (NotificationItem item : state.getNotifications()) {
            if (normalize(getString(item.title)).contains(query)) {
                visible.add(item);
            }
        }
        adapter.submit(visible);
        boolean empty = visible.isEmpty();
        view.findViewById(R.id.container_notifications_empty).setVisibility(empty ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.list_notifications).setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // Permite buscar palavras em português mesmo quando o usuário omite acentos.
    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    // Mostra o skeleton por um segundo para representar a futura consulta da lista.
    private void showMockLoading(View view) {
        View content = view.findViewById(R.id.scroll_notifications_content);
        View skeleton = view.findViewById(R.id.skeleton_notifications);
        if (state.isLoaded()) {
            content.setVisibility(View.VISIBLE);
            skeleton.setVisibility(View.GONE);
            return;
        }
        content.setVisibility(View.INVISIBLE);
        skeleton.setVisibility(View.VISIBLE);
        finishLoading = () -> {
            state.markLoaded();
            content.setVisibility(View.VISIBLE);
            skeleton.setVisibility(View.GONE);
            finishLoading = null;
        };
        view.postDelayed(finishLoading, 1_000L);
    }

    // Abre o detalhe mock da notificação no layout correspondente ao fluxo atual.
    private void showDetails() {
        detailDialog = new BottomSheetDialog(requireContext());
        detailDialog.setContentView(R.layout.sheet_notification_detail);
        View sheet = detailDialog.findViewById(R.id.container_notification_detail_sheet);
        ((TextView) sheet.findViewById(R.id.text_notification_detail_title))
                .setText(R.string.notifications_detail_title);
        ((TextView) sheet.findViewById(R.id.text_notification_detail_body)).setText(
                state.isManagerFlow() ? R.string.notifications_manager_detail_body
                        : R.string.notifications_employee_detail_body);
        View detailCard = sheet.findViewById(R.id.container_notification_detail_card);
        View eventAction = sheet.findViewById(R.id.container_notification_open_event);
        View openEvent = sheet.findViewById(R.id.button_notification_open_event);
        if (state.isManagerFlow()) {
            eventAction.setVisibility(View.GONE);
            sheet.setPadding(0, sheet.getPaddingTop(), 0,
                    getResources().getDimensionPixelSize(R.dimen.notifications_manager_sheet_bottom_padding));
        } else {
            detailCard.setMinimumHeight(
                    getResources().getDimensionPixelSize(R.dimen.notifications_employee_detail_height));
            eventAction.setVisibility(View.VISIBLE);
        }
        openEvent.setOnClickListener(clicked -> {
            detailDialog.dismiss();
            Toast.makeText(requireContext(), R.string.notifications_event_pending,
                    Toast.LENGTH_SHORT).show();
        });
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
        // Libera o atraso e o bottom sheet junto com a interface da tela.
        if (finishLoading != null && getView() != null) getView().removeCallbacks(finishLoading);
        if (detailDialog != null) detailDialog.dismiss();
        finishLoading = null;
        detailDialog = null;
        adapter = null;
        super.onDestroyView();
    }
}
