package com.example.astro_mobile.chat;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.astro_mobile.R;
import com.google.gson.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ChatFileContentDeviceTest {
    @Test public void twelveZeroMonthsHaveVisibleMeasuredChart() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            JsonObject graph=new JsonObject();graph.addProperty("tipo","grafico_linhas");graph.addProperty("versao",1);
            graph.addProperty("titulo","Conclusões por mês");graph.addProperty("fonte","Dados fictícios");graph.addProperty("unidade","participações");
            JsonArray months=new JsonArray();for(int i=1;i<=12;i++){JsonObject month=new JsonObject();month.addProperty("categoria","Mês "+i);month.addProperty("valor",0);months.add(month);}graph.add("dados",months);
            JsonArray blocks=new JsonArray();blocks.add(graph);LinearLayout root=render(blocks);
            int width=ChatStructuredContent.dp(root.getContext(),240);root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));root.layout(0,0,width,root.getMeasuredHeight());
            LinearLayout card=(LinearLayout)root.getChildAt(0);android.widget.HorizontalScrollView viewport=(android.widget.HorizontalScrollView)card.getChildAt(1);
            assertEquals(ChatStructuredContent.dp(root.getContext(),190),viewport.getHeight());
            assertEquals(viewport.getHeight(),viewport.getChildAt(0).getHeight());assertTrue(viewport.getChildAt(0).getWidth()>viewport.getWidth());
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(viewport.getWidth(),viewport.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
            try {viewport.draw(new android.graphics.Canvas(bitmap));int painted=0;for(int y=0;y<bitmap.getHeight();y++)for(int x=0;x<bitmap.getWidth();x++)if(android.graphics.Color.alpha(bitmap.getPixel(x,y))>0)painted++;assertTrue("Eixos, pontos e linha devem ser desenhados",painted>100);}finally{bitmap.recycle();}
            for(int i=1;i<=12;i++)assertTrue(ChatAnalyticalContentDeviceTest.texts(root).contains(i+". Mês "+i+": 0"));
        });
    }
    static JsonObject file() {
        return JsonParser.parseString("{\"tipo\":\"arquivo\",\"versao\":1,\"id\":\"fc9acaf4-4ea1-416b-a1ed-dc2d1c3d90b4\",\"nome\":\"relatorio-de-rh.pdf\",\"mime_type\":\"application/pdf\",\"tamanho_bytes\":76360,\"url\":\"https://files.example/relatorio.pdf?assinatura=EXEMPLO\",\"expira_em\":\"2099-11-01T20:00:00Z\",\"estado\":\"disponivel\"}").getAsJsonObject();
    }
    static LinearLayout render(JsonArray blocks) {
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);
        ChatStructuredContent.render(root,ChatMarkdown.create(context),"",blocks);return root;
    }
    @Test public void fileKeepsOrderedHistoryAndUnknownBlockDoesNotEraseIt() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            JsonArray blocks=new JsonArray();blocks.add(JsonParser.parseString("{\"tipo\":\"texto\",\"conteudo\":\"Relatório disponível\"}"));
            blocks.add(file());blocks.add(JsonParser.parseString("{\"tipo\":\"futuro\"}"));
            blocks.add(JsonParser.parseString("{\"tipo\":\"texto\",\"conteudo\":\"Fim do relatório\"}"));
            LinearLayout root=render(blocks),history=render(blocks.deepCopy());assertEquals(4,root.getChildCount());
            assertEquals(ChatAnalyticalContentDeviceTest.texts(root),ChatAnalyticalContentDeviceTest.texts(history));
            assertTrue(ChatAnalyticalContentDeviceTest.texts(root.getChildAt(1)).contains("relatorio-de-rh.pdf"));
            assertFalse(ChatAnalyticalContentDeviceTest.texts(root).toString().contains("https://"));
            LinearLayout card=(LinearLayout)root.getChildAt(1);Button action=(Button)card.getChildAt(card.getChildCount()-1);
            assertTrue(action.isEnabled());assertEquals("Baixar PDF: relatorio-de-rh.pdf",action.getContentDescription());
        });
    }
    @Test public void expiredUnavailableAndMissingSizeHaveNoSuccessAction() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for(int kind=0;kind<4;kind++) {
                JsonObject file=file();file.add("tamanho_bytes",JsonNull.INSTANCE);
                if(kind==0)file.addProperty("estado","expirado");
                if(kind==1)file.addProperty("estado","indisponivel");
                if(kind==2)file.add("url",JsonNull.INSTANCE);
                if(kind==3)file.addProperty("expira_em","2020-01-01T00:00:00Z");
                JsonArray blocks=new JsonArray();blocks.add(file);LinearLayout root=render(blocks),card=(LinearLayout)root.getChildAt(0);
                assertEquals(3,card.getChildCount());assertFalse(card.getChildAt(2).isEnabled());
                assertFalse(ChatAnalyticalContentDeviceTest.texts(root).toString().contains("bytes"));
            }
            JsonArray blocks=new JsonArray();blocks.add(JsonParser.parseString("{\"tipo\":\"texto\",\"conteudo\":\"Não foi possível gerar o PDF.\"}"));
            assertEquals(1,render(blocks).getChildCount());
        });
    }
    @Test public void attachedCardExpiresWithoutNetworkOrNewSignature() throws Exception {
        try(androidx.test.core.app.ActivityScenario<com.example.astro_mobile.MainActivity> scenario=
                    androidx.test.core.app.ActivityScenario.launch(com.example.astro_mobile.MainActivity.class)) {
            java.util.concurrent.atomic.AtomicReference<Button> button=new java.util.concurrent.atomic.AtomicReference<>();
            scenario.onActivity(activity -> {
                JsonObject file=file();file.addProperty("expira_em",java.time.Instant.now().plusSeconds(3).toString());
                JsonArray blocks=new JsonArray();blocks.add(file);
                LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);
                ChatStructuredContent.render(root,ChatMarkdown.create(activity),"",blocks);activity.setContentView(root);
                LinearLayout card=(LinearLayout)root.getChildAt(0);button.set((Button)card.getChildAt(card.getChildCount()-1));assertTrue(button.get().isEnabled());
            });
            android.os.SystemClock.sleep(3500);
            scenario.onActivity(activity -> assertFalse(button.get().isEnabled()));
        }
    }
    @Test public void captureAnalyticalPdfPreviewAtTabletWidth() throws Exception {
        try(androidx.test.core.app.ActivityScenario<com.example.astro_mobile.MainActivity> scenario=
                    androidx.test.core.app.ActivityScenario.launch(com.example.astro_mobile.MainActivity.class)) {
            scenario.onActivity(activity -> {
                LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,24,24,24);
                root.setBackgroundColor(activity.getColor(R.color.chat_assistant));
                JsonArray blocks=new JsonArray();blocks.add(JsonParser.parseString("{\"tipo\":\"texto\",\"conteudo\":\"Dados fictícios para homologação. O PDF é um bloco independente.\"}"));blocks.add(file());
                ChatStructuredContent.render(root,ChatMarkdown.create(activity),"",blocks);activity.setContentView(root);
            });
            android.os.SystemClock.sleep(600);
            android.graphics.Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            java.io.File image=new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("chat-qa"),"pdf-simulated.png");
            try(java.io.FileOutputStream output=new java.io.FileOutputStream(image)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}finally{bitmap.recycle();}
        }
    }
}
