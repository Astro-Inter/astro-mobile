package com.example.astro_mobile.manager.forms.mock;

import com.example.astro_mobile.manager.forms.ManagerForm;

import java.util.Arrays;
import java.util.List;

public final class ManagerFormSamples {
    private ManagerFormSamples() { }

    public static List<ManagerForm> all() {
        // Exemplos visuais do Figma; a lista real será carregada pela API posteriormente.
        String title = "Checklist de Inspeção de segurança";
        return Arrays.asList(
                new ManagerForm(title, "Amanhã", "NR - 5", ManagerForm.Status.URGENT, true),
                new ManagerForm(title, "20/05/2027", "NR - 5", ManagerForm.Status.NEW, true),
                new ManagerForm(title, "20/05/2027", "NR - 5", ManagerForm.Status.PENDING, false),
                new ManagerForm(title, "20/05/2027", "NR - 5", ManagerForm.Status.FINISHED, true));
    }
}
