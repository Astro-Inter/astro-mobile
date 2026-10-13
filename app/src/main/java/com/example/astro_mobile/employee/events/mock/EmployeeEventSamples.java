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

    public static String descriptionFor(String title) {
        // Conteúdo local do protótipo; nenhum desses textos vem de uma requisição.
        switch (title) {
            case "Integração de segurança":
                return "Integração sobre os procedimentos de segurança, a prevenção de riscos e as responsabilidades no ambiente de trabalho.\n\nFaça o curso em anexo e anexe um print do certificado emitido ao final como evidência.";
            case "Inspeção de equipamentos":
                return "Inspeção das condições dos equipamentos e dos cuidados necessários para utilizá-los com segurança.\n\nConsulte as orientações em anexo e registre a evidência da inspeção.";
            case "Checklist de proteção":
                return "Verificação dos itens de proteção e das medidas de segurança adotadas no ambiente de trabalho.\n\nConsulte o checklist em anexo e registre a evidência da verificação.";
            default:
                return "Treinamento sobre o uso correto, conservação e responsabilidades relacionadas aos Equipamentos de Proteção Individual (EPI) no ambiente de trabalho.\n\nFaça o curso em anexo e anexe um print do certificado emitido ao final como evidência.";
        }
    }
}
