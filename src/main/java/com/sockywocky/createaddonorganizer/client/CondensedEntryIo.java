package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;

import com.sockywocky.createaddonorganizer.TabLayout;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

public final class CondensedEntryIo {
    private static final String DIR = "condensed_entries";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String BASE_ITEM = "base_item";
    private static final String ITEMS = "items";
    private static final String ITEM_TAG = "item_tag";
    private static final String TITLE = "title";
    private static final String USE_TAG = "USE_TAG";

    private CondensedEntryIo() {}

    public record Imported(String title, String icon, List<String> members, ResourceLocation preferredTab) {}

    public static List<Imported> scanResources() {
        List<Imported> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getResourceManager() == null) {
            return out;
        }
        Map<ResourceLocation, Resource> files;
        try {
            files = mc.getResourceManager().listResources(DIR, path -> path.getPath().endsWith(".json"));
        } catch (Throwable t) {
            createaddonorganizer.LOGGER.warn("[CAO] could not list condensed_entries resources", t);
            return out;
        }
        for (Map.Entry<ResourceLocation, Resource> file : files.entrySet()) {
            try (Reader reader = file.getValue().openAsReader()) {
                JsonElement root = GSON.fromJson(reader, JsonElement.class);
                if (root != null && root.isJsonObject()) {
                    collect(root.getAsJsonObject(), null, out);
                }
            } catch (IOException | RuntimeException e) {
                createaddonorganizer.LOGGER.warn("[CAO] skipping unreadable condensed entry file {}",
                        file.getKey(), e);
            }
        }
        return out;
    }

    private static void collect(JsonObject object, ResourceLocation tab, List<Imported> out) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement value = entry.getValue();
            if (!value.isJsonObject()) {
                continue;
            }
            JsonObject child = value.getAsJsonObject();
            if (child.has(BASE_ITEM)) {
                Imported parsed = parse(entry.getKey(), child, tab);
                if (parsed != null) {
                    out.add(parsed);
                }
                continue;
            }
            ResourceLocation nested = ResourceLocation.tryParse(entry.getKey());
            collect(child, nested != null ? nested : tab, out);
        }
    }

    private static Imported parse(String id, JsonObject object, ResourceLocation tab) {
        String base = asString(object.get(BASE_ITEM));
        List<String> members = new ArrayList<>();
        if (object.has(ITEMS) && object.get(ITEMS).isJsonArray()) {
            for (JsonElement element : object.getAsJsonArray(ITEMS)) {
                String item = asString(element);
                if (item != null && exists(item)) {
                    members.add(item);
                }
            }
        } else if (object.has(ITEM_TAG)) {
            members.addAll(itemsOfTag(asString(object.get(ITEM_TAG))));
        }
        if (members.size() < 2) {
            return null;
        }
        String title = asString(object.get(TITLE));
        if (title == null || USE_TAG.equals(title)) {
            title = readableTitle(id, object);
        }
        return new Imported(title, base != null && exists(base) ? base : members.get(0), members, tab);
    }

    private static String readableTitle(String id, JsonObject object) {
        String tag = asString(object.get(ITEM_TAG));
        String source = tag != null ? tag : id;
        int colon = source.indexOf(':');
        String path = colon < 0 ? source : source.substring(colon + 1);
        int slash = path.lastIndexOf('/');
        String leaf = slash < 0 ? path : path.substring(slash + 1);
        String spaced = leaf.replace('_', ' ').trim();
        if (spaced.isEmpty()) {
            return "Group";
        }
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static List<String> itemsOfTag(String tagId) {
        ResourceLocation parsed = tagId == null ? null : ResourceLocation.tryParse(tagId);
        if (parsed == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        TagKey<Item> key = TagKey.create(BuiltInRegistries.ITEM.key(), parsed);
        BuiltInRegistries.ITEM.getTagOrEmpty(key).forEach(holder -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(holder.value());
            if (id != null) {
                out.add(id.toString());
            }
        });
        return out;
    }

    private static boolean exists(String itemId) {
        ResourceLocation parsed = ResourceLocation.tryParse(itemId);
        return parsed != null && BuiltInRegistries.ITEM.get(parsed) != Items.AIR;
    }

    private static String asString(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        try {
            return element.getAsString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Result importInto(TabLayout layout, List<Imported> entries) {
        if (layout == null) {
            return new Result(layout, 0);
        }
        Set<String> present = new LinkedHashSet<>();
        Set<String> taken = new LinkedHashSet<>();
        for (TabLayout.Entry entry : layout.safeEntries()) {
            if (entry.isItem()) {
                present.add(entry.item());
                if (entry.groupId() != null) {
                    taken.add(entry.item());
                }
            }
        }
        TabLayout updated = layout;
        int added = 0;
        int index = 0;
        for (Imported entry : entries) {
            List<String> members = new ArrayList<>();
            for (String member : entry.members()) {
                if (present.contains(member) && !taken.contains(member)) {
                    members.add(member);
                }
            }
            if (members.size() < 2) {
                continue;
            }
            String icon = members.contains(entry.icon()) ? entry.icon() : members.get(0);
            TabLayout.ItemGroup group =
                    new TabLayout.ItemGroup("cc" + index++, entry.title(), icon);
            TabLayout next = updated.withImportedGroup(group, members);
            if (next != updated) {
                updated = next;
                taken.addAll(members);
                added++;
            }
        }
        return new Result(updated, added);
    }

    public record Result(TabLayout layout, int count) {}

    public static Path export(TabLayout layout) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject byTab = new JsonObject();
        for (TabLayout.ItemGroup group : layout.safeItemGroups()) {
            List<String> members = layout.membersOf(group.id());
            if (members.size() < 2) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty(BASE_ITEM, layout.iconItemOf(group.id()));
            entry.addProperty(TITLE, group.displayTitle());
            com.google.gson.JsonArray items = new com.google.gson.JsonArray();
            for (String member : members) {
                items.add(member);
            }
            entry.add(ITEMS, items);
            byTab.add(createaddonorganizer.MODID + ":" + group.id(), entry);
        }
        if (byTab.size() == 0) {
            return null;
        }
        Map<String, JsonObject> indexed = new LinkedHashMap<>();
        indexed.put("0", byTab);
        JsonObject tabHolder = new JsonObject();
        indexed.forEach(tabHolder::add);
        root.add(layout.id().toString(), tabHolder);

        Path dir = FMLPaths.CONFIGDIR.get().resolve(createaddonorganizer.MODID).resolve("condensed_entries");
        Files.createDirectories(dir);
        Path file = dir.resolve(layout.id().getNamespace() + "_" + layout.id().getPath() + ".json");
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
        return file;
    }
}
