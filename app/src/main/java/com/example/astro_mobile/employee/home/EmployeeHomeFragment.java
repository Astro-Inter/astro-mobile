package com.example.astro_mobile.employee.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.astro_mobile.R;

public class EmployeeHomeFragment extends Fragment {

    private static final String STATE_EMPTY_PREVIEW = "empty_preview";

    private static final MockRow[] EVENT_ROWS = {
            new MockRow(R.string.employee_home_training, R.string.employee_home_due_tomorrow,
                    R.string.employee_home_urgent, R.drawable.home_warning),
            new MockRow(R.string.employee_home_training, R.string.employee_home_due_date,
                    R.string.employee_home_new, R.drawable.home_new),
            new MockRow(R.string.employee_home_training, R.string.employee_home_due_date,
                    R.string.employee_home_pending, 0)
    };

    private static final MockRow[] NOTIFICATION_ROWS = {
            new MockRow(R.string.employee_home_notification_item, 0,
                    R.string.employee_home_notification_time, R.drawable.home_new),
            new MockRow(R.string.employee_home_notification_item, 0,
                    R.string.employee_home_notification_time, 0),
            new MockRow(R.string.employee_home_notification_item, 0,
                    R.string.employee_home_notification_time, 0)
    };

    private boolean emptyPreview;

    public EmployeeHomeFragment() {
        super(R.layout.fragment_employee_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyPreview = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_EMPTY_PREVIEW);

        addRows(view.findViewById(R.id.list_employee_home_events), EVENT_ROWS);
        addRows(view.findViewById(R.id.list_employee_home_notifications), NOTIFICATION_ROWS);
        showPreview(view);

        view.findViewById(R.id.text_employee_home_greeting).setOnLongClickListener(pressed -> {
            emptyPreview = !emptyPreview;
            showPreview(view);
            view.findViewById(R.id.scroll_employee_home).scrollTo(0, 0);
            return true;
        });

        int[] mockActions = {
                R.id.button_employee_home_notifications,
                R.id.button_employee_home_events,
                R.id.button_employee_home_pending_card,
                R.id.button_employee_home_review_card,
                R.id.button_employee_home_completed_card,
                R.id.button_employee_home_events_more,
                R.id.button_employee_home_notifications_section,
                R.id.button_employee_home_notifications_more,
                R.id.button_employee_home_nav_events,
                R.id.button_employee_home_nav_chat,
                R.id.button_employee_home_nav_profile,
                R.id.button_employee_home_ai
        };
        for (int id : mockActions) {
            view.findViewById(id).setOnClickListener(clicked -> showMockMessage());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_EMPTY_PREVIEW, emptyPreview);
        super.onSaveInstanceState(outState);
    }

    private void addRows(LinearLayout container, MockRow[] rows) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (MockRow row : rows) {
            View item = inflater.inflate(R.layout.item_home_feed_row, container, false);
            TextView title = item.findViewById(R.id.text_home_feed_title);
            TextView subtitle = item.findViewById(R.id.text_home_feed_subtitle);
            TextView meta = item.findViewById(R.id.text_home_feed_meta);
            ImageView statusIcon = item.findViewById(R.id.image_home_feed_status);

            title.setText(row.title);
            if (row.subtitle == 0) {
                subtitle.setVisibility(View.GONE);
            } else {
                subtitle.setText(row.subtitle);
            }
            meta.setText(row.meta);
            if (row.statusIcon == 0) {
                statusIcon.setVisibility(View.INVISIBLE);
            } else {
                statusIcon.setImageResource(row.statusIcon);
            }
            item.findViewById(R.id.button_home_feed_row)
                    .setOnClickListener(clicked -> showMockMessage());
            container.addView(item);
        }
    }

    private void showPreview(View view) {
        TextView subtitle = view.findViewById(R.id.text_employee_home_subtitle);
        TextView message = view.findViewById(R.id.text_employee_home_message);
        ImageView messageIcon = view.findViewById(R.id.image_employee_home_message);
        View messageContainer = view.findViewById(R.id.container_employee_home_message);

        subtitle.setText(emptyPreview ? R.string.employee_home_subtitle_empty
                : R.string.employee_home_subtitle_filled);
        message.setText(emptyPreview ? R.string.employee_home_urgent_pending
                : R.string.employee_home_no_pending);
        messageIcon.setImageResource(emptyPreview ? R.drawable.home_warning
                : R.drawable.home_check);
        messageContainer.setBackgroundResource(emptyPreview
                ? R.drawable.bg_home_warning_message
                : R.drawable.bg_home_positive_message);

        view.findViewById(R.id.container_employee_home_workspace)
                .setVisibility(emptyPreview ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.list_employee_home_events)
                .setVisibility(emptyPreview ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_employee_home_events_more)
                .setVisibility(emptyPreview ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_employee_home_events_empty)
                .setVisibility(emptyPreview ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.list_employee_home_notifications)
                .setVisibility(emptyPreview ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_employee_home_notifications_more)
                .setVisibility(emptyPreview ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.container_employee_home_notifications_empty)
                .setVisibility(emptyPreview ? View.VISIBLE : View.GONE);
    }

    private void showMockMessage() {
        Toast.makeText(requireContext(), R.string.employee_home_mock_unavailable,
                Toast.LENGTH_SHORT).show();
    }

    private static class MockRow {
        final int title;
        final int subtitle;
        final int meta;
        final int statusIcon;

        MockRow(int title, int subtitle, int meta, int statusIcon) {
            this.title = title;
            this.subtitle = subtitle;
            this.meta = meta;
            this.statusIcon = statusIcon;
        }
    }
}
