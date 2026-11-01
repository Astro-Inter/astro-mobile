package com.example.astro_mobile.chat.human;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.astro_mobile.MainActivity;
import com.example.astro_mobile.R;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HumanTypingAvatarDeviceTest {
    @Test public void typingRowShowsPeerAvatarBesideThreeDotsAndKeepsFallback() throws Exception {
        var instrumentation=InstrumentationRegistry.getInstrumentation();var context=instrumentation.getTargetContext();
        File fixture=new File(context.getCacheDir(),"typing-avatar-fixture.png");
        Bitmap bitmap=Bitmap.createBitmap(40,40,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.GREEN);
        try(FileOutputStream out=new FileOutputStream(fixture)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}
        AtomicReference<RecyclerView> list=new AtomicReference<>();AtomicReference<HumanTypingAdapter> adapter=new AtomicReference<>();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                HumanConversation peer=new HumanConversation("Pessoa de exemplo","Gestor",true,0);peer.profilePhotoUrl=android.net.Uri.fromFile(fixture).toString();
                RecyclerView rows=new RecyclerView(activity);rows.setLayoutManager(new LinearLayoutManager(activity));rows.setBackgroundColor(activity.getColor(R.color.chat_surface));
                HumanTypingAdapter typing=new HumanTypingAdapter(peer);rows.setAdapter(typing);activity.setContentView(rows);typing.setTyping(true);adapter.set(typing);list.set(rows);
            });
            AtomicReference<Boolean> loaded=new AtomicReference<>(false);
            for(int attempt=0;attempt<60&&!loaded.get();attempt++) {
                scenario.onActivity(activity -> {var holder=(HumanTypingAdapter.Holder)list.get().findViewHolderForAdapterPosition(0);loaded.set(holder!=null&&holder.avatar.getDrawable() instanceof BitmapDrawable);});
                if(!loaded.get())android.os.SystemClock.sleep(100);
            }
            assertTrue("Foto do participante deve aparecer",loaded.get());
            scenario.onActivity(activity -> {
                var holder=(HumanTypingAdapter.Holder)list.get().findViewHolderForAdapterPosition(0);
                ViewGroup dots=holder.itemView.findViewById(R.id.indicator_human_chat_typing);assertEquals(3,dots.getChildCount());
                int gap=Math.round(8*activity.getResources().getDisplayMetrics().density);
                assertEquals(gap,dots.getLeft()-holder.avatar.getRight());
                assertEquals(dots.getBottom(),holder.avatar.getBottom());
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO,holder.avatar.getImportantForAccessibility());
                assertEquals(Color.GREEN,((BitmapDrawable)holder.avatar.getDrawable()).getBitmap().getPixel(20,20));
            });
            Bitmap capture=instrumentation.getUiAutomation().takeScreenshot();File output=new File(context.getExternalFilesDir("chat-qa"),"typing-avatar-simulated.png");
            try(FileOutputStream out=new FileOutputStream(output)){capture.compress(Bitmap.CompressFormat.PNG,100,out);}finally{capture.recycle();}
            scenario.onActivity(activity -> {
                adapter.get().setTyping(false);assertEquals(0,adapter.get().getItemCount());
                HumanConversation noPhoto=new HumanConversation("Sem foto","Gestor",true,0);
                HumanTypingAdapter fallback=new HumanTypingAdapter(noPhoto);list.get().setAdapter(fallback);fallback.setTyping(true);
            });
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> {var holder=(HumanTypingAdapter.Holder)list.get().findViewHolderForAdapterPosition(0);assertNotNull(holder);assertNotNull(holder.avatar.getDrawable());assertFalse(holder.avatar.getDrawable() instanceof BitmapDrawable);});
        } finally {fixture.delete();}
    }
}
