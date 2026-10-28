package com.example.astro_mobile.manager.staff;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astro_mobile.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;
import java.util.function.Consumer;

public final class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.Row> {
    private final List<StaffMember> members;
    private final Consumer<StaffMember> memberClick;

    public StaffAdapter(List<StaffMember> members, Consumer<StaffMember> memberClick) {
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
        // Mostra foto ou as iniciais fornecidas pela API enquanto a foto não carrega.
        StaffMember member = members.get(position);
        row.name.setText(member.name);
        row.role.setText(member.role);
        row.initials.setText(member.initials);
        row.initials.setVisibility(View.VISIBLE);
        Glide.with(row.itemView).clear(row.avatar);
        row.avatar.setImageDrawable(null);
        row.avatar.setVisibility(View.GONE);
        if (member.photoUrl != null && !member.photoUrl.trim().isEmpty()) {
            // Mantém a ImageView medida durante o carregamento; as iniciais ficam por cima até a foto chegar.
            row.avatar.setVisibility(View.VISIBLE);
            Glide.with(row.itemView).load(member.photoUrl).circleCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override public boolean onLoadFailed(@Nullable GlideException error, Object model,
                                Target<Drawable> target, boolean firstResource) {
                            row.avatar.setVisibility(View.GONE);
                            row.initials.setVisibility(View.VISIBLE);
                            return false;
                        }

                        @Override public boolean onResourceReady(Drawable resource, Object model,
                                Target<Drawable> target, DataSource dataSource, boolean firstResource) {
                            row.initials.setVisibility(View.GONE);
                            row.avatar.setVisibility(View.VISIBLE);
                            return false;
                        }
                    }).into(row.avatar);
        }
        row.pending.setText(row.itemView.getResources().getQuantityString(
                R.plurals.staff_pending_count, (int) member.pendingCount, member.pendingCount));
        row.button.setOnClickListener(clicked -> memberClick.accept(member));
    }

    @Override
    public int getItemCount() { return members.size(); }

    static final class Row extends RecyclerView.ViewHolder {
        final TextView name, role, pending;
        final TextView initials;
        final ImageView avatar;
        final View button;

        Row(View view) {
            super(view);
            name = view.findViewById(R.id.text_staff_name);
            role = view.findViewById(R.id.text_staff_role);
            pending = view.findViewById(R.id.text_staff_pending);
            initials = view.findViewById(R.id.text_staff_avatar_initials);
            avatar = view.findViewById(R.id.image_staff_avatar);
            button = view.findViewById(R.id.button_staff_member);
        }
    }
}
