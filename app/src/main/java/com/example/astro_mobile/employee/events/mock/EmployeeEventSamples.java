package com.example.astro_mobile.employee.events.mock;

public final class EmployeeEventSamples {
    private EmployeeEventSamples() { }

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
