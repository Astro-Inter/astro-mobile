package com.example.astro_mobile.data.ai;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Transporte simulado HTTPS, persistência real no MediaStore; apaga somente os PDFs criados pelo teste. */
@RunWith(AndroidJUnit4.class)
public class ChatFileRepositoryDeviceTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private JsonObject block(String name) {
        return JsonParser.parseString("{\"tipo\":\"arquivo\",\"versao\":1,\"id\":\""+UUID.randomUUID()+"\",\"nome\":\""+name+"\",\"mime_type\":\"application/pdf\",\"url\":\"https://files.example/relatorio.pdf?assinatura=EXEMPLO\",\"expira_em\":\"2099-11-01T20:00:00Z\",\"estado\":\"disponivel\"}").getAsJsonObject();
    }
    private ChatFileRepository.Result execute(JsonObject block, int code, byte[] body, AtomicInteger calls) throws Exception {
        OkHttpClient client=new OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).addInterceptor(chain -> {
            calls.incrementAndGet();
            assertNull(chain.request().header("Authorization"));
            assertNull(chain.request().header("Cookie"));
            assertEquals("application/pdf",chain.request().header("Accept"));
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("Simulado")
                    .header("Location","https://other.example/ignored.pdf")
                    .body(ResponseBody.create(MediaType.get("application/pdf"),body)).build();
        }).build();
        AtomicReference<ChatFileRepository.Result> result=new AtomicReference<>();CountDownLatch done=new CountDownLatch(1);
        new ChatFileRepository(context,client).download(ChatFileBlock.fromJson(block),value -> {result.set(value);done.countDown();});
        assertTrue(done.await(15,TimeUnit.SECONDS));return result.get();
    }
    private Uri find(String name) {
        try(Cursor cursor=context.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.MediaColumns._ID},MediaStore.MediaColumns.DISPLAY_NAME+"=?",new String[]{name},null)) {
            if(cursor!=null&&cursor.moveToFirst())return android.content.ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cursor.getLong(0));
            return null;
        }
    }
    @Test public void explicitDownloadSavesPdfWithoutAstroCredentials() throws Exception {
        String name="astro-teste-"+UUID.randomUUID()+".pdf";
        byte[] pdf="%PDF-1.4\n% Documento simulado\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        Uri saved=null;
        try {
            AtomicInteger calls=new AtomicInteger();
            assertEquals(ChatFileRepository.Result.SAVED,execute(block(name),200,pdf,calls));
            assertEquals(1,calls.get());saved=find(name);assertNotNull(saved);
            try(java.io.InputStream input=context.getContentResolver().openInputStream(saved)) {
                assertNotNull(input);byte[] actual=new byte[pdf.length];assertEquals(pdf.length,input.read(actual));assertArrayEquals(pdf,actual);
            }
        } finally {if(saved==null)saved=find(name);if(saved!=null)context.getContentResolver().delete(saved,null,null);}
    }
    @Test public void expiredAndMissingUrlsDoNotStartTransport() throws Exception {
        for(String state:new String[]{"expirado","indisponivel","disponivel"}) {
            JsonObject block=block("nao-baixar.pdf");block.addProperty("estado",state);block.add("url",com.google.gson.JsonNull.INSTANCE);
            AtomicInteger calls=new AtomicInteger();assertEquals("expirado".equals(state)?ChatFileRepository.Result.EXPIRED:ChatFileRepository.Result.FAILED,execute(block,200,new byte[0],calls));assertEquals(0,calls.get());
        }
    }
    @Test public void deniedRedirectMalformedAndOversizeNeverCreateSuccessFile() throws Exception {
        for(int code:new int[]{403,404,500,302,200}) {
            String name="astro-falha-"+UUID.randomUUID()+".pdf";AtomicInteger calls=new AtomicInteger();
            assertEquals(ChatFileRepository.Result.FAILED,execute(block(name),code,"nao e um PDF".getBytes(),calls));
            assertEquals(1,calls.get());assertNull(find(name));
        }
        String name="astro-limite-"+UUID.randomUUID()+".pdf";AtomicInteger calls=new AtomicInteger();
        assertEquals(ChatFileRepository.Result.FAILED,execute(block(name),200,new byte[ChatFileBlock.MAX_BYTES+1],calls));assertNull(find(name));
    }
}
