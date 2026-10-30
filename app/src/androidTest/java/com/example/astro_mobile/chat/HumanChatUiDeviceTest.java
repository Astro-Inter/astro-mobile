package com.example.astro_mobile.chat;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import android.os.Bundle;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.astro_mobile.MainActivity;
import com.example.astro_mobile.R;
import com.example.astro_mobile.chat.human.HumanChatFragment;
import com.example.astro_mobile.chat.human.HumanConversation;
import com.example.astro_mobile.chat.human.HumanConversationsViewModel;
import com.example.astro_mobile.data.chat.ChatConnectionState;
import com.google.firebase.auth.FirebaseAuth;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifica a tela com a sessão existente, sem publicar mensagens ou alterar credenciais. */
@RunWith(AndroidJUnit4.class)
public final class HumanChatUiDeviceTest {
    @Test public void realHistoryDraftRecreationAndBackgroundUseTheExistingArchitecture() throws Exception {
        assumeTrue("Requer sessão Firebase no aparelho", FirebaseAuth.getInstance().getCurrentUser() != null);
        AtomicReference<HumanConversationsViewModel> model = new AtomicReference<>();
        AtomicReference<HumanConversation> selected = new AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                NavController nav = navigation(activity);
                NavGraph graph = nav.getNavInflater().inflate(R.navigation.nav_graph);
                graph.setStartDestination(R.id.conversationsFragment);
                nav.setGraph(graph);
                model.set(new ViewModelProvider(activity, HumanConversationsViewModel.Factory.createDefault(activity))
                        .get(HumanConversationsViewModel.class));
            });
            await(() -> model.get().isLoaded() && !model.get().isLoading());
            onMain(() -> selected.set(model.get().visible(true, false, "").stream()
                    .filter(item -> item.lastMessageText != null && item.lastMessageText.startsWith("[teste mobile]"))
                    .findFirst().orElse(null)));
            assumeTrue("Requer conversa criada pelo teste autorizado de duas contas", selected.get() != null);
            HumanConversation conversation = selected.get();
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString(HumanChatFragment.CONVERSATION_ID, conversation.conversationId);
                args.putLong(HumanChatFragment.PEER_ID, conversation.participantId);
                args.putString(HumanChatFragment.PEER_NAME, conversation.name);
                args.putString(HumanChatFragment.PEER_ROLE, conversation.role);
                args.putBoolean(HumanChatFragment.PEER_IS_MANAGER, conversation.manager);
                navigation(activity).navigate(R.id.action_conversations_to_human_chat, args);
            });
            await(() -> conversation.loaded && !conversation.historyLoading
                    && model.get().connectionState().getValue() == ChatConnectionState.CONNECTED);
            onView(withId(R.id.text_human_chat_connection)).check(matches(withText(R.string.human_chat_connected)));
            onView(withId(R.id.list_human_chat_messages)).check(matches(isDisplayed()));
            onView(withId(R.id.button_human_chat_send)).check(matches(isEnabled()));
            assertFalse(conversation.messages.isEmpty());
            String original = conversation.draft;
            try {
                onView(withId(R.id.input_human_chat_message)).perform(replaceText("Rascunho de teste sem envio"), closeSoftKeyboard());
                scenario.recreate();
                await(() -> !conversation.historyLoading);
                onView(withId(R.id.input_human_chat_message)).check(matches(withText("Rascunho de teste sem envio")));
                scenario.moveToState(Lifecycle.State.CREATED);
                onMain(() -> assertEquals(ChatConnectionState.STOPPED, model.get().connectionState().getValue()));
                scenario.moveToState(Lifecycle.State.RESUMED);
                await(() -> model.get().connectionState().getValue() == ChatConnectionState.CONNECTED && !conversation.historyLoading);
                onView(withId(R.id.input_human_chat_message)).check(matches(withText("Rascunho de teste sem envio")));
                onView(withId(R.id.text_human_chat_connection)).check(matches(withText(R.string.human_chat_connected)));
            } finally {
                onMain(() -> conversation.draft = original);
            }
        }
    }
    private static NavController navigation(MainActivity activity) {
        return ((NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();
    }
    private static void onMain(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(45);
        AtomicReference<Boolean> ready = new AtomicReference<>(false);
        do {
            onMain(() -> ready.set(condition.getAsBoolean()));
            if (ready.get()) return;
            Thread.sleep(150);
        } while (System.nanoTime() < deadline);
        fail("O estado esperado da tela não foi alcançado");
    }
}
