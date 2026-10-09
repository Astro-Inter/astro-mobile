package com.example.astro_mobile.employee.events.mock;

import com.example.astro_mobile.employee.events.EmployeeEvent;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class EmployeeEventSamples {
    private EmployeeEventSamples() { }

    public static List<EmployeeEvent> forMonth(YearMonth month) {
        // Quatro estados distintos bastam para exercitar calendário e filtros sem lotar a tela.
        List<EmployeeEvent> events = new ArrayList<>();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int firstDay = YearMonth.from(tomorrow).equals(month) ? tomorrow.getDayOfMonth() : 10;
        if (firstDay > month.lengthOfMonth() - 3) firstDay = month.lengthOfMonth() - 3;
        events.add(event("Treinamento de EPI", month, firstDay, EmployeeEvent.Status.PENDING, true));
        events.add(event("Integração de segurança", month, firstDay + 1, EmployeeEvent.Status.PENDING, false));
        events.add(event("Inspeção de equipamentos", month, firstDay + 2, EmployeeEvent.Status.REVIEW, false));
        events.add(event("Checklist de proteção", month, firstDay + 3, EmployeeEvent.Status.DONE, false));
        return events;
    }

    private static EmployeeEvent event(String title, YearMonth month, int day,
                                       EmployeeEvent.Status status, boolean urgent) {
        return new EmployeeEvent(title, month.atDay(day), status, urgent);
    }
}
