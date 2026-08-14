package com.sockywocky.createaddonorganizer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

public final class TabLayoutStore {

    public static final int SPARE_SLOTS = 8;
    private static final int VERSION = 2;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FMLPaths.CONFIGDIR.get().resolve("createaddonorganizer");
    private static final Path FILE = DIR.resolve("tab_layouts.json");
    private static final Path LEGACY_FILE = DIR.resolve("custom_tabs.json");

    private record StoreData(int version, List<TabLayout> tabs) {}

    private record LegacyTab(int slot, String name, String icon, int nextSectionId,
            List<TabLayout.Entry> entries) {}

    private record LegacyData(int version, List<LegacyTab> tabs) {}

    private static final Map<ResourceLocation, TabLayout> LAYOUTS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ItemStack> ICON_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Component> NAME_CACHE = new ConcurrentHashMap<>();

    private static boolean loaded;
    private static boolean dirty;
    private static int registeredSlots;
    private static volatile boolean anyOverrides;

    private TabLayoutStore() {}

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        LAYOUTS.clear();
        invalidateCaches();
        boolean migrated = false;
        if (Files.isRegularFile(FILE)) {
            readCurrent();
        } else if (Files.isRegularFile(LEGACY_FILE)) {
            migrated = readLegacy();
        }
        int highest = -1;
        for (ResourceLocation id : LAYOUTS.keySet()) {
            highest = Math.max(highest, TabLayout.slotOf(id));
        }
        registeredSlots = highest + 1 + SPARE_SLOTS;
        refreshOverrideFlag();
        if (migrated) {
            save();
        }
    }

    private static void readCurrent() {
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            StoreData data = GSON.fromJson(reader, StoreData.class);
            if (data == null || data.tabs() == null) {
                return;
            }
            for (TabLayout layout : data.tabs()) {
                if (layout == null || layout.tab() == null) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(layout.tab());
                if (id != null) {
                    LAYOUTS.put(id, layout);
                }
            }
        } catch (IOException | RuntimeException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to read tab_layouts.json, starting empty", e);
            LAYOUTS.clear();
        }
    }

    private static boolean readLegacy() {
        try (Reader reader = Files.newBufferedReader(LEGACY_FILE, StandardCharsets.UTF_8)) {
            LegacyData data = GSON.fromJson(reader, LegacyData.class);
            if (data == null || data.tabs() == null) {
                return false;
            }
            for (LegacyTab old : data.tabs()) {
                if (old == null || old.slot() < 0) {
                    continue;
                }
                ResourceLocation id = TabLayout.idForSlot(old.slot());
                LAYOUTS.put(id, new TabLayout(id.toString(), old.name(), old.icon(), old.nextSectionId(),
                        old.entries() == null ? List.of() : List.copyOf(old.entries()), List.of(), true,
                        List.of(), 0));
            }
            createaddonorganizer.LOGGER.info("[CAO] migrated {} custom tab(s) from custom_tabs.json to "
                    + "tab_layouts.json", LAYOUTS.size());
            return true;
        } catch (IOException | RuntimeException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to migrate custom_tabs.json, starting empty", e);
            LAYOUTS.clear();
            return false;
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(DIR);
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(new StoreData(VERSION, new ArrayList<>(LAYOUTS.values())), writer);
            }
        } catch (IOException e) {
            createaddonorganizer.LOGGER.error("[CAO] failed to write tab_layouts.json", e);
        }
    }

    public static synchronized List<TabLayout> all() {
        return List.copyOf(LAYOUTS.values());
    }

    public static synchronized List<TabLayout> customTabs() {
        List<TabLayout> out = new ArrayList<>();
        for (TabLayout layout : LAYOUTS.values()) {
            if (layout.isCustom()) {
                out.add(layout);
            }
        }
        return out;
    }

    public static synchronized TabLayout byId(ResourceLocation id) {
        return id == null ? null : LAYOUTS.get(id);
    }

    public static TabLayout bySlot(int slot) {
        return byId(TabLayout.idForSlot(slot));
    }

    public static boolean isCustomTab(ResourceLocation id) {
        TabLayout layout = byId(id);
        return layout != null && layout.isCustom();
    }

    public static boolean hasLayout(ResourceLocation id) {
        return byId(id) != null;
    }

    public static synchronized int registeredSlotCount() {
        return registeredSlots;
    }

    public static synchronized int usedSlotCount() {
        int used = 0;
        for (TabLayout layout : LAYOUTS.values()) {
            if (layout.isCustom()) {
                used++;
            }
        }
        return used;
    }

    public static synchronized int freeSlotCount() {
        return Math.max(0, registeredSlots - usedSlotCount());
    }

    public static synchronized TabLayout createCustom(String name, String icon) {
        int slot = nextFreeSlot();
        if (slot < 0) {
            return null;
        }
        ResourceLocation id = TabLayout.idForSlot(slot);
        TabLayout layout = TabLayout.empty(id, name, icon);
        LAYOUTS.put(id, layout);
        invalidate(id);
        save();
        return layout;
    }

    public static synchronized void put(TabLayout layout) {
        if (layout == null) {
            return;
        }
        LAYOUTS.put(layout.id(), layout);
        invalidate(layout.id());
        save();
    }

    public static synchronized void putQuiet(TabLayout layout) {
        if (layout == null) {
            return;
        }
        LAYOUTS.put(layout.id(), layout);
        invalidate(layout.id());
        dirty = true;
    }

    public static synchronized void flush() {
        if (dirty) {
            dirty = false;
            save();
        }
    }

    public static synchronized TabLayout delete(ResourceLocation id) {
        TabLayout removed = LAYOUTS.remove(id);
        if (removed != null) {
            invalidate(id);
            save();
        }
        return removed;
    }

    public static synchronized void resetAll() {
        if (LAYOUTS.isEmpty()) {
            return;
        }
        for (ResourceLocation id : new ArrayList<>(LAYOUTS.keySet())) {
            invalidate(id);
        }
        LAYOUTS.clear();
        save();
    }

    public static boolean hasOverrides() {
        return anyOverrides;
    }

    public static Component nameOverride(ResourceLocation id) {
        if (id == null || !anyOverrides) {
            return null;
        }
        Component cached = NAME_CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        TabLayout layout = byId(id);
        if (layout == null || layout.isCustom()) {
            return null;
        }
        String name = layout.nameOverride();
        if (name == null) {
            return null;
        }
        Component built = Component.literal(name);
        NAME_CACHE.put(id, built);
        return built;
    }

    public static ItemStack iconOverride(ResourceLocation id) {
        if (id == null || !anyOverrides) {
            return null;
        }
        ItemStack cached = ICON_CACHE.get(id);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }
        TabLayout layout = byId(id);
        if (layout == null || layout.isCustom() || layout.iconOverride() == null) {
            return null;
        }
        ItemStack stack = layout.iconStack();
        ICON_CACHE.put(id, stack);
        return stack.isEmpty() ? null : stack;
    }

    private static void invalidate(ResourceLocation id) {
        ICON_CACHE.remove(id);
        NAME_CACHE.remove(id);
        refreshOverrideFlag();
    }

    private static void refreshOverrideFlag() {
        for (TabLayout layout : LAYOUTS.values()) {
            if (!layout.isCustom()) {
                anyOverrides = true;
                return;
            }
        }
        anyOverrides = false;
    }

    private static void invalidateCaches() {
        ICON_CACHE.clear();
        NAME_CACHE.clear();
    }

    private static int nextFreeSlot() {
        for (int i = 0; i < registeredSlots; i++) {
            if (!LAYOUTS.containsKey(TabLayout.idForSlot(i))) {
                return i;
            }
        }
        return -1;
    }
}

