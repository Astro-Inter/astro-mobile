package com.example.astro_mobile.data.chat;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public final class StompFrameTest {
    @Test public void decodesPartialUnicodeFramesHeartbeatsAndSeveralFramesTogether() {
        String body = "Olá 🪐\nsegunda linha\0fim";
        String frame = StompFrame.encode("MESSAGE", Map.of("subscription", "private-messages"), body);
        List<StompFrame> frames = new ArrayList<>();
        StompFrame.Decoder decoder = new StompFrame.Decoder();
        decoder.accept("\n\r\n" + frame.substring(0, 20), frames::add);
        assertTrue(frames.isEmpty());
        decoder.accept(frame.substring(20) + "\nCONNECTED\r\nversion:1.2\r\n\r\n\0", frames::add);
        assertEquals(2, frames.size());
        assertEquals(body, frames.get(0).body);
        assertEquals("CONNECTED", frames.get(1).command);
    }
    @Test public void escapesHeadersAndPreservesFirstDuplicate() {
        String encoded = StompFrame.encode("MESSAGE", Map.of("test", "a:b\nc\\d\r"), "");
        List<StompFrame> frames = new ArrayList<>();
        new StompFrame.Decoder().accept(encoded, frames::add);
        assertEquals("a:b\nc\\d\r", frames.get(0).headers.get("test"));
        new StompFrame.Decoder().accept("MESSAGE\nx:first\nx:second\n\n\0", frames::add);
        assertEquals("first", frames.get(1).headers.get("x"));
    }
    @Test public void crlfHeadersDoNotMistakeBlankLinesInBodyForHeaderSeparator() {
        List<StompFrame> frames = new ArrayList<>();
        new StompFrame.Decoder().accept("MESSAGE\r\nsubscription:private-messages\r\n\r\nprimeira\n\nsegunda\0", frames::add);
        assertEquals("primeira\n\nsegunda", frames.get(0).body);
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsMalformedLength() {
        new StompFrame.Decoder().accept("MESSAGE\ncontent-length:-1\n\nx\0", ignored -> fail());
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsMalformedEscape() {
        new StompFrame.Decoder().accept("MESSAGE\nx:bad\\t\n\n\0", ignored -> fail());
    }
}
