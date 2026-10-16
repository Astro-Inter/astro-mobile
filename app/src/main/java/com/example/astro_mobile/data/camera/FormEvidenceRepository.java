package com.example.astro_mobile.data.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public final class FormEvidenceRepository {
    private final Context context;

    public FormEvidenceRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public Uri createCapture() throws IOException {
        // Expõe somente a pasta temporária de evidências, sem acesso aos demais arquivos do app.
        File directory = new File(context.getCacheDir(), "form_evidences");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Evidence directory unavailable");
        File photo = File.createTempFile("evidence_", ".jpg", directory);
        return FileProvider.getUriForFile(context, context.getPackageName() + ".form-evidences", photo);
    }

    public Bitmap preview(Uri photo) throws IOException {
        // Decodifica uma miniatura, não a foto inteira da câmera na memória da interface.
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), photo),
                (decoder, info, source) -> {
                    int largest = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                    float scale = Math.min(1f, 192f / largest);
                    decoder.setTargetSize(Math.max(1, Math.round(info.getSize().getWidth() * scale)),
                            Math.max(1, Math.round(info.getSize().getHeight() * scale)));
                });
    }

    public void remove(Uri photo) {
        context.getContentResolver().delete(photo, null, null);
    }
}
