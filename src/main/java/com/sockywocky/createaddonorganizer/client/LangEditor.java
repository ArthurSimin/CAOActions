package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.loading.FMLPaths;

public final class LangEditor {

    public enum Target { SOURCE, OVERRIDE }

    public record Result(Target target, Path file) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final java.lang.reflect.Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static final Path OVERRIDE_DIR =
            FMLPaths.CONFIGDIR.get().resolve(createaddonorganizer.MODID).resolve("lang_edits");

    private static final String[] OWNED_PREFIXES = {
            createaddonorganizer.MODID + ".",
            "key." + createaddonorganizer.MODID + ".",
            "key.categories." + createaddonorganizer.MODID,
            "itemGroup." + createaddonorganizer.MODID + ".",
    };

    private static final Map<String, String> OVERRIDES = new LinkedHashMap<>();

    private static final Map<String, String> FALLBACKS = new HashMap<>();

    private static String loadedLanguage;
    private static Language base;

    private LangEditor() {}

    public static void ensureInstalled() {
        String code = languageCode();
        if (!code.equals(loadedLanguage)) {
            loadedLanguage = code;
            OVERRIDES.clear();
            OVERRIDES.putAll(readOverrides(code));
            base = null;
        }
        Language current = Language.getInstance();
        if (current instanceof Overridden) {
            return;
        }
        base = current;
        if (!OVERRIDES.isEmpty()) {
            reinject();
        }
    }

    public static String languageCode() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null || mc.getLanguageManager() == null ? Language.DEFAULT : mc.getLanguageManager().getSelected();
    }

    public static boolean isEdited(String key) {
        return OVERRIDES.containsKey(key);
    }

    public static String shipped(String key) {
        return delegate().getOrDefault(key, key);
    }

    public static String current(String key) {
        String edited = OVERRIDES.get(key);
        if (edited != null) {
            return edited;
        }
        if (delegate().has(key)) {
            return delegate().getOrDefault(key, key);
        }
        String fallback = FALLBACKS.get(key);
        return fallback != null ? fallback : key;
    }

    public static void noteFallback(String key, String fallback) {
        if (fallback != null && !fallback.isEmpty()) {
            FALLBACKS.put(key, fallback);
        }
    }

    public static boolean owned(String key) {
        for (String prefix : OWNED_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static Path sourceFile() {
        Path projectRoot = Path.of("").toAbsolutePath().getParent();
        if (projectRoot == null || !Files.isDirectory(projectRoot.resolve("src/main/resources"))) {
            return null;
        }
        Path file = projectRoot.resolve("src/main/resources/assets/" + createaddonorganizer.MODID
                + "/lang/" + languageCode() + ".json");
        return Files.isRegularFile(file) ? file : null;
    }

    public static Result set(String key, String value) throws IOException {
        if (value.equals(shipped(key))) {
            return revert(key);
        }
        OVERRIDES.put(key, value);
        Path source = owned(key) ? sourceFile() : null;
        if (source != null) {
            LangJson.put(source, key, value);
        }
        writeOverrides();
        reinject();
        return new Result(source != null ? Target.SOURCE : Target.OVERRIDE,
                source != null ? source : overrideFile());
    }

    public static Result revert(String key) throws IOException {
        OVERRIDES.remove(key);
        Path source = owned(key) ? sourceFile() : null;
        if (source != null) {
            if (delegate().has(key)) {
                LangJson.put(source, key, shipped(key));
            } else {
                LangJson.remove(source, key);
            }
        }
        writeOverrides();
        reinject();
        return new Result(source != null ? Target.SOURCE : Target.OVERRIDE,
                source != null ? source : overrideFile());
    }

    private static Language delegate() {
        if (base == null) {
            Language current = Language.getInstance();
            base = current instanceof Overridden overridden ? overridden.delegate : current;
        }
        return base;
    }

    private static void reinject() {
        Language.inject(new Overridden(delegate(), Map.copyOf(OVERRIDES)));
    }

    private static Path overrideFile() {
        return OVERRIDE_DIR.resolve(languageCode() + ".json");
    }

    private static Map<String, String> readOverrides(String code) {
        Path file = OVERRIDE_DIR.resolve(code + ".json");
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, String> data = GSON.fromJson(reader, MAP_TYPE);
            return data == null ? Map.of() : data;
        } catch (IOException | RuntimeException e) {
            createaddonorganizer.LOGGER.warn("[CAO] could not read text edits for {}", code, e);
            return Map.of();
        }
    }

    private static void writeOverrides() throws IOException {
        Path file = overrideFile();
        if (OVERRIDES.isEmpty()) {
            Files.deleteIfExists(file);
            return;
        }
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(OVERRIDES, writer);
        }
    }

    private static final class Overridden extends Language {

        private final Language delegate;
        private final Map<String, String> edits;

        private Overridden(Language delegate, Map<String, String> edits) {
            this.delegate = delegate;
            this.edits = edits;
        }

        @Override
        public String getOrDefault(String key, String defaultValue) {
            String edited = edits.get(key);
            return edited != null ? edited : delegate.getOrDefault(key, defaultValue);
        }

        @Override
        public boolean has(String id) {
            return edits.containsKey(id) || delegate.has(id);
        }

        @Override
        public boolean isDefaultRightToLeft() {
            return delegate.isDefaultRightToLeft();
        }

        @Override
        public FormattedCharSequence getVisualOrder(FormattedText text) {
            return delegate.getVisualOrder(text);
        }

        @Override
        public Map<String, String> getLanguageData() {
            Map<String, String> merged = new LinkedHashMap<>(delegate.getLanguageData());
            merged.putAll(edits);
            return merged;
        }

        @Override
        public Component getComponent(String key) {
            return edits.containsKey(key) ? null : delegate.getComponent(key);
        }
    }
}
