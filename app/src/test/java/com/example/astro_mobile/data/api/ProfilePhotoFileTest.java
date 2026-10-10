package com.example.astro_mobile.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class ProfilePhotoFileTest {
    @Test
    public void detectsSupportedFormatsFromContent() throws IOException {
        assertEquals("image/jpeg", read(new byte[] { (byte) 255, (byte) 216, (byte) 255 }).getMimeType());
        assertEquals("image/png", read(new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10 }).getMimeType());
        assertEquals("image/webp", read(new byte[] { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P' }).getMimeType());
        assertThrows(ProfilePhotoFile.InvalidPhotoException.class, () -> read(new byte[] { 'G', 'I', 'F' }));
        assertThrows(ProfilePhotoFile.InvalidPhotoException.class, () -> read(new byte[0]));
    }

    @Test
    public void acceptsLimitAndRejectsLargerFiles() throws IOException {
        byte[] bytes = new byte[ProfilePhotoFile.MAX_BYTES];
        bytes[0] = (byte) 255; bytes[1] = (byte) 216; bytes[2] = (byte) 255;
        assertEquals(ProfilePhotoFile.MAX_BYTES, read(bytes).getBytes().length);
        assertThrows(ProfilePhotoFile.InvalidPhotoException.class,
                () -> read(new byte[ProfilePhotoFile.MAX_BYTES + 1]));
    }

    private ProfilePhotoFile read(byte[] bytes) throws IOException {
        return ProfilePhotoFile.read(new ByteArrayInputStream(bytes));
    }
}
