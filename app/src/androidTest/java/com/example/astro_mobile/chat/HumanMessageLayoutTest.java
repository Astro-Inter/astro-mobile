package com.example.astro_mobile.chat;

import static org.junit.Assert.*;
import android.graphics.Bitmap;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.astro_mobile.MainActivity;
import com.example.astro_mobile.R;
import com.example.astro_mobile.chat.human.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Verificação local de layout, sem API, autenticação adicional ou envio de mensagens. */
@RunWith(AndroidJUnit4.class)
public final class HumanMessageLayoutTest {
    @Test public void messagesSeparateTimeAndShowQuoteWithoutOverlap() throws Exception {
        verifyLayout(0);
    }

    @Test public void shortRepliesStayReadableOnCompactWidth() throws Exception {
        verifyLayout(320);
    }

    private void verifyLayout(int widthDp) throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<RecyclerView> list = new AtomicReference<>();
            AtomicReference<String> copied = new AtomicReference<>();
            scenario.onActivity(activity -> {
                HumanConversation conversation = new HumanConversation("Contato", "Gestor", true, 0);
                HumanMessage original = new HumanMessage("Você concluiu o treinamento de segurança?", "09:41", false);
                original.id = "original"; original.createdAt = "2026-10-30T12:41:00Z";
                HumanMessage response = new HumanMessage("Sim! Finalizei o treinamento e já encaminhei o comprovante.", "09:42", true);
                response.id = "response"; response.createdAt = "2026-10-30T12:42:00Z"; response.replyToMessageId = original.id;
                HumanMessage shortMessage = new HumanMessage("Obrigada!", "09:43", true);
                shortMessage.id = "last"; shortMessage.createdAt = "2026-10-30T12:43:00Z";
                conversation.messages.add(original); conversation.messages.add(response); conversation.messages.add(shortMessage);
                HumanMessage shortReply = new HumanMessage("Oi", "09:44", true);
                shortReply.id = "short-reply"; shortReply.createdAt = "2026-10-30T12:44:00Z";
                shortReply.replyToMessageId = shortMessage.id;
                conversation.messages.add(shortReply);
                FrameLayout frame = new FrameLayout(activity);
                frame.setBackgroundColor(activity.getColor(R.color.chat_surface));
                RecyclerView messages = new RecyclerView(activity);
                int padding = Math.round(30 * activity.getResources().getDisplayMetrics().density);
                messages.setPadding(padding, padding, padding, padding);
                messages.setLayoutManager(new LinearLayoutManager(activity));
                messages.setAdapter(new HumanMessagesAdapter(conversation, new HumanMessagesAdapter.Listener() {
                    @Override public void onQuote(String id) { }
                    @Override public void onCopy(HumanMessage message) { copied.set(message.text); }
                }));
                int width = widthDp == 0 ? FrameLayout.LayoutParams.MATCH_PARENT
                        : Math.round(widthDp * activity.getResources().getDisplayMetrics().density);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, FrameLayout.LayoutParams.MATCH_PARENT);
                params.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                frame.addView(messages, params); activity.setContentView(frame); list.set(messages);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                View received = list.get().findViewHolderForAdapterPosition(0).itemView;
                View sent = list.get().findViewHolderForAdapterPosition(1).itemView;
                View text = sent.findViewById(R.id.text_human_message_content);
                View time = sent.findViewById(R.id.text_human_message_time);
                assertTrue(time.getTop() >= text.getBottom());
                assertEquals(View.VISIBLE, sent.findViewById(R.id.container_human_quote).getVisibility());
                assertEquals("Você concluiu o treinamento de segurança?",
                        ((TextView) sent.findViewById(R.id.text_human_quote_content)).getText().toString());
                assertTrue(sent.findViewById(R.id.container_human_message).getRight()
                        > received.findViewById(R.id.container_human_message).getRight());
                assertEquals(View.GONE, sent.findViewById(R.id.image_human_message_avatar).getVisibility());
                assertEquals(View.VISIBLE, received.findViewById(R.id.image_human_message_avatar).getVisibility());
                View reply = list.get().findViewHolderForAdapterPosition(3).itemView;
                TextView author = reply.findViewById(R.id.text_human_quote_author);
                TextView quote = reply.findViewById(R.id.text_human_quote_content);
                assertEquals("Você", author.getText().toString());
                assertEquals(1, author.getLineCount());
                assertEquals(1, ((TextView) reply.findViewById(R.id.text_human_message_content)).getLineCount());
                float density = activity.getResources().getDisplayMetrics().density;
                int timeGap = time.getTop() - text.getBottom();
                assertTrue("Timestamp must stay close to the message", timeGap <= 12 * density);
                assertTrue("Short replies must reserve readable space for the quote", quote.getWidth() >= 120 * density);
                View replyTime = reply.findViewById(R.id.text_human_message_time);
                assertTrue(replyTime.getTop()
                        >= reply.findViewById(R.id.text_human_message_content).getBottom());
                View bubble = reply.findViewById(R.id.container_human_message);
                bubble.performClick();
                assertNull("A tap must not copy the message", copied.get());
                assertFalse("A tap must not open an options menu", bubble.hasOnClickListeners());
                assertTrue(bubble.performLongClick());
                assertEquals("Oi", copied.get());
                copied.set(null);
                assertTrue(reply.findViewById(R.id.container_human_quote).performLongClick());
                assertEquals("Oi", copied.get());
                assertEquals(4, list.get().getAdapter().getItemCount());
            });
            Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            File output = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(), "human-message-layout.png");
            try (FileOutputStream stream = new FileOutputStream(output)) { screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream); }
        }
    }
}
