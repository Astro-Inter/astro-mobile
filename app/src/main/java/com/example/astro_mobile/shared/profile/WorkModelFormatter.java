package com.example.astro_mobile.shared.profile;

import android.content.Context;
import com.example.astro_mobile.R;
import java.util.Locale;

public final class WorkModelFormatter {
    private WorkModelFormatter() { }

    // Apresenta a modalidade sem alterar o valor recebido da API.
    public static String format(Context context, String value) {
        if (value == null || value.trim().isEmpty()) return value;
        switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "PRESENCIAL": return context.getString(R.string.staff_detail_work_presencial);
            case "HIBRIDO":
            case "HÍBRIDO": return context.getString(R.string.staff_detail_work_hybrid);
            case "REMOTO": return context.getString(R.string.staff_detail_work_remote);
            default:
                String text = value.trim().toLowerCase(new Locale("pt", "BR"));
                return text.substring(0, 1).toUpperCase(new Locale("pt", "BR")) + text.substring(1);
        }
    }
}
