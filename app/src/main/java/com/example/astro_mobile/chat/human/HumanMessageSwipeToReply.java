package com.example.astro_mobile.chat.human;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.astro_mobile.R;
import java.util.function.Consumer;

public final class HumanMessageSwipeToReply extends ItemTouchHelper.SimpleCallback {
    private final HumanConversation conversation;
    private final Consumer<HumanMessage> onReply;
    private final ItemTouchHelper helper = new ItemTouchHelper(this);
    private String selectedId;
    private boolean ready, feedback;
    private Drawable replyIcon;

    public HumanMessageSwipeToReply(HumanConversation conversation, Consumer<HumanMessage> onReply) {
        super(0, ItemTouchHelper.RIGHT);
        this.conversation = conversation;
        this.onReply = onReply;
    }

    public void attach(RecyclerView messages) { helper.attachToRecyclerView(messages); }
    public void detach() {
        selectedId = null;
        ready = false;
        helper.attachToRecyclerView(null);
    }

    @Override public int getMovementFlags(@NonNull RecyclerView list, @NonNull RecyclerView.ViewHolder holder) {
        if (!(holder.getBindingAdapter() instanceof HumanMessagesAdapter)) return makeMovementFlags(0, 0);
        HumanMessage message = messageAt(holder.getBindingAdapterPosition());
        return makeMovementFlags(0, message == null || message.id == null || conversation.pending != null
                ? 0 : ItemTouchHelper.RIGHT);
    }

    @Override public void onSelectedChanged(@Nullable RecyclerView.ViewHolder holder, int actionState) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && holder != null) {
            HumanMessage message = messageAt(holder.getBindingAdapterPosition());
            selectedId = message == null ? null : message.id;
            ready = feedback = false;
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            HumanMessage target = conversation.findMessage(selectedId);
            boolean reply = ready && target != null && conversation.pending == null;
            selectedId = null;
            ready = feedback = false;
            if (reply) onReply.accept(target);
        }
        super.onSelectedChanged(holder, actionState);
    }

    @Override public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView list,
                                     @NonNull RecyclerView.ViewHolder holder, float dx, float dy,
                                     int actionState, boolean active) {
        float density = list.getResources().getDisplayMetrics().density;
        float distance = Math.max(0, Math.min(dx, 72 * density));
        if (active) {
            ready = dx >= 64 * density;
            if (ready && !feedback) {
                holder.itemView.performHapticFeedback(HapticFeedbackConstants.GESTURE_START);
                feedback = true;
            }
        }
        if (distance > 0) {
            if (replyIcon == null) {
                Drawable resource = AppCompatResources.getDrawable(list.getContext(), R.drawable.ic_arrow_back);
                if (resource != null) {
                    replyIcon = resource.mutate();
                    replyIcon.setTint(list.getContext().getColor(R.color.chat_link));
                }
            }
            Drawable icon = replyIcon;
            if (icon != null) {
                icon.setAlpha(Math.round(255 * Math.min(1, distance / (64 * density))));
                int width = Math.round(20 * density);
                int height = Math.round(16 * density);
                int left = holder.itemView.getLeft() + Math.round((distance - width) / 2);
                int top = holder.itemView.getTop() + (holder.itemView.getHeight() - height) / 2;
                int saved = canvas.save();
                canvas.clipRect(holder.itemView.getLeft(), holder.itemView.getTop(),
                        holder.itemView.getLeft() + distance, holder.itemView.getBottom());
                icon.setBounds(left, top, left + width, top + height);
                icon.draw(canvas);
                canvas.restoreToCount(saved);
            }
        }
        super.onChildDraw(canvas, list, holder, distance, 0, actionState, active);
    }

    @Override public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder holder) { return Float.MAX_VALUE; }
    @Override public float getSwipeEscapeVelocity(float defaultValue) { return Float.MAX_VALUE; }
    @Override public boolean onMove(@NonNull RecyclerView list, @NonNull RecyclerView.ViewHolder source,
                                    @NonNull RecyclerView.ViewHolder target) { return false; }
    @Override public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) { }

    private HumanMessage messageAt(int position) {
        return position < 0 || position >= conversation.messages.size() ? null : conversation.messages.get(position);
    }
}
