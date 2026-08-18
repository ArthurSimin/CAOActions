package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;

public final class TextProbe {

    public record Hit(String key, String text, int x, int y, int width, int height, boolean tooltip) {

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private static final int STACK_LIMIT = 16;
    private static final int FRAME_LIMIT = 4096;

    private static final Object[] CONTEXT = new Object[STACK_LIMIT];
    private static int contextDepth;

    private static List<Hit> collecting = new ArrayList<>();
    private static List<Hit> lastFrame = List.of();

    private static final Map<FormattedCharSequence, String> SPLIT_KEYS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static Language indexedWith;
    private static Map<String, String> reverseIndex = Map.of();

    private static boolean capturing;
    private static boolean suspended;

    private TextProbe() {}

    public static void setCapturing(boolean value) {
        if (capturing == value) {
            return;
        }
        capturing = value;
        contextDepth = 0;
        collecting = new ArrayList<>();
        if (!value) {
            lastFrame = List.of();
        }
    }

    public static boolean capturing() {
        return capturing && !suspended;
    }

    public static void suspend(boolean value) {
        suspended = value;
    }

    public static List<Hit> lastFrame() {
        return lastFrame;
    }

    public static void endFrame() {
        if (!capturing) {
            return;
        }
        lastFrame = collecting;
        collecting = new ArrayList<>();
        contextDepth = 0;
    }

    public static void push(Object source) {
        if (!capturing() || contextDepth >= STACK_LIMIT) {
            return;
        }
        CONTEXT[contextDepth++] = source;
    }

    public static void pop() {
        if (contextDepth > 0) {
            CONTEXT[--contextDepth] = null;
        }
    }

    public static void rememberSplit(FormattedText source, List<FormattedCharSequence> lines) {
        if (!capturing() || lines == null || lines.isEmpty() || !(source instanceof Component component)) {
            return;
        }
        Set<String> keys = new LinkedHashSet<>();
        gather(component, keys);
        if (keys.size() != 1) {
            return;
        }
        String key = keys.iterator().next();
        for (FormattedCharSequence line : lines) {
            SPLIT_KEYS.put(line, key);
        }
    }

    public static void record(Font font, FormattedCharSequence text, float x, float y, Matrix4f matrix) {
        if (!capturing()) {
            return;
        }
        record(font, flatten(text), font.width(text), SPLIT_KEYS.get(text), x, y, matrix);
    }

    public static void record(Font font, String text, float x, float y, Matrix4f matrix) {
        if (!capturing() || text == null) {
            return;
        }
        record(font, text, font.width(text), null, x, y, matrix);
    }

    private static void record(Font font, String text, int width, String knownKey, float x, float y,
            Matrix4f matrix) {
        if (text.isBlank() || width <= 0 || collecting.size() >= FRAME_LIMIT) {
            return;
        }
        String key = knownKey != null ? knownKey : attribute(text);
        if (key == null) {
            return;
        }
        Vector3f min = matrix.transformPosition(new Vector3f(x, y, 0f));
        Vector3f max = matrix.transformPosition(new Vector3f(x + width, y + font.lineHeight, 0f));
        int left = Math.round(Math.min(min.x, max.x));
        int top = Math.round(Math.min(min.y, max.y));
        int right = Math.round(Math.max(min.x, max.x));
        int bottom = Math.round(Math.max(min.y, max.y));
        collecting.add(new Hit(key, text, left, top, Math.max(1, right - left), Math.max(1, bottom - top), false));
    }

    public static void recordTooltip(Object source) {
        if (!capturing()) {
            return;
        }
        Set<String> keys = new LinkedHashSet<>();
        gather(source, keys);
        for (String key : keys) {
            collecting.add(new Hit(key, Language.getInstance().getOrDefault(key), 0, 0, 0, 0, true));
        }
    }

    private static String attribute(String rendered) {
        Set<String> keys = new LinkedHashSet<>();
        for (int i = contextDepth - 1; i >= 0 && keys.isEmpty(); i--) {
            gather(CONTEXT[i], keys);
        }
        if (keys.isEmpty()) {
            return reverseIndex().get(rendered);
        }
        if (keys.size() == 1) {
            return keys.iterator().next();
        }
        String fallback = null;
        for (String key : keys) {
            String value = Language.getInstance().getOrDefault(key, key);
            if (value.equals(rendered)) {
                return key;
            }
            if (fallback == null && (value.contains(rendered) || rendered.contains(value))) {
                fallback = key;
            }
        }
        return fallback != null ? fallback : keys.iterator().next();
    }

    private static void gather(Object source, Set<String> out) {
        if (source instanceof Component component) {
            gather(component, out);
        } else if (source instanceof Iterable<?> many) {
            for (Object entry : many) {
                if (entry instanceof Component component) {
                    gather(component, out);
                }
            }
        }
    }

    public static void collectKeys(Component component, Set<String> out) {
        if (component != null) {
            gather(component, out);
        }
    }

    private static void gather(Component component, Set<String> out) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            out.add(translatable.getKey());
            LangEditor.noteFallback(translatable.getKey(), translatable.getFallback());
        }
        for (Component sibling : component.getSiblings()) {
            gather(sibling, out);
        }
    }

    private static Map<String, String> reverseIndex() {
        Language language = Language.getInstance();
        if (language == indexedWith) {
            return reverseIndex;
        }
        Map<String, String> index = new HashMap<>();
        for (Map.Entry<String, String> entry : language.getLanguageData().entrySet()) {
            String previous = index.putIfAbsent(entry.getValue(), entry.getKey());
            if (previous != null && !LangEditor.owned(previous) && LangEditor.owned(entry.getKey())) {
                index.put(entry.getValue(), entry.getKey());
            }
        }
        indexedWith = language;
        reverseIndex = index;
        return index;
    }

    private static String flatten(FormattedCharSequence text) {
        StringBuilder out = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            out.appendCodePoint(codePoint);
            return true;
        });
        return out.toString();
    }
}
