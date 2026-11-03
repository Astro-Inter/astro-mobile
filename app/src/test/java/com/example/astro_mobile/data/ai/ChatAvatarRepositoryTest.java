package com.example.astro_mobile.data.ai;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChatAvatarRepositoryTest {
    @Test public void acceptsSignedHttpsAvatarFromStorage() {
        assertTrue(ChatAvatarRepository.isAllowedUrl(
                "https://account.r2.cloudflarestorage.com/avatar/pessoa%20teste.jpg?signature=example"));
    }

    @Test public void rejectsOtherOriginsAndEmbeddedCredentials() {
        String[] urls = {"http://account.r2.cloudflarestorage.com/avatar.jpg",
                "https://example.com/avatar.jpg", "file:///tmp/avatar.jpg",
                "https://account.r2.cloudflarestorage.com.evil.example/avatar.jpg",
                "https://user@account.r2.cloudflarestorage.com/avatar.jpg"};
        for (String url : urls) assertFalse(url, ChatAvatarRepository.isAllowedUrl(url));
    }

    @Test public void rejectsMissingOrMalformedUrl() {
        assertFalse(ChatAvatarRepository.isAllowedUrl(null));
        assertFalse(ChatAvatarRepository.isAllowedUrl(""));
        assertFalse(ChatAvatarRepository.isAllowedUrl("https://account.r2.cloudflarestorage.com/bad path"));
    }
}
