package com.sockywocky.createaddonorganizer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import net.neoforged.fml.loading.FMLPaths;

public final class ItemObliteratorConfig {

    public static final String KEY = "blacklisted_items";
    public static final String REGEX_PREFIX = "!";

    private static final String[] FILE_NAMES = {"item_obliterator.json5", "item_obliterator.json"};

    private ItemObliteratorConfig() {}

    public static Path file() {
        Path dir = FMLPaths.CONFIGDIR.get();
        for (String name : FILE_NAMES) {
            Path candidate = dir.resolve(name);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static boolean isRegex(String entry) {
        return entry != null && entry.startsWith(REGEX_PREFIX);
    }

    public static List<String> read() {
        Path path = file();
        if (path == null) {
            return null;
        }
        try (Reader in = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                JsonReader reader = new JsonReader(in)) {
            reader.setLenient(true);
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return null;
            }
            JsonObject object = root.getAsJsonObject();
            if (!object.has(KEY) || !object.get(KEY).isJsonArray()) {
                return List.of();
            }
            JsonArray array = object.getAsJsonArray(KEY);
            List<String> out = new ArrayList<>(array.size());
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    out.add(element.getAsString());
                }
            }
            return out;
        } catch (Throwable t) {
            createaddonorganizer.LOGGER.warn("[CAO] could not read Item Obliterator's config at {}", path, t);
            return null;
        }
    }

    public static boolean write(List<String> entries) {
        Path path = file();
        if (path == null) {
            return false;
        }
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            String updated = splice(text, entries);
            if (updated == null) {
                createaddonorganizer.LOGGER.warn("[CAO] Item Obliterator's config has no \"{}\" array to edit; "
                        + "leaving {} untouched", KEY, path);
                return false;
            }
            Files.writeString(path, updated, StandardCharsets.UTF_8);
            return true;
        } catch (IOException | RuntimeException e) {
            createaddonorganizer.LOGGER.warn("[CAO] could not write Item Obliterator's config at {}", path, e);
            return false;
        }
    }

    public static List<String> tidied(List<String> entries) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                seen.add(trimmed);
            }
        }
        return new ArrayList<>(seen);
    }

    private static String splice(String text, List<String> entries) {
        String quoted = '"' + KEY + '"';
        int key = text.indexOf(quoted);
        if (key < 0) {
            return null;
        }
        int open = text.indexOf('[', key + quoted.length());
        if (open < 0) {
            return null;
        }
        int close = closingBracket(text, open);
        if (close < 0) {
            return null;
        }
        int lineStart = text.lastIndexOf('\n', key) + 1;
        String indent = text.substring(lineStart, key);
        if (!indent.isBlank()) {
            indent = "  ";
        }
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        return text.substring(0, open) + render(entries, indent, newline) + text.substring(close + 1);
    }

    private static String render(List<String> entries, String indent, String newline) {
        if (entries.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[").append(newline);
        for (int i = 0; i < entries.size(); i++) {
            sb.append(indent).append("  ").append('"').append(escape(entries.get(i))).append('"');
            if (i < entries.size() - 1) {
                sb.append(',');
            }
            sb.append(newline);
        }
        return sb.append(indent).append(']').toString();
    }

    private static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int closingBracket(String text, int open) {
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
