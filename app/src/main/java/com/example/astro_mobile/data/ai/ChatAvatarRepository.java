package com.example.astro_mobile.data.ai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Avatares assinados da IA: sem cache, redirecionamento ou registro das URLs. */
public final class ChatAvatarRepository {
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(3);
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final Handler main = new Handler(Looper.getMainLooper());

    public interface Callback { void onLoaded(Bitmap bitmap); }

    public static final class Operation {
        private volatile boolean canceled;
        private volatile HttpURLConnection connection;
        private Future<?> task;

        public void cancel() {
            canceled = true;
            if (task != null) task.cancel(true);
            HttpURLConnection current = connection;
            if (current != null) current.disconnect();
        }
    }

    public Operation load(String url, Callback callback) {
        Operation operation = new Operation();
        if (!isAllowedUrl(url)) return operation;
        operation.task = WORKERS.submit(() -> {
            HttpURLConnection connection = null;
            try {
                if (operation.canceled) return;
                connection = (HttpURLConnection) new URI(url).toURL().openConnection();
                operation.connection = connection;
                if (operation.canceled) return;
                connection.setUseCaches(false);
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                if (connection.getResponseCode() != 200 || connection.getContentLengthLong() > MAX_BYTES) return;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream input = connection.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (operation.canceled || bytes.size() + count > MAX_BYTES) return;
                        bytes.write(buffer, 0, count);
                    }
                }
                byte[] data = bytes.toByteArray();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
                if (options.outWidth <= 0 || options.outHeight <= 0 || operation.canceled) return;
                options.inJustDecodeBounds = false;
                options.inSampleSize = 1;
                while (options.outWidth / options.inSampleSize > 256 || options.outHeight / options.inSampleSize > 256)
                    options.inSampleSize *= 2;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                if (bitmap != null) main.post(() -> {
                    if (!operation.canceled) callback.onLoaded(bitmap);
                });
            } catch (Exception ignored) {
                // Falha de acesso ou formato mantém as iniciais exibidas pela interface.
            } finally {
                operation.connection = null;
                if (connection != null) connection.disconnect();
            }
        });
        return operation;
    }

    static boolean isAllowedUrl(String value) {
        if (value == null) return false;
        try {
            URI uri = new URI(value);
            return "https".equals(uri.getScheme()) && uri.getHost() != null
                    && uri.getHost().endsWith(".r2.cloudflarestorage.com") && uri.getUserInfo() == null;
        } catch (Exception ignored) { return false; }
    }
}
