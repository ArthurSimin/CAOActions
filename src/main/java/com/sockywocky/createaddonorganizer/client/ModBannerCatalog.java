package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class ModBannerCatalog {
    public record TabEntry(ResourceLocation id, String label) {}

    public record ModEntry(String modName, List<TabEntry> tabs) {}

    private record RawTab(String id, String label) {}

    private record RawMod(String name, List<RawTab> tabs) {}

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<RawMod>>() {}.getType();
    private static final ResourceLocation RESOURCE =
            ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID, "mod_banner_catalog.json");

    private static List<ModEntry> cached;

    private ModBannerCatalog() {}

    public static List<ModEntry> entries() {
        List<ModEntry> snapshot = cached;
        if (snapshot == null) {
            snapshot = load();
            cached = snapshot;
        }
        return snapshot;
    }

    public static void invalidate() {
        cached = null;
    }

    private static List<ModEntry> load() {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(RESOURCE);
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            List<RawMod> raw = GSON.fromJson(reader, LIST_TYPE);
            if (raw == null) {
                return List.of();
            }
            List<ModEntry> out = new ArrayList<>(raw.size());
            for (RawMod mod : raw) {
                if (mod == null || mod.name() == null || mod.name().isBlank() || mod.tabs() == null) {
                    continue;
                }
                List<TabEntry> tabs = new ArrayList<>(mod.tabs().size());
                for (RawTab tab : mod.tabs()) {
                    if (tab == null || tab.id() == null) {
                        continue;
                    }
                    ResourceLocation id = ResourceLocation.tryParse(tab.id());
                    if (id == null) {
                        createaddonorganizer.LOGGER.warn("[CAO] mod banner catalog has an invalid tab id: {}", tab.id());
                        continue;
                    }
                    String label = tab.label() == null || tab.label().isBlank() ? mod.name() : tab.label();
                    tabs.add(new TabEntry(id, label));
                }
                if (!tabs.isEmpty()) {
                    out.add(new ModEntry(mod.name(), List.copyOf(tabs)));
                }
            }
            return List.copyOf(out);
        } catch (IOException | RuntimeException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to read the mod banner catalog", e);
            return List.of();
        }
    }
}
