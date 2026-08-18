package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LangJson {

    private LangJson() {}

    public static void put(Path file, String key, String value) throws IOException {
        String raw = Files.readString(file, StandardCharsets.UTF_8);
        Files.writeString(file, put(raw, key, value), StandardCharsets.UTF_8);
    }

    public static String put(String raw, String key, String value) throws IOException {
        String newline = raw.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = new ArrayList<>(List.of(raw.split("\r\n|\n|\r", -1)));
        String quotedKey = "\"" + escape(key) + "\"";
        String entry = quotedKey + ": \"" + escape(value) + "\"";

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.stripLeading();
            if (!startsWithKey(trimmed, quotedKey)) {
                continue;
            }
            String indent = line.substring(0, line.length() - trimmed.length());
            boolean comma = line.stripTrailing().endsWith(",");
            lines.set(i, indent + entry + (comma ? "," : ""));
            return String.join(newline, lines);
        }

        int close = -1;
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).strip().startsWith("}")) {
                close = i;
                break;
            }
        }
        if (close < 0) {
            throw new IOException("no closing brace to append after");
        }
        String indent = "  ";
        for (int i = close - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String stripped = line.strip();
            if (!stripped.endsWith(",") && !stripped.endsWith("{")) {
                lines.set(i, line.stripTrailing() + ",");
            }
            if (!stripped.endsWith("{")) {
                indent = line.substring(0, line.length() - line.stripLeading().length());
            }
            break;
        }
        lines.add(close, indent + entry);
        return String.join(newline, lines);
    }

    public static void remove(Path file, String key) throws IOException {
        String raw = Files.readString(file, StandardCharsets.UTF_8);
        Files.writeString(file, remove(raw, key), StandardCharsets.UTF_8);
    }

    public static String remove(String raw, String key) {
        String newline = raw.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = new ArrayList<>(List.of(raw.split("\r\n|\n|\r", -1)));
        String quotedKey = "\"" + escape(key) + "\"";

        for (int i = 0; i < lines.size(); i++) {
            if (!startsWithKey(lines.get(i).stripLeading(), quotedKey)) {
                continue;
            }
            boolean wasLast = !lines.get(i).stripTrailing().endsWith(",");
            lines.remove(i);
            if (wasLast) {
                for (int j = i - 1; j >= 0; j--) {
                    String line = lines.get(j);
                    if (line.isBlank()) {
                        continue;
                    }
                    String trailing = line.stripTrailing();
                    if (trailing.endsWith(",")) {
                        lines.set(j, trailing.substring(0, trailing.length() - 1));
                    }
                    break;
                }
            }
            return String.join(newline, lines);
        }
        return raw;
    }

    private static boolean startsWithKey(String trimmed, String quotedKey) {
        if (!trimmed.startsWith(quotedKey)) {
            return false;
        }
        return trimmed.substring(quotedKey.length()).stripLeading().startsWith(":");
    }

    public static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
