package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ItemLibrary {

    public record Entry(String id, String modId, String name, String lowerName, ItemStack stack) {}

    private static List<Entry> all;
    private static Map<String, Entry> byId;
    private static List<String> modIds;
    private static String lastQuery = "";
    private static List<Entry> lastResult;

    private ItemLibrary() {}

    public static synchronized List<Entry> all() {
        if (all == null) {
            build();
        }
        return all;
    }

    public static synchronized Entry byId(String id) {
        if (all == null) {
            build();
        }
        return byId.get(id);
    }

    public static ItemStack stackOf(String id) {
        Entry entry = byId(id);
        return entry == null ? ItemStack.EMPTY : entry.stack();
    }

    public static synchronized List<String> modIds() {
        if (modIds == null) {
            TreeSet<String> ids = new TreeSet<>();
            for (Entry entry : all()) {
                ids.add(entry.modId());
            }
            modIds = List.copyOf(ids);
        }
        return modIds;
    }

    public static synchronized void invalidate() {
        all = null;
        byId = null;
        modIds = null;
        lastQuery = "";
        lastResult = null;
    }

    private static void build() {
        List<Entry> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (key == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            String name;
            try {
                name = stack.getHoverName().getString();
            } catch (Throwable t) {
                name = key.getPath();
            }
            out.add(new Entry(key.toString(), key.getNamespace(), name,
                    name.toLowerCase(Locale.ROOT), stack));
        }
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        all = List.copyOf(out);
        Map<String, Entry> index = new HashMap<>(out.size() * 2);
        for (Entry entry : out) {
            index.put(entry.id(), entry);
        }
        byId = index;
    }

    public static synchronized List<Entry> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            lastQuery = "";
            lastResult = all();
            return lastResult;
        }
        boolean byMod = q.startsWith("@");
        String needle = byMod ? q.substring(1).trim() : q;
        List<Entry> source = narrowableFrom(q, byMod);
        List<Entry> out = new ArrayList<>();
        for (Entry entry : source) {
            boolean hit = byMod
                    ? entry.modId().contains(needle)
                    : entry.lowerName().contains(needle) || entry.id().contains(needle);
            if (hit) {
                out.add(entry);
            }
        }
        lastQuery = q;
        lastResult = List.copyOf(out);
        return lastResult;
    }

    private static List<Entry> narrowableFrom(String q, boolean byMod) {
        if (lastResult != null && !lastQuery.isEmpty()
                && lastQuery.startsWith("@") == byMod
                && q.startsWith(lastQuery)) {
            return lastResult;
        }
        return all();
    }

    public static List<Entry> ofMod(String modId) {
        List<Entry> out = new ArrayList<>();
        for (Entry entry : all()) {
            if (entry.modId().equals(modId)) {
                out.add(entry);
            }
        }
        return out;
    }
}
