package com.example.astro_mobile.chat;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Execute em processo de instrumentação próprio. Somente o host fictício é interceptado.
 * O renderer/loader de produção permanece intacto; não altera TLS ou autenticação do app. */
@RunWith(AndroidJUnit4.class)
public class ChatAvatarCacheCompatibilityTest {
    private static final String HOST="frontend-test.r2.cloudflarestorage.com";
    private static final AtomicInteger REQUESTS=new AtomicInteger();
    private static final AtomicInteger BAD_REQUEST=new AtomicInteger();
    private static byte[] image;
    private static LinearLayout attachedHost;
    private androidx.test.core.app.ActivityScenario<com.example.astro_mobile.MainActivity> scenario;
    @Before public void attachTestSurface() {
        scenario=androidx.test.core.app.ActivityScenario.launch(com.example.astro_mobile.MainActivity.class);
        scenario.onActivity(activity -> {
            attachedHost=new LinearLayout(activity);attachedHost.setOrientation(LinearLayout.VERTICAL);activity.setContentView(attachedHost);
        });
    }
    @After public void closeTestSurface(){scenario.close();attachedHost=null;}
    @BeforeClass public static void isolatedImageTransport() throws Exception {
        Bitmap bitmap=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.GREEN);
        try(ByteArrayOutputStream bytes=new ByteArrayOutputStream()){bitmap.compress(Bitmap.CompressFormat.PNG,100,bytes);image=bytes.toByteArray();}finally{bitmap.recycle();}
        URL originalHandlerContext=new URL("https://example.com/");
        URL.setURLStreamHandlerFactory(protocol -> !"https".equals(protocol)?null:new URLStreamHandler(){
            @Override protected URLConnection openConnection(URL url) throws java.io.IOException {
                if(!HOST.equals(url.getHost()))return new URL(originalHandlerContext,url.toExternalForm()).openConnection();
                return new HttpURLConnection(url) {
                    @Override public void connect(){ }
                    @Override public void disconnect(){ }
                    @Override public boolean usingProxy(){return false;}
                    @Override public int getResponseCode(){
                        REQUESTS.incrementAndGet();
                        if(getUseCaches()||getInstanceFollowRedirects()||getRequestProperty("Authorization")!=null)BAD_REQUEST.incrementAndGet();
                        if(url.getPath().contains("slow"))android.os.SystemClock.sleep(600);
                        return url.getPath().contains("denied")||url.getPath().contains("expired")?403:200;
                    }
                    @Override public java.io.InputStream getInputStream() throws java.io.IOException {
                        if(url.getPath().contains("failure"))throw new java.io.IOException();
                        return new ByteArrayInputStream(url.getPath().contains("invalid")?new byte[]{1,2,3}:image);
                    }
                };
            }
        });
    }
    private static JsonObject photo(String path) {
        JsonObject photo=new JsonObject();photo.addProperty("tipo","imagem");photo.addProperty("descricao","Foto de exemplo");
        if(path==null)photo.add("url",JsonNull.INSTANCE);else photo.addProperty("url","https://"+HOST+"/"+path);
        return photo;
    }
    private static JsonObject profile(String name,String path) {
        JsonObject profile=new JsonObject();profile.addProperty("tipo","ficha_usuario");profile.addProperty("versao",1);profile.addProperty("titulo","Perfil de exemplo");
        if(name!=null)profile.addProperty("nome",name);profile.add("foto",photo(path));return profile;
    }
    private static LinearLayout render(JsonArray blocks) {
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);
        ChatStructuredContent.render(root,ChatMarkdown.create(context),"",blocks);attachedHost.addView(root);return root;
    }
    private static JsonArray one(JsonObject block){JsonArray array=new JsonArray();array.add(block);return array;}
    private static FrameLayout avatar(LinearLayout root){return (FrameLayout)((LinearLayout)((LinearLayout)root.getChildAt(0)).getChildAt(1)).getChildAt(0);}
    private static ImageView image(FrameLayout frame){return (ImageView)frame.getChildAt(1);}
    private static void awaitLoaded(FrameLayout frame) {
        for(int i=0;i<80;i++) {
            AtomicReference<Boolean> loaded=new AtomicReference<>(false);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> loaded.set(image(frame).getDrawable() instanceof BitmapDrawable));
            if(loaded.get())return;android.os.SystemClock.sleep(50);
        }
        fail("Foto simulada não carregou");
    }
    @Test public void ownAndAuthorizedOtherPhotosHaveSamePostHistoryRendering() {
        for(String name:new String[]{"Pessoa Atual","Pessoa Autorizada"}) {
            AtomicReference<LinearLayout> post=new AtomicReference<>(),history=new AtomicReference<>();JsonArray blocks=one(profile(name,"valid.png"));String original=blocks.toString();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {assertTrue(ChatStructuredContent.valid(blocks));post.set(render(blocks));history.set(render(blocks.deepCopy()));});
            awaitLoaded(avatar(post.get()));awaitLoaded(avatar(history.get()));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                assertEquals(ChatAnalyticalContentDeviceTest.texts(post.get()),ChatAnalyticalContentDeviceTest.texts(history.get()));
                assertEquals(Color.GREEN,((BitmapDrawable)image(avatar(post.get())).getDrawable()).getBitmap().getPixel(0,0));
                assertEquals(original,blocks.toString());assertEquals(0,BAD_REQUEST.get());
            });
        }
    }
    @Test public void missingDeniedExpiredAndBrokenPhotosKeepInitialsWithoutRetry() {
        for(String path:new String[]{null,"denied.png","expired.png","failure.png","invalid.png"}) {
            AtomicReference<LinearLayout> root=new AtomicReference<>();int before=REQUESTS.get();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> root.set(render(one(profile("Pessoa Exemplo",path)))));
            android.os.SystemClock.sleep(800);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                FrameLayout frame=avatar(root.get());assertEquals("PE",((TextView)frame.getChildAt(0)).getText().toString());
                assertNull(image(frame).getDrawable());assertEquals(path==null?0:1,REQUESTS.get()-before);
            });
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> assertEquals("●",((TextView)avatar(render(one(profile(null,null)))).getChildAt(0)).getText().toString()));
    }
    @Test public void tableAndLegacyGalleryKeepPhotoFallbackAndReceiveValidImages() {
        JsonObject table=JsonParser.parseString("{\"tipo\":\"tabela\",\"versao\":1,\"titulo\":\"Pessoas\",\"colunas\":[{\"chave\":\"foto\",\"titulo\":\"Foto\"},{\"chave\":\"nome\",\"titulo\":\"Nome\"}],\"linhas\":[{\"nome\":\"Pessoa Exemplo\"}]}").getAsJsonObject();
        table.getAsJsonArray("linhas").get(0).getAsJsonObject().add("foto",photo("valid.png"));
        JsonObject gallery=JsonParser.parseString("{\"tipo\":\"galeria_usuarios\",\"versao\":1,\"titulo\":\"Pessoas\",\"imagens\":[{\"nome\":\"Pessoa Exemplo\"}]}").getAsJsonObject();
        gallery.getAsJsonArray("imagens").get(0).getAsJsonObject().addProperty("url","https://"+HOST+"/valid.png");
        AtomicReference<FrameLayout> tableAvatar=new AtomicReference<>(),galleryAvatar=new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            JsonArray blocks=new JsonArray();blocks.add(table);blocks.add(gallery);assertTrue(ChatStructuredContent.valid(blocks));LinearLayout root=render(blocks);
            LinearLayout grid=(LinearLayout)((android.widget.HorizontalScrollView)((LinearLayout)root.getChildAt(0)).getChildAt(1)).getChildAt(0);
            tableAvatar.set((FrameLayout)((LinearLayout)grid.getChildAt(1)).getChildAt(0));galleryAvatar.set((FrameLayout)((LinearLayout)((LinearLayout)root.getChildAt(1)).getChildAt(1)).getChildAt(0));
        });
        awaitLoaded(tableAvatar.get());awaitLoaded(galleryAvatar.get());assertEquals(0,BAD_REQUEST.get());
    }
    @Test public void laterPhotoCompletionDoesNotPopulateAnotherAccountCard() {
        AtomicReference<LinearLayout> old=new AtomicReference<>(),current=new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {old.set(render(one(profile("Conta Anterior","slow.png"))));current.set(render(one(profile("Conta Atual",null))));});
        awaitLoaded(avatar(old.get()));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {assertNull(image(avatar(current.get())).getDrawable());assertEquals("CA",((TextView)avatar(current.get()).getChildAt(0)).getText().toString());});
    }
    @Test public void ownershipChangeClearsPreviousMessagesDraftsAndSession() throws Exception {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            com.example.astro_mobile.data.ai.AiChatRepository repository=new com.example.astro_mobile.data.ai.AiChatRepository(null,(force,callback)->callback.onToken("fixture"));
            ChatViewModel model=new ChatViewModel(repository,null,com.example.astro_mobile.data.firebase.FirebaseSessionIdentity.createDefault());
            try {
                java.lang.reflect.Field messages=ChatViewModel.class.getDeclaredField("messages");messages.setAccessible(true);
                @SuppressWarnings("unchecked") java.util.List<ChatMessage> previous=(java.util.List<ChatMessage>)messages.get(model);
                previous.add(ChatMessage.fromResponse("Dados da conta anterior",one(profile("Conta Anterior",null))));model.setDraft("Rascunho privado");
                java.lang.reflect.Field session=ChatViewModel.class.getDeclaredField("sessionId");session.setAccessible(true);session.set(model,"c4dd8100-1122-3344-5566-77889900aabb");
                java.lang.reflect.Field owner=ChatViewModel.class.getDeclaredField("owner");owner.setAccessible(true);owner.set(model,"fixture-conta-anterior");
                java.lang.reflect.Method check=ChatViewModel.class.getDeclaredMethod("checkAccount");check.setAccessible(true);assertEquals(false,check.invoke(model));
                assertTrue(model.getState().getValue().messages.isEmpty());assertNull(model.getState().getValue().sessionId);assertEquals("",model.getDraft());
            }catch(ReflectiveOperationException error){throw new AssertionError(error);}finally{model.onCleared();}
        });
    }
}
