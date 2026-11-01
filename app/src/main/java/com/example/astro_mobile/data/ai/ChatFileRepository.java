package com.example.astro_mobile.data.ai;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Download explícito e anônimo. Não usa AiApiClient, Firebase, cookies nem redirecionamentos. */
public final class ChatFileRepository {
    public enum Result { SAVED, FAILED, EXPIRED, BUSY }
    public interface Callback { void onResult(Result result); }
    private static final ExecutorService DOWNLOADS = Executors.newFixedThreadPool(2);
    private static final Set<String> ACTIVE = ConcurrentHashMap.newKeySet();
    private final ContentResolver resolver;
    private final OkHttpClient client;
    public ChatFileRepository(Context context) {
        this(context, new OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
                .retryOnConnectionFailure(false).callTimeout(45, TimeUnit.SECONDS).build());
    }
    // Injeção restrita ao pacote para testes de transporte sem acesso ao armazenamento remoto.
    ChatFileRepository(Context context, OkHttpClient client) {
        resolver = context.getApplicationContext().getContentResolver();
        this.client = client;
    }
    public void download(ChatFileBlock file, Callback callback) {
        Handler main = new Handler(Looper.getMainLooper());
        if (file.availability(Instant.now()) != ChatFileBlock.Availability.AVAILABLE) {
            callback.onResult(file.availability(Instant.now()) == ChatFileBlock.Availability.EXPIRED ? Result.EXPIRED : Result.FAILED); return;
        }
        if (!ACTIVE.add(file.getId())) { callback.onResult(Result.BUSY); return; }
        DOWNLOADS.execute(() -> {
            Result result = Result.FAILED;
            Uri destination = null;
            try {
                if (file.availability(Instant.now()) != ChatFileBlock.Availability.AVAILABLE) {
                    result = Result.EXPIRED;
                } else {
                    // Nenhum cabeçalho de autorização é copiado da conexão com a API Astro.
                    Request request = new Request.Builder().url(file.getUrl()).header("Accept", "application/pdf").build();
                    byte[] bytes;
                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null) throw new java.io.IOException();
                        okhttp3.MediaType contentType = response.body().contentType();
                        if (contentType == null || !"application".equals(contentType.type()) || !"pdf".equals(contentType.subtype())) throw new java.io.IOException();
                        if (response.body().contentLength() > ChatFileBlock.MAX_BYTES) throw new java.io.IOException();
                        try (InputStream input = response.body().byteStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                            byte[] buffer = new byte[8192]; int count;
                            while ((count = input.read(buffer)) != -1) {
                                if (output.size() + count > ChatFileBlock.MAX_BYTES) throw new java.io.IOException();
                                output.write(buffer, 0, count);
                            }
                            bytes = output.toByteArray();
                        }
                    }
                    if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D'
                            || bytes[3] != 'F' || bytes[4] != '-') throw new java.io.IOException();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
                    values.put(MediaStore.MediaColumns.MIME_TYPE, file.getMimeType());
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Astro");
                    values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                    destination = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (destination == null) throw new java.io.IOException();
                    try (OutputStream output = resolver.openOutputStream(destination)) {
                        if (output == null) throw new java.io.IOException();
                        output.write(bytes);
                    }
                    values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    if (resolver.update(destination, values, null, null) != 1) throw new java.io.IOException();
                    result = Result.SAVED;
                }
            } catch (Exception ignored) {
                // Mensagens de exceção podem conter a URL assinada; não registrar.
                if (destination != null) try { resolver.delete(destination, null, null); } catch (RuntimeException ignoredCleanup) { }
            } finally {
                ACTIVE.remove(file.getId());
            }
            Result completed = result;
            main.post(() -> callback.onResult(completed));
        });
    }
}
