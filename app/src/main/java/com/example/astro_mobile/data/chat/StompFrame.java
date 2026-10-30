package com.example.astro_mobile.data.chat;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Codec STOMP 1.2: comprimento em bytes UTF-8, escapes e frames parciais. */
final class StompFrame {
    final String command;
    final Map<String, String> headers;
    final String body;

    StompFrame(String command, Map<String, String> headers, String body) {
        this.command = command;
        this.headers = headers;
        this.body = body;
    }

    static String encode(String command, Map<String, String> headers, String body) {
        StringBuilder result = new StringBuilder(command).append('\n');
        headers.forEach((key, value) -> result.append(escape(key)).append(':').append(
                "CONNECT".equals(command) ? value : escape(value)).append('\n'));
        if (!body.isEmpty()) result.append("content-length:")
                .append(body.getBytes(StandardCharsets.UTF_8).length).append('\n');
        return result.append('\n').append(body).append('\0').toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n").replace(":", "\\c");
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch != '\\') { result.append(ch); continue; }
            if (++i == value.length()) throw new IllegalArgumentException("Escape STOMP inválido");
            switch (value.charAt(i)) {
                case 'r': result.append('\r'); break;
                case 'n': result.append('\n'); break;
                case 'c': result.append(':'); break;
                case '\\': result.append('\\'); break;
                default: throw new IllegalArgumentException("Escape STOMP inválido");
            }
        }
        return result.toString();
    }

    static final class Decoder {
        private String pending = "";
        void reset() { pending = ""; }

        void accept(String chunk, java.util.function.Consumer<StompFrame> consumer) {
            pending += chunk;
            if (pending.length() > 262144) throw new IllegalArgumentException("Frame STOMP excede limite");
            while (!pending.isEmpty()) {
                if (pending.charAt(0) == '\n' || pending.charAt(0) == '\r') {
                    pending = pending.substring(1);
                    continue;
                }
                int separator = pending.indexOf("\n\n"), separatorLength = 2;
                int crlf = pending.indexOf("\r\n\r\n");
                if (crlf >= 0 && (separator < 0 || crlf < separator)) { separator = crlf; separatorLength = 4; }
                if (separator < 0) return;
                String[] lines = pending.substring(0, separator).split("\r?\n");
                Map<String, String> headers = new LinkedHashMap<>();
                for (int i = 1; i < lines.length; i++) {
                    int colon = lines[i].indexOf(':');
                    if (colon <= 0) throw new IllegalArgumentException("Cabeçalho STOMP inválido");
                    boolean escaped = !"CONNECTED".equals(lines[0]) && !"CONNECT".equals(lines[0]);
                    String key = lines[i].substring(0, colon), value = lines[i].substring(colon + 1);
                    headers.putIfAbsent(escaped ? unescape(key) : key, escaped ? unescape(value) : value);
                }
                int start = separator + separatorLength;
                int end;
                if (headers.containsKey("content-length")) {
                    int length = Integer.parseInt(headers.get("content-length"));
                    if (length < 0 || length > 262144) throw new IllegalArgumentException("Comprimento STOMP inválido");
                    byte[] remaining = pending.substring(start).getBytes(StandardCharsets.UTF_8);
                    if (remaining.length <= length) return;
                    if (remaining[length] != 0) throw new IllegalArgumentException("Terminador STOMP inválido");
                    end = start + new String(remaining, 0, length, StandardCharsets.UTF_8).length();
                } else {
                    end = pending.indexOf('\0', start);
                    if (end < 0) return;
                }
                StompFrame frame = new StompFrame(lines[0], headers, pending.substring(start, end));
                pending = pending.substring(end + 1);
                consumer.accept(frame);
            }
        }
    }
}
