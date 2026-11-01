package com.example.astro_mobile.employee.events;

import static org.junit.Assert.assertEquals;
import com.example.astro_mobile.data.api.dto.CollaboratorEventData;
import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import org.junit.Test;

public final class EmployeeEventsViewModelTest {
    @Test public void calendarMonthDoesNotFilterTheListAndListFiltersDoNotHideCalendarEvents() throws Exception {
        LocalDate later = LocalDate.now().plusMonths(2);
        String json = "{\"id\":1,\"eventTitle\":\"Treinamento\",\"classLimitDate\":\""
                + later + "T12:00:00\",\"viewed\":true,\"participationStatus\":\"PENDENTE\",\"eventStatus\":\"ATIVO\"}";
        CollaboratorEventData event = new Gson().fromJson(json, CollaboratorEventData.class);
        EmployeeEventsViewModel model = new EmployeeEventsViewModel(null);
        Field events = EmployeeEventsViewModel.class.getDeclaredField("events");
        events.setAccessible(true);
        events.set(model, Arrays.asList(event));
        model.setMonth(YearMonth.now());
        assertEquals(1, model.visibleEvents().size());
        model.setQuery("outro evento");
        assertEquals(0, model.visibleEvents().size());
        assertEquals(1, model.eventsOn(later).size());
    }
}
