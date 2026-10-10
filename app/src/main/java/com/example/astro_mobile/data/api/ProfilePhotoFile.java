package com.example.astro_mobile.data.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Valida o conteúdo selecionado sem confiar no nome ou no tamanho informado pelo provedor. */
public final class ProfilePhotoFile {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private final byte[] bytes;
    private final String mimeType;
    private final String filename;

    private ProfilePhotoFile(byte[] bytes, String mimeType, String filename) {
        this.bytes = bytes;
        this.mimeType = mimeType;
        this.filename = filename;
    }

    public byte[] getBytes() { return bytes; }
    public String getMimeType() { return mimeType; }
    public String getFilename() { return filename; }

    public static ProfilePhotoFile read(InputStream input) throws IOException {
        if (input == null) throw new IOException("Foto indisponível");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("Leitura cancelada");
            if (output.size() + count > MAX_BYTES) throw new InvalidPhotoException();
            output.write(buffer, 0, count);
        }
        byte[] bytes = output.toByteArray();
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216
                && (bytes[2] & 255) == 255) {
            return new ProfilePhotoFile(bytes, "image/jpeg", "profile.jpg");
        }
        if (bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78
                && bytes[3] == 71 && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) {
            return new ProfilePhotoFile(bytes, "image/png", "profile.png");
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return new ProfilePhotoFile(bytes, "image/webp", "profile.webp");
        }
        throw new InvalidPhotoException();
    }

    public static final class InvalidPhotoException extends IOException { }
}
