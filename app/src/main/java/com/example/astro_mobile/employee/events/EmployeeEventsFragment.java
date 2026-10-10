package com.example.astro_mobile.employee.events;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.astro_mobile.R;
import com.example.astro_mobile.shared.navigation.HomeNavigation;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class EmployeeEventsFragment extends Fragment {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final int[] filterIds = { R.id.filter_employee_events_all, R.id.filter_employee_events_pending,
            R.id.filter_employee_events_review, R.id.filter_employee_events_done };
    private EmployeeEventsViewModel state;
    private Runnable finishLoading;
    private BottomSheetDialog daySheet;

    public EmployeeEventsFragment() { super(R.layout.fragment_employee_events); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        state = new ViewModelProvider(this).get(EmployeeEventsViewModel.class);
        GridLayout calendarGrid = view.findViewById(R.id.grid_employee_events_calendar);
        calendarGrid.addOnLayoutChangeListener((changedView, left, top, right, bottom,
                                                oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft && right > left) {
                renderCalendar(view, state.getMonth());
            }
        });
        // A lista e o calendário compartilham os mesmos filtros locais.
        for (int index = 0; index < filterIds.length; index++) {
            final int selected = index;
            view.findViewById(filterIds[index]).setOnClickListener(clicked -> {
                state.setFilter(selected == 0 ? null : EmployeeEvent.Status.values()[selected - 1]);
                render(view);
            });
        }
        EditText search = view.findViewById(R.id.input_employee_events_search);
        search.setText(state.getQuery());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                state.setQuery(s.toString());
                render(view);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        search.setOnEditorActionListener((input, action, event) -> {
            // A busca acontece ao digitar; o botão do teclado apenas fecha o teclado.
            InputMethodManager keyboard = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
            input.clearFocus();
            return true;
        });
        view.findViewById(R.id.button_employee_events_back).setOnClickListener(clicked ->
                Navigation.findNavController(clicked).popBackStack(HomeNavigation.destination(requireContext()), false));
        view.findViewById(R.id.button_employee_events_previous).setOnClickListener(clicked -> changeMonth(view, -1));
        view.findViewById(R.id.button_employee_events_next).setOnClickListener(clicked -> changeMonth(view, 1));
        view.findViewById(R.id.button_employee_events_month).setOnClickListener(clicked -> showMonthMenu(view, clicked));
        view.findViewById(R.id.button_employee_events_year).setOnClickListener(clicked -> showYearMenu(view, clicked));
        render(view);
        // Simula um segundo de carregamento sem bloquear a interface nem acessar a API.
        if (!state.isLoaded()) {
            View content = view.findViewById(R.id.scroll_employee_events);
            View skeleton = view.findViewById(R.id.skeleton_employee_events);
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

    private void render(View view) {
        // Atualiza os filtros, os dias marcados e as linhas, inclusive o estado vazio.
        int selected = state.getFilter() == null ? 0 : state.getFilter().ordinal() + 1;
        for (int index = 0; index < filterIds.length; index++) view.findViewById(filterIds[index]).setSelected(index == selected);
        YearMonth month = state.getMonth();
        TextView monthButton = view.findViewById(R.id.button_employee_events_month);
        monthButton.setText(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.US));
        TextView yearButton = view.findViewById(R.id.button_employee_events_year);
        yearButton.setText(String.valueOf(month.getYear()));
        renderCalendar(view, month);
        List<EmployeeEvent> events = state.visibleEvents();
        populateRows(view.findViewById(R.id.list_employee_events), events);
        view.findViewById(R.id.text_employee_events_empty).setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.divider_employee_events_empty).setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderCalendar(View view, YearMonth month) {
        // A grade usa sete colunas de mesma largura, sem comprimir o texto no tablet.
        GridLayout grid = view.findViewById(R.id.grid_employee_events_calendar);
        grid.removeAllViews();
        for (String weekday : getResources().getStringArray(R.array.employee_events_weekdays)) {
            TextView cell = addCell(grid, dp(28));
            cell.setText(weekday);
            cell.setTextSize(12);
            cell.setTextColor(requireContext().getColor(R.color.astro_input_hint));
            cell.setBackground(null);
            cell.setClickable(false);
            cell.setFocusable(false);
        }
        int offset = month.atDay(1).getDayOfWeek().getValue() % 7;
        int cells = ((offset + month.lengthOfMonth() + 6) / 7) * 7;
        for (int index = 0; index < cells; index++) {
            LocalDate day = month.atDay(1).plusDays(index - offset);
            TextView cell = addCell(grid, dayCellHeight(grid));
            if (day.isBefore(month.atDay(1))) {
                cell.setText("");
                cell.setClickable(false);
                cell.setFocusable(false);
                continue;
            }
            cell.setText(String.valueOf(day.getDayOfMonth()));
            cell.setContentDescription(getString(R.string.employee_events_day, DATE.format(day)));
            if (!YearMonth.from(day).equals(month)) {
                cell.setAlpha(0.45f);
                cell.setClickable(false);
                cell.setFocusable(false);
                continue;
            }
            boolean hasEvents = !state.eventsOn(day).isEmpty();
            cell.setSelected(hasEvents);
            cell.setActivated(day.equals(LocalDate.now()));
            if (cell.isActivated()) cell.setTextColor(requireContext().getColor(R.color.astro_dark_background));
            cell.setOnClickListener(clicked -> showDay(day));
        }
    }

    private TextView addCell(GridLayout grid, int height) {
        TextView cell = (TextView) getLayoutInflater().inflate(R.layout.item_employee_events_day, grid, false);
        GridLayout.LayoutParams params = (GridLayout.LayoutParams) cell.getLayoutParams();
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.height = height;
        grid.addView(cell, params);
        return cell;
    }

    private int dayCellHeight(GridLayout grid) {
        if (grid.getWidth() == 0) return dp(48);
        return Math.max(dp(36), grid.getWidth() / 7 - dp(2));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void changeMonth(View view, int amount) {
        state.setMonth(state.getMonth().plusMonths(amount));
        render(view);
    }

    private void showMonthMenu(View view, View anchor) {
        // Os seletores permitem navegar para meses e anos sem trocar de tela.
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        for (int month = 1; month <= 12; month++) menu.getMenu().add(0, month, month,
                java.time.Month.of(month).getDisplayName(TextStyle.FULL, PT_BR));
        menu.setOnMenuItemClickListener(item -> {
            state.setMonth(YearMonth.of(state.getMonth().getYear(), item.getItemId()));
            render(view);
            return true;
        });
        menu.show();
    }

    private void showYearMenu(View view, View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        int current = LocalDate.now().getYear();
        for (int year = current - 5; year <= current + 5; year++) menu.getMenu().add(0, year, year, String.valueOf(year));
        menu.setOnMenuItemClickListener(item -> {
            state.setMonth(YearMonth.of(item.getItemId(), state.getMonth().getMonthValue()));
            render(view);
            return true;
        });
        menu.show();
    }

    private void showDay(LocalDate day) {
        // Um toque no dia abre somente o resumo; detalhes completos são outra tarefa.
        List<EmployeeEvent> events = state.eventsOn(day);
        daySheet = new BottomSheetDialog(requireContext());
        View sheet = getLayoutInflater().inflate(R.layout.sheet_employee_events, null);
        ((TextView) sheet.findViewById(R.id.text_employee_events_sheet_date)).setText(getString(R.string.employee_events_day, DATE.format(day)));
        ((TextView) sheet.findViewById(R.id.text_employee_events_sheet_count)).setText(
                events.isEmpty() ? getString(R.string.employee_events_empty)
                        : getResources().getQuantityString(R.plurals.employee_events_count, events.size(), events.size()));
        populateRows(sheet.findViewById(R.id.list_employee_events_sheet), events);
        sheet.findViewById(R.id.button_employee_events_sheet_close).setOnClickListener(clicked -> daySheet.dismiss());
        daySheet.setContentView(sheet);
        daySheet.getBehavior().setMaxWidth(getResources().getDimensionPixelSize(R.dimen.employee_home_content_max_width));
        daySheet.show();
        View parent = (View) sheet.getParent();
        parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        if (daySheet.getWindow() != null) daySheet.getWindow().setDimAmount(0.7f);
    }

    private void populateRows(RecyclerView container, List<EmployeeEvent> events) {
        container.setAdapter(new EmployeeEventsAdapter(events, clicked -> Toast.makeText(requireContext(),
                R.string.employee_events_details_pending, Toast.LENGTH_SHORT).show()));
    }

    @Override
    public void onDestroyView() {
        // Descarta o atraso e o resumo quando as Views da tela deixam de existir.
        if (finishLoading != null) requireView().removeCallbacks(finishLoading);
        finishLoading = null;
        if (daySheet != null) daySheet.dismiss();
        daySheet = null;
        super.onDestroyView();
    }
}
