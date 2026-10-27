package com.example.astro_mobile.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;
import com.example.astro_mobile.data.api.dto.NotificationData;

import java.util.ArrayList;
import java.util.List;

public final class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {
    private final boolean managerFlow;
    private final OnNotificationClickListener clickListener;
    private final List<NotificationData> notifications = new ArrayList<>();

    public NotificationsAdapter(boolean managerFlow, OnNotificationClickListener clickListener) {
        this.managerFlow = managerFlow;
        this.clickListener = clickListener;
    }

    // Troca a lista visível depois da consulta ou da busca local.
    public void submit(List<NotificationData> items) {
        notifications.clear();
        notifications.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        View content = row.findViewById(R.id.container_notification_row);
        float density = parent.getResources().getDisplayMetrics().density;
        int minHeight = Math.round((managerFlow ? 80 : 60) * density);
        int verticalPadding = Math.round((managerFlow ? 18 : 8) * density);
        content.setMinimumHeight(minHeight);
        content.setPadding(content.getPaddingLeft(), verticalPadding,
                content.getPaddingRight(), verticalPadding);
        return new NotificationViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationData notification = notifications.get(position);
        holder.title.setText(notification.getTitle());
        holder.time.setText(NotificationTimeFormatter.format(holder.itemView.getContext(),
                notification.getHours()));
        holder.unreadDot.setVisibility(notification.isViewed() ? View.INVISIBLE : View.VISIBLE);
        holder.itemView.findViewById(R.id.container_notification_row)
                .setOnClickListener(clicked -> clickListener.onNotificationClick(notification));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationData notification);
    }

    static final class NotificationViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView time;
        final ImageView unreadDot;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_notification_title);
            time = itemView.findViewById(R.id.text_notification_time);
            unreadDot = itemView.findViewById(R.id.image_notification_unread);
        }
    }
}
