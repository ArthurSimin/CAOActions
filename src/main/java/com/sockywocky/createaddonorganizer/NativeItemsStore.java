package com.sockywocky.createaddonorganizer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

public final class NativeItemsStore {

    private static final int VERSION = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FMLPaths.CONFIGDIR.get().resolve("createaddonorganizer");
    private static final Path FILE = DIR.resolve("native_items.json");

    private record StoreData(int version, Map<String, List<String>> tabs) {}

    private static final Map<ResourceLocation, List<String>> ITEMS = new LinkedHashMap<>();

    private static boolean loaded;
    private static boolean dirty;

    private NativeItemsStore() {}

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.isRegularFile(FILE)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            StoreData data = GSON.fromJson(reader, StoreData.class);
            if (data == null || data.tabs() == null) {
                return;
            }
            for (Map.Entry<String, List<String>> entry : data.tabs().entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                if (id != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    ITEMS.put(id, List.copyOf(entry.getValue()));
                }
            }
        } catch (IOException | RuntimeException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to read native_items.json, starting empty", e);
            ITEMS.clear();
        }
    }

    public static synchronized List<String> get(ResourceLocation tabId) {
        load();
        return tabId == null ? List.of() : ITEMS.getOrDefault(tabId, List.of());
    }

    public static synchronized void put(ResourceLocation tabId, List<String> itemIds) {
        if (tabId == null || itemIds == null || itemIds.isEmpty()) {
            return;
        }
        load();
        List<String> copy = List.copyOf(itemIds);
        if (copy.equals(ITEMS.get(tabId))) {
            return;
        }
        ITEMS.put(tabId, copy);
        dirty = true;
    }

    public static synchronized void flush() {
        if (!dirty) {
            return;
        }
        dirty = false;
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<String>> entry : ITEMS.entrySet()) {
            out.put(entry.getKey().toString(), entry.getValue());
        }
        try {
            Files.createDirectories(DIR);
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(new StoreData(VERSION, out), writer);
            }
        } catch (IOException e) {
            createaddonorganizer.LOGGER.error("[CAO] failed to write native_items.json", e);
        }
    }
}
