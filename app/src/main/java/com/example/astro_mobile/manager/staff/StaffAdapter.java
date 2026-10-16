package com.example.astro_mobile.manager.staff;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;

import java.util.List;

public final class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.Row> {
    private final int[] avatars = {R.drawable.participant_avatar_1,
            R.drawable.participant_avatar_2, R.drawable.participant_avatar_3};
    private final List<StaffMember> members;
    private final View.OnClickListener memberClick;

    public StaffAdapter(List<StaffMember> members, View.OnClickListener memberClick) {
        this.members = members;
        this.memberClick = memberClick;
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staff_member, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        // Preenche o retrato, cargo e estado de cada exemplo sem mudar os dados do mock.
        StaffMember member = members.get(position);
        row.name.setText(member.name);
        row.role.setText(member.role);
        row.avatar.setImageResource(avatars[member.avatarIndex]);
        row.pending.setText(member.pendingCount == 0
                ? row.itemView.getContext().getString(R.string.staff_no_pending)
                : row.itemView.getResources().getQuantityString(R.plurals.staff_pending_count,
                        member.pendingCount, member.pendingCount));
        row.button.setOnClickListener(memberClick);
    }

    @Override
    public int getItemCount() { return members.size(); }

    static final class Row extends RecyclerView.ViewHolder {
        final TextView name, role, pending;
        final ImageView avatar;
        final View button;

        Row(View view) {
            super(view);
            name = view.findViewById(R.id.text_staff_name);
            role = view.findViewById(R.id.text_staff_role);
            pending = view.findViewById(R.id.text_staff_pending);
            avatar = view.findViewById(R.id.image_staff_avatar);
            button = view.findViewById(R.id.button_staff_member);
        }
    }
}
