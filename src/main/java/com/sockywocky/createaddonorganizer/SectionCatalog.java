package com.sockywocky.createaddonorganizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sockywocky.createaddonorganizer.client.CaoSection;
import com.sockywocky.createaddonorganizer.mixin.CreativeModeTabDisplayNameAccessor;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedHub;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;

import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

public final class SectionCatalog {
    private SectionCatalog() {}

    public record Entry(ResourceLocation id, Component name, boolean parent, boolean readOnly,
            Integer nativeTextColor, Integer nativeSecondaryTextColor, boolean tabOwned) {
        public Entry(ResourceLocation id, Component name, boolean parent) {
            this(id, name, parent, false, null, null, false);
        }
    }

    public static Set<ResourceLocation> knownHubs() {
        Set<ResourceLocation> hubs = new HashSet<>();
        hubs.add(createaddonorganizer.CREATE_BASE);
        hubs.addAll(createaddonorganizer.MANAGED_PARENTS);
        hubs.addAll(Config.allRouteTargets());
        hubs.addAll(Config.extraMainSections());
        hubs.addAll(AddonGroups.hubs());
        if (SimulatedSupport.isLoaded()) {
            hubs.add(SimulatedSupport.MAIN_TAB);
        }
        hubs.removeIf(Config::isForceExcluded);
        hubs.removeIf(id -> !SimulatedSupport.isMainTab(id) && !BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id));
        return hubs;
    }

    public static boolean isModDrawn(ResourceLocation id) {
        return SimulatedSupport.isLoaded() && SimulatedHub.isAdopted(id);
    }

    public static List<Entry> colorables() {
        Map<ResourceLocation, Component> names = new HashMap<>();
        Map<ResourceLocation, List<ResourceLocation>> addonsByParent = new LinkedHashMap<>();
        Set<ResourceLocation> hubCandidates = knownHubs();
        for (ResourceLocation parent : hubCandidates) {
            addonsByParent.putIfAbsent(parent, new ArrayList<>());
        }

        for (Map.Entry<ResourceKey<CreativeModeTab>, CreativeModeTab> e : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            ResourceLocation id = e.getKey().location();
            String nameOverride = Config.sectionNameOverride(id);
            names.put(id, nameOverride != null && !nameOverride.isBlank()
                    ? Component.literal(nameOverride) : e.getValue().getDisplayName());
            if (AddonDetection.isOffered(id)) {
                ResourceLocation parent = Config.parentFor(id);
                if (parent != null) {
                    addonsByParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(id);
                }
            }
        }

        Set<ResourceLocation> managed = addonsByParent.keySet();

        List<Entry> out = new ArrayList<>();
        for (ResourceLocation parent : orderedParents(managed)) {
            List<Row> rows = sectionOrder(parent, addonsByParent.getOrDefault(parent, List.of()), names);
            boolean firstRow = true;
            for (Row row : rows) {
                out.add(new Entry(row.id(), row.name(), row.id().equals(parent), false, null, null, row.tabOwned()));
                if (firstRow && SimulatedSupport.isLoaded() && SimulatedSupport.isMainTab(parent)) {
                    out.addAll(hostDrawnRows());
                }
                firstRow = false;
            }
        }
        return out;
    }

    private static List<Entry> hostDrawnRows() {
        List<Entry> out = new ArrayList<>();
        for (ResourceLocation id : SimulatedHub.adoptedInDrawOrder()) {
            Component title = SimulatedHub.adoptedTitle(id);
            out.add(new Entry(id, labelFor(id, title), false, false, null, null, false));
        }
        for (SimulatedHub.NativeSection section : SimulatedHub.nativeSections()) {
            out.add(new Entry(section.id(), labelFor(section.id(), section.title()), false, true,
                    section.textColor(), section.secondaryTextColor(), false));
        }
        return out;
    }

    private record Row(ResourceLocation id, Component name, boolean tabOwned) {}

    private static List<ResourceLocation> orderedParents(Set<ResourceLocation> managed) {
        List<ResourceLocation> order = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id != null && managed.contains(id) && !order.contains(id)) {
                order.add(id);
            }
        }
        for (ResourceLocation id : managed) {
            if (!order.contains(id)) {
                order.add(id);
            }
        }
        return order;
    }

    private static List<Row> sectionOrder(ResourceLocation parent, List<ResourceLocation> predictedAddons,
            Map<ResourceLocation, Component> names) {
        List<Section<?>> live = FancyTabSections.REGISTERED_TABS.get(parent);
        if (live != null && !live.isEmpty()) {
            List<Row> rows = new ArrayList<>(live.size());
            for (Section<?> section : live) {
                ResourceLocation id = section.id();
                rows.add(new Row(id, id.equals(parent) ? hubName(parent) : nameOf(id, section, names),
                        TabLayout.ownerOfSectionId(id) != null));
            }
            return rows;
        }
        List<Row> rows = new ArrayList<>(predictedAddons.size() + 1);
        rows.add(new Row(parent, hubName(parent), false));
        for (ResourceLocation id : Config.applyOrder(predictedAddons,
                id -> names.getOrDefault(id, Component.literal(id.toString())).getString())) {
            rows.add(new Row(id, nameOf(id, null, names), false));
        }
        return rows;
    }

    private static Component hubName(ResourceLocation parent) {
        String override = Config.sectionNameOverride(parent);
        return labelFor(parent, override == null ? null : Component.literal(override));
    }

    public static Component labelFor(ResourceLocation id, Component candidate) {
        if (candidate != null && !candidate.getString().isBlank()) {
            return candidate;
        }
        if (id == null) {
            return Component.empty();
        }
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
        if (tab instanceof CreativeModeTabDisplayNameAccessor accessor) {
            Component raw = accessor.createaddonorganizer$rawDisplayName();
            if (raw != null && !raw.getString().isBlank()) {
                return raw;
            }
        }
        String modName = ModList.get().getModContainerById(id.getNamespace())
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(null);
        if (modName != null && !modName.isBlank()) {
            return Component.literal(modName);
        }
        return Component.literal(id.toString());
    }

    private static Component nameOf(ResourceLocation id, Section<?> section, Map<ResourceLocation, Component> names) {
        if (section != null && TabLayout.ownerOfSectionId(id) != null) {
            return labelFor(id, CaoSection.titleOf(section));
        }
        Component registered = names.get(id);
        if (registered != null && !registered.getString().isBlank()) {
            return registered;
        }
        String override = Config.sectionNameOverride(id);
        return labelFor(id, override == null ? null : Component.literal(override));
    }
}

