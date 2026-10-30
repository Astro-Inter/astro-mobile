package com.example.astro_mobile.chat;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.view.View;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.astro_mobile.MainActivity;
import com.example.astro_mobile.R;
import com.example.astro_mobile.chat.human.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class HumanMessageSwipeTest {
    @Test public void swipesSelectReceivedAndSentMessagesWithoutRemovingRows() {
        verifySwipe(0, 96, false, "received");
        verifySwipe(1, 96, false, "sent");
    }

    @Test public void shortAndOppositeSwipesDoNotSelectReply() {
        verifySwipe(0, 24, false, null);
        verifySwipe(1, -96, false, null);
    }

    @Test public void pendingSendPreventsChangingReply() {
        verifySwipe(0, 96, true, null);
    }

    private void verifySwipe(int position, float distanceDp, boolean pending, String expected) {
        AtomicReference<HumanConversation> conversation = new AtomicReference<>();
        AtomicReference<HumanMessageSwipeToReply> gesture = new AtomicReference<>();
        AtomicReference<RecyclerView> list = new AtomicReference<>();
        AtomicInteger selections = new AtomicInteger();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                HumanConversation model = new HumanConversation("Contato", "Gestor", true, 0);
                model.participantId = 2L;
                HumanMessage received = new HumanMessage("Mensagem recebida para responder", "09:41", false);
                received.id = "received"; received.createdAt = "2026-10-30T12:41:00Z";
                HumanMessage sent = new HumanMessage("Minha mensagem", "09:42", true);
                sent.id = "sent"; sent.createdAt = "2026-10-30T12:42:00Z";
                model.messages.add(received); model.messages.add(sent);
                if (pending) model.pending = new HumanPendingSend(model);
                RecyclerView messages = new RecyclerView(activity);
                messages.setId(R.id.list_human_chat_messages);
                messages.setLayoutManager(new LinearLayoutManager(activity));
                messages.setAdapter(new HumanMessagesAdapter(model, new HumanMessagesAdapter.Listener() {
                    @Override public void onOptions(HumanMessage message, View anchor) { }
                    @Override public void onQuote(String id) { }
                }));
                HumanMessageSwipeToReply swipe = new HumanMessageSwipeToReply(model, message -> {
                    model.replyToMessageId = message.id;
                    selections.incrementAndGet();
                });
                swipe.attach(messages);
                FrameLayout frame = new FrameLayout(activity);
                frame.addView(messages);
                activity.setContentView(frame);
                conversation.set(model); gesture.set(swipe); list.set(messages);
            });
            onView(withId(R.id.list_human_chat_messages)).perform(new GeneralSwipeAction(Swipe.SLOW,
                    view -> coordinates((RecyclerView) view, position, distanceDp < 0 ? 160 : 32),
                    view -> coordinates((RecyclerView) view, position, (distanceDp < 0 ? 160 : 32) + distanceDp),
                    Press.FINGER));
            scenario.onActivity(activity -> {
                assertEquals(expected, conversation.get().replyToMessageId);
                assertEquals(expected == null ? 0 : 1, selections.get());
                assertEquals(2, list.get().getAdapter().getItemCount());
                gesture.get().detach();
            });
        }
    }

    private float[] coordinates(RecyclerView list, int position, float xDp) {
        View row = list.findViewHolderForAdapterPosition(position).itemView;
        int[] location = new int[2];
        row.getLocationOnScreen(location);
        return new float[] {location[0] + xDp * list.getResources().getDisplayMetrics().density,
                location[1] + row.getHeight() / 2f};
    }
}
