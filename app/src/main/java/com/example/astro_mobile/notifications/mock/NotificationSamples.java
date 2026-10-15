package com.example.astro_mobile.notifications.mock;

import com.example.astro_mobile.R;
import com.example.astro_mobile.notifications.NotificationItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class NotificationSamples {
    private NotificationSamples() { }

    // As amostras reproduzem as listas de Gestor e Colaborador mostradas no Figma.
    public static List<NotificationItem> forManager() {
        return Arrays.asList(
                new NotificationItem(R.string.notifications_manager_validations,
                        R.string.notifications_time, true),
                new NotificationItem(R.string.notifications_manager_form,
                        R.string.notifications_time, true),
                new NotificationItem(R.string.notifications_manager_validations,
                        R.string.notifications_time, false)
        );
    }

    // A lista do Colaborador contém nove lembretes, com os dois primeiros não lidos.
    public static List<NotificationItem> forEmployee() {
        List<NotificationItem> notifications = new ArrayList<>(9);
        for (int index = 0; index < 9; index++) {
            notifications.add(new NotificationItem(R.string.notifications_employee_training,
                    R.string.notifications_time, index < 2));
        }
        return notifications;
    }
}
