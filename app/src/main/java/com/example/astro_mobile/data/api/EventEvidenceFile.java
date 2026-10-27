package com.example.astro_mobile.data.api;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;

public final class EventEvidenceFile {
    public static final long MAX_BYTES = 50L * 1024 * 1024;
    private final Uri uri;
    private final String name;
    private final long size;
    private final String mimeType;

    private EventEvidenceFile(Uri uri, String name, long size, String mimeType) {
        this.uri = uri;
        this.name = name;
        this.size = size;
        this.mimeType = mimeType;
    }

    public Uri getUri() { return uri; }
    public String getName() { return name; }

    public static EventEvidenceFile read(ContentResolver resolver, Uri uri) throws IOException {
        // Consulta só os metadados; o arquivo é transmitido em partes pelo cliente HTTP.
        String name = "evidencia";
        long size = -1;
        try (Cursor cursor = resolver.query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(0);
                if (!cursor.isNull(1)) size = cursor.getLong(1);
            }
        }
        if (size == 0 || size > MAX_BYTES) throw new IOException("Invalid evidence size");
        String type = resolver.getType(uri);
        return new EventEvidenceFile(uri, name == null ? "evidencia" : name, size,
                type == null ? "application/octet-stream" : type);
    }

    public MultipartBody.Part toPart(ContentResolver resolver) {
        RequestBody body = new RequestBody() {
            @Override public MediaType contentType() { return MediaType.parse(mimeType); }
            @Override public long contentLength() { return size; }
            @Override public void writeTo(BufferedSink sink) throws IOException {
                try (InputStream input = resolver.openInputStream(uri)) {
                    if (input == null) throw new IOException("Evidence unavailable");
                    byte[] buffer = new byte[8192];
                    long total = 0;
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        if (total > MAX_BYTES) throw new IOException("Evidence exceeds 50 MB");
                        sink.write(buffer, 0, count);
                    }
                }
            }
        };
        return MultipartBody.Part.createFormData("files", name, body);
    }
}
