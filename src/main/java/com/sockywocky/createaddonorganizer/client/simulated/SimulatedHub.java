package com.sockywocky.createaddonorganizer.client.simulated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.client.BannerTextures;
import com.sockywocky.createaddonorganizer.client.CaoSection;
import com.sockywocky.createaddonorganizer.client.SafeIcon;
import com.sockywocky.createaddonorganizer.mixin.simulated.SimResourceManagerAccessor;

import dev.simulated_team.simulated.client.sections.SimulatedSection;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;
import foundry.veil.api.client.color.Color;
import foundry.veil.api.client.color.Colorc;

import net.mcexpanded.fancytabsections.creativetab.ConglomerateOfItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SimulatedHub {
    private static final Set<ResourceLocation> OWNED_SECTIONS = new LinkedHashSet<>();
    private static final Map<ResourceLocation, List<ResourceLocation>> OWNED_ITEM_IDS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Component> OWNED_TITLES = new LinkedHashMap<>();

    private static final Set<ResourceLocation> ADOPTED = new LinkedHashSet<>();
    private static final Map<ResourceLocation, Object> ADOPTED_ORIGINALS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Object> INJECTED = new LinkedHashMap<>();

    private static final ResourceLocation SIMULATED_SELF = ResourceLocation.fromNamespaceAndPath("simulated", "simulated");
    private static final ResourceLocation AERONAUTICS_SELF = ResourceLocation.fromNamespaceAndPath("aeronautics", "aeronautics");
    private static final ResourceLocation OFFROAD_SELF = ResourceLocation.fromNamespaceAndPath("offroad", "offroad");

    private static final ResourceLocation SIMULATED_ICON_ITEM = ResourceLocation.fromNamespaceAndPath("simulated", "physics_assembler");
    private static final ResourceLocation AERONAUTICS_ICON_ITEM = ResourceLocation.fromNamespaceAndPath("aeronautics", "white_envelope");
    private static final ResourceLocation OFFROAD_ICON_ITEM = ResourceLocation.fromNamespaceAndPath("offroad", "tire");

    private static int stateVersion = 0;

    private SimulatedHub() {}

    public static int stateVersion() {
        return stateVersion;
    }

    public static boolean owns(ResourceLocation id) {
        return OWNED_SECTIONS.contains(id);
    }

    public static ResourceLocation ownerIfAny(ResourceLocation id) {
        return OWNED_SECTIONS.contains(id) ? SimulatedSupport.MAIN_TAB : null;
    }

    public record NativeSection(ResourceLocation id, Component title, int textColor, int secondaryTextColor,
            int backgroundColor, ResourceLocation sprite, int priority, boolean animateOnHover) {}

    public static List<NativeSection> nativeSections() {
        List<NativeSection> out = new ArrayList<>();
        for (Object value : accessor().getSortedValues()) {
            if (!(value instanceof SimulatedSection section)) {
                continue;
            }
            ResourceLocation id = accessor().getToId().get(value);
            if (id == null || OWNED_SECTIONS.contains(id)) {
                continue;
            }
            out.add(describe(id, section));
        }
        return out;
    }

    public static NativeSection houseStyleFor(String namespace) {
        NativeSection best = null;
        for (Map.Entry<ResourceLocation, Object> entry : ADOPTED_ORIGINALS.entrySet()) {
            if (entry.getKey().getNamespace().equals(namespace)
                    && entry.getValue() instanceof SimulatedSection section) {
                best = better(best, describe(entry.getKey(), section));
            }
        }
        for (Object value : accessor().getSortedValues()) {
            if (!(value instanceof SimulatedSection section) || INJECTED.containsValue(value)) {
                continue;
            }
            ResourceLocation id = accessor().getToId().get(value);
            if (id != null && id.getNamespace().equals(namespace) && !ADOPTED_ORIGINALS.containsKey(id)) {
                best = better(best, describe(id, section));
            }
        }
        return best;
    }

    private static NativeSection better(NativeSection current, NativeSection candidate) {
        if (current == null) {
            return candidate;
        }
        return candidate.priority() < current.priority() ? candidate : current;
    }

    private static NativeSection describe(ResourceLocation id, SimulatedSection section) {
        Colorc light = section.title().color();
        Colorc dark = section.title().secondaryColor().orElse(light.darken(0.2F, new Color()));
        return new NativeSection(id, section.title().text(), light.argb(), dark.argb(),
                section.title().background().argb(), section.sprite(), section.priority(), section.animateOnHover());
    }

    public static List<ResourceLocation> nativeSprites() {
        List<ResourceLocation> out = new ArrayList<>();
        for (Object value : accessor().getSortedValues()) {
            ResourceLocation id = accessor().getToId().get(value);
            if (id != null && OWNED_SECTIONS.contains(id)) {
                continue;
            }
            if (value instanceof SimulatedSection section && !out.contains(section.sprite())) {
                out.add(section.sprite());
            }
        }
        for (Object original : ADOPTED_ORIGINALS.values()) {
            if (original instanceof SimulatedSection section && !out.contains(section.sprite())) {
                out.add(section.sprite());
            }
        }
        return out;
    }

    public static boolean isAdopted(ResourceLocation id) {
        return ADOPTED.contains(id);
    }

    public static Set<ResourceLocation> adoptedIds() {
        return Set.copyOf(ADOPTED);
    }

    public static List<ResourceLocation> adoptedInDrawOrder() {
        List<ResourceLocation> out = new ArrayList<>();
        for (Object value : accessor().getSortedValues()) {
            ResourceLocation id = accessor().getToId().get(value);
            if (id != null && ADOPTED.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }

    public static Component adoptedTitle(ResourceLocation id) {
        return OWNED_TITLES.get(id);
    }

    public static Component originalTitle(ResourceLocation id) {
        return ADOPTED_ORIGINALS.get(id) instanceof SimulatedSection section ? section.title().text() : null;
    }

    public static void adopt(ResourceLocation id, Component title) {
        Object original = accessor().getEntries().get(id);
        if (original == null || OWNED_SECTIONS.contains(id)) {
            return;
        }
        ADOPTED_ORIGINALS.put(id, original);
        ADOPTED.add(id);
        OWNED_TITLES.put(id, title);
        OWNED_SECTIONS.add(id);
        reinject(id);
        rebuildAfterMutation();
    }

    public static void releaseAdopted(ResourceLocation id) {
        Object original = ADOPTED_ORIGINALS.remove(id);
        ADOPTED.remove(id);
        OWNED_SECTIONS.remove(id);
        OWNED_TITLES.remove(id);
        INJECTED.remove(id);
        if (original != null) {
            accessor().getEntries().put(id, original);
        }
        rebuildAfterMutation();
    }

    public static void inject(ResourceLocation id, Component title) {
        OWNED_TITLES.put(id, title);
        OWNED_SECTIONS.add(id);
        reinject(id);
        rebuildAfterMutation();
    }

    public static void foldItems(ResourceLocation id, Iterable<ItemStack> items) {
        List<ResourceLocation> previous = OWNED_ITEM_IDS.get(id);
        if (previous != null) {
            SimulatedRegistrate.TAB_ITEMS.removeIf(supplier -> previous.contains(BuiltInRegistries.ITEM.getKey(supplier.get())));
            previous.forEach(SimulatedRegistrate.ITEM_TO_SECTION::remove);
        }
        List<ResourceLocation> itemIds = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemIds.contains(itemId)) {
                continue;
            }
            itemIds.add(itemId);
            SimulatedRegistrate.ITEM_TO_SECTION.put(itemId, id);
            SimulatedRegistrate.TAB_ITEMS.add(() -> BuiltInRegistries.ITEM.get(itemId));
        }
        OWNED_ITEM_IDS.put(id, itemIds);
    }

    public static void remove(ResourceLocation id) {
        if (ADOPTED.contains(id)) {
            releaseAdopted(id);
            return;
        }
        List<ResourceLocation> itemIds = OWNED_ITEM_IDS.remove(id);
        if (itemIds != null) {
            SimulatedRegistrate.TAB_ITEMS.removeIf(supplier -> itemIds.contains(BuiltInRegistries.ITEM.getKey(supplier.get())));
            itemIds.forEach(SimulatedRegistrate.ITEM_TO_SECTION::remove);
        }
        OWNED_SECTIONS.remove(id);
        OWNED_TITLES.remove(id);
        INJECTED.remove(id);
        accessor().getEntries().remove(id);
        rebuildAfterMutation();
    }

    public static void retractAll() {
        for (ResourceLocation id : new ArrayList<>(OWNED_SECTIONS)) {
            if (!ADOPTED.contains(id)) {
                remove(id);
            }
        }
    }

    public static void reorder(List<ResourceLocation> orderedIds) {
        List<ResourceLocation> renumbered = new ArrayList<>();
        for (ResourceLocation id : orderedIds) {
            if (OWNED_SECTIONS.contains(id)) {
                renumbered.add(id);
            }
        }
        for (ResourceLocation id : ADOPTED) {
            if (!renumbered.contains(id) && ADOPTED_ORIGINALS.get(id) instanceof SimulatedSection original) {
                reinjectWithPriority(id, original.priority());
            }
        }
        int base = maxPriorityOutside(renumbered) + 1;
        int index = 0;
        for (ResourceLocation id : renumbered) {
            reinjectWithPriority(id, base + index);
            index++;
        }
        rebuildAfterMutation();
    }

    public static void applyTitle(ResourceLocation id, Component title) {
        OWNED_TITLES.put(id, title);
        reinject(id);
        rebuildAfterMutation();
    }

    public static void verifyInjected() {
        boolean changed = false;
        for (ResourceLocation id : OWNED_SECTIONS) {
            Object current = accessor().getEntries().get(id);
            if (current != null && current == INJECTED.get(id)) {
                continue;
            }
            if (current != null && ADOPTED.contains(id)) {
                ADOPTED_ORIGINALS.put(id, current);
            }
            reinject(id);
            changed = true;
        }
        if (changed) {
            rebuildAfterMutation();
        }
    }

    public static void renderOwned(GuiGraphics g, Font font, ResourceLocation id, int left, int top, int sectionRow) {
        CaoSection section = caoSectionFor(id);
        g.pose().pushPose();
        g.pose().translate(-left, -top, 0);
        section.render(g, font, left, top + sectionRow * 18);
        g.pose().popPose();
    }

    private static CaoSection caoSectionFor(ResourceLocation id) {
        Component title = OWNED_TITLES.getOrDefault(id, Component.literal(id.toString()));
        String bannerRef = Config.bannerRefFor(id);
        ResourceLocation texture = bannerRef != null ? BannerTextures.resolve(bannerRef) : null;
        return new CaoSection(id, title, Config.bannerColorFor(id), texture, Config.textColorFor(id), ConglomerateOfItems.create());
    }

    private static void reinject(ResourceLocation id) {
        Integer existing = currentPriority(id);
        if (existing == null && ADOPTED_ORIGINALS.get(id) instanceof SimulatedSection original) {
            existing = original.priority();
        }
        reinjectWithPriority(id, existing != null ? existing : maxPriorityOutside(OWNED_SECTIONS) + 1);
    }

    private static Integer currentPriority(ResourceLocation id) {
        Object current = accessor().getEntries().get(id);
        return current instanceof SimulatedSection s ? s.priority() : null;
    }

    private static void reinjectWithPriority(ResourceLocation id, int priority) {
        Component title = OWNED_TITLES.getOrDefault(id, Component.literal(id.toString()));
        JsonObject titleJson = new JsonObject();
        titleJson.add("text", ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, title).getOrThrow());

        JsonObject json = new JsonObject();
        json.addProperty("priority", priority);

        if (ADOPTED_ORIGINALS.get(id) instanceof SimulatedSection original) {
            json.addProperty("sprite", original.sprite().toString());
            json.addProperty("only_animate_on_hover", original.animateOnHover());
            titleJson.addProperty("background", hex(original.title().background()));
            titleJson.addProperty("color", hex(original.title().color()));
            original.title().secondaryColor()
                    .ifPresent(secondary -> titleJson.addProperty("secondary_color", hex(secondary)));
        }

        json.add("title", titleJson);

        SimulatedSection section = SimulatedSection.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        accessor().getEntries().put(id, section);
        INJECTED.put(id, section);
    }

    private static String hex(Colorc color) {
        return String.format("#%08x", color.argb());
    }

    private static int maxPriorityOutside(Collection<ResourceLocation> renumbered) {
        int max = 0;
        for (Map.Entry<ResourceLocation, Object> entry : accessor().getEntries().entrySet()) {
            if (renumbered.contains(entry.getKey())) {
                continue;
            }
            if (entry.getValue() instanceof SimulatedSection s) {
                max = Math.max(max, s.priority());
            }
        }
        return max;
    }

    private static void rebuildAfterMutation() {
        Map<ResourceLocation, Object> entries = accessor().getEntries();
        Map<Object, ResourceLocation> toId = accessor().getToId();
        toId.clear();
        entries.forEach((key, value) -> toId.put(value, key));

        List<Object> values = new ArrayList<>(entries.values());
        values.sort((a, b) -> ((SimulatedSection) a).compareTo((SimulatedSection) b));

        List<Object> sortedValues = accessor().getSortedValues();
        sortedValues.clear();
        sortedValues.addAll(values);

        stateVersion++;
    }

    public record IndexEntry(ResourceLocation id, Component title, boolean owned) {}

    public static List<IndexEntry> allSectionsInOrder() {
        List<IndexEntry> out = new ArrayList<>();
        for (Object value : accessor().getSortedValues()) {
            if (!(value instanceof SimulatedSection section)) {
                continue;
            }
            ResourceLocation id = accessor().getToId().get(value);
            if (id == null) {
                continue;
            }
            boolean owned = OWNED_SECTIONS.contains(id);
            Component title = owned ? OWNED_TITLES.getOrDefault(id, Component.literal(id.toString())) : section.title().text();
            out.add(new IndexEntry(id, title, owned));
        }
        return out;
    }

    public static Integer rowOf(ResourceLocation id) {
        return SimulatedCreativeTab.SECTION_Y_VALUES.containsKey(id) ? SimulatedCreativeTab.SECTION_Y_VALUES.getInt(id) : null;
    }

    public static Map<ResourceLocation, List<ItemStack>> itemsBySection() {
        Map<ResourceLocation, List<ItemStack>> out = new LinkedHashMap<>();
        for (var supplier : SimulatedRegistrate.TAB_ITEMS) {
            Item item = supplier.get();
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            ResourceLocation sectionId = SimulatedRegistrate.ITEM_TO_SECTION.get(itemId);
            if (sectionId == null) {
                continue;
            }
            out.computeIfAbsent(sectionId, key -> new ArrayList<>()).add(new ItemStack(item));
        }
        return out;
    }

    public static ItemStack iconFor(ResourceLocation sectionId, boolean owned) {
        if (owned && !ADOPTED.contains(sectionId)) {
            return ownedIcon(sectionId);
        }
        if (SIMULATED_SELF.equals(sectionId)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(SIMULATED_ICON_ITEM));
        }
        if (AERONAUTICS_SELF.equals(sectionId)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(AERONAUTICS_ICON_ITEM));
        }
        if (OFFROAD_SELF.equals(sectionId)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(OFFROAD_ICON_ITEM));
        }
        for (var supplier : SimulatedRegistrate.TAB_ITEMS) {
            Item item = supplier.get();
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (sectionId.equals(SimulatedRegistrate.ITEM_TO_SECTION.get(itemId))) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack ownedIcon(ResourceLocation sectionId) {
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(sectionId);
        ItemStack tabIcon = SafeIcon.of(tab);
        if (!tabIcon.isEmpty()) {
            return tabIcon;
        }
        List<ResourceLocation> itemIds = OWNED_ITEM_IDS.get(sectionId);
        if (itemIds != null && !itemIds.isEmpty()) {
            return new ItemStack(BuiltInRegistries.ITEM.get(itemIds.get(0)));
        }
        return ItemStack.EMPTY;
    }

    private static SimResourceManagerAccessor accessor() {
        return (SimResourceManagerAccessor) SimResourceManagers.SIMULATED_SECTION;
    }
}

