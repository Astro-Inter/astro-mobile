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
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<RecyclerView> list = new AtomicReference<>();
            scenario.onActivity(activity -> {
                HumanConversation conversation = new HumanConversation("Contato", "Gestor", true, 0);
                HumanMessage original = new HumanMessage("Você concluiu o treinamento de segurança?", "09:41", false);
                original.id = "original"; original.createdAt = "2026-10-30T12:41:00Z";
                HumanMessage response = new HumanMessage("Sim! Finalizei o treinamento e já encaminhei o comprovante.", "09:42", true);
                response.id = "response"; response.createdAt = "2026-10-30T12:42:00Z"; response.replyToMessageId = original.id;
                HumanMessage shortMessage = new HumanMessage("Obrigada!", "09:43", false);
                shortMessage.id = "last"; shortMessage.createdAt = "2026-10-30T12:43:00Z";
                conversation.messages.add(original); conversation.messages.add(response); conversation.messages.add(shortMessage);
                FrameLayout frame = new FrameLayout(activity);
                frame.setBackgroundColor(activity.getColor(R.color.chat_surface));
                RecyclerView messages = new RecyclerView(activity);
                int padding = Math.round(30 * activity.getResources().getDisplayMetrics().density);
                messages.setPadding(padding, padding, padding, padding);
                messages.setLayoutManager(new LinearLayoutManager(activity));
                messages.setAdapter(new HumanMessagesAdapter(conversation, new HumanMessagesAdapter.Listener() {
                    @Override public void onOptions(HumanMessage message, View anchor) { }
                    @Override public void onQuote(String id) { }
                }));
                frame.addView(messages); activity.setContentView(frame); list.set(messages);
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
            });
            Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            File output = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(), "human-message-layout.png");
            try (FileOutputStream stream = new FileOutputStream(output)) { screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream); }
        }
    }
}
