package com.example.astro_mobile.manager.staff.mock;

import com.example.astro_mobile.manager.staff.StaffMember;

import java.util.Arrays;
import java.util.List;

public final class StaffSamples {
    private StaffSamples() { }

    public static List<StaffMember> members() {
        // São os três exemplos visuais do Figma, não funcionários recebidos da API.
        return Arrays.asList(
                new StaffMember("Kevin Parker", "Eletricista", 10, 0),
                new StaffMember("Kevin Parker", "Analista de dados", 10, 1),
                new StaffMember("Kevin Parker", "Tech Lead", 0, 2));
    }
}
