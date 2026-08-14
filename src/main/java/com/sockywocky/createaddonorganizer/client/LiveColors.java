package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import com.sockywocky.createaddonorganizer.client.simulated.NativeSections;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedHub;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class LiveColors {
    private LiveColors() {}

    public static void apply(ResourceLocation id, ColorSpec spec) {
        if (isSimulated(id)) {
            return;
        }
        replace(id, s -> s.withBanner(spec));
    }

    public static void applyTexture(ResourceLocation id, ResourceLocation texture) {
        if (isSimulated(id)) {
            return;
        }
        replace(id, s -> s.withTexture(texture));
    }

    public static void applyTextColor(ResourceLocation id, ColorSpec spec) {
        if (isSimulated(id)) {
            return;
        }
        replace(id, s -> s.withTextColor(spec));
    }

    public static void applyTitle(ResourceLocation id, Component title) {
        if (isSimulated(id)) {
            SimulatedHub.applyTitle(id, title);
            return;
        }
        replace(id, s -> s.withTitle(title));
    }

    public static void applyOrder(ResourceLocation parent, List<ResourceLocation> orderedAddonIds) {
        if (SimulatedSupport.isMainTab(parent)) {
            SimulatedHub.reorder(orderedAddonIds);
            return;
        }
        List<Section<?>> sections = FancyTabSections.REGISTERED_TABS.get(parent);
        if (sections == null || sections.isEmpty()) {
            return;
        }
        Set<ResourceLocation> movable = new LinkedHashSet<>(orderedAddonIds);
        List<Integer> slots = new ArrayList<>();
        Map<ResourceLocation, Section<?>> byId = new LinkedHashMap<>();
        for (int i = 0; i < sections.size(); i++) {
            Section<?> section = sections.get(i);
            if (movable.contains(section.id())) {
                slots.add(i);
                byId.put(section.id(), section);
            }
        }
        if (slots.isEmpty()) {
            return;
        }
        List<Section<?>> ordered = new ArrayList<>(slots.size());
        for (ResourceLocation id : orderedAddonIds) {
            Section<?> section = byId.remove(id);
            if (section != null) {
                ordered.add(section);
            }
        }
        ordered.addAll(byId.values());
        List<Section<?>> rebuilt = new ArrayList<>(sections);
        for (int i = 0; i < slots.size(); i++) {
            rebuilt.set(slots.get(i), ordered.get(i));
        }
        FancyTabSections.REGISTERED_TABS.put(parent, rebuilt);
    }

    public static ResourceLocation findParent(ResourceLocation id) {
        if (SimulatedSupport.isLoaded()) {
            ResourceLocation owner = SimulatedHub.ownerIfAny(id);
            if (owner != null) {
                return owner;
            }
        }
        for (Map.Entry<ResourceLocation, List<Section<?>>> entry : FancyTabSections.REGISTERED_TABS.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            for (Section<?> section : entry.getValue()) {
                if (section.id().equals(id)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public static Section<?> remove(ResourceLocation id) {
        if (isSimulated(id)) {
            SimulatedHub.remove(id);
            return null;
        }
        Section<?> first = null;
        for (ResourceLocation parent : new ArrayList<>(FancyTabSections.REGISTERED_TABS.keySet())) {
            List<Section<?>> sections = FancyTabSections.REGISTERED_TABS.get(parent);
            if (sections == null) {
                continue;
            }
            List<Section<?>> rebuilt = new ArrayList<>(sections.size());
            for (Section<?> section : sections) {
                if (section.id().equals(id)) {
                    if (first == null) {
                        first = section;
                    }
                } else {
                    rebuilt.add(section);
                }
            }
            if (rebuilt.size() != sections.size()) {
                FancyTabSections.REGISTERED_TABS.put(parent, rebuilt);
            }
        }
        return first;
    }

    public static void moveToParent(ResourceLocation id, ResourceLocation newParent) {
        boolean toSimulated = SimulatedSupport.isMainTab(newParent);
        if (isSimulated(id)) {
            if (toSimulated) {
                return;
            }
            SimulatedHub.remove(id);
            Section<?> section = createaddonorganizer.sectionFromLiveTab(id);
            if (section != null) {
                FancyTabSections.addSection(newParent, section);
            }
            return;
        }
        if (toSimulated) {
            Section<?> removed = remove(id);
            if (removed != null) {
                SimulatedHub.inject(id, CaoSection.titleOf(removed));
                SimulatedHub.foldItems(id, removed.items().getStacks());
            }
            return;
        }
        Section<?> section = remove(id);
        if (section != null) {
            FancyTabSections.addSection(newParent, section);
        }
    }

    public static boolean isAdoptedNative(ResourceLocation id) {
        return SimulatedSupport.isLoaded() && SimulatedHub.isAdopted(id);
    }

    public static Component nativeTitle(ResourceLocation id) {
        return isAdoptedNative(id) ? SimulatedHub.originalTitle(id) : null;
    }

    public static void releaseNative(ResourceLocation id) {
        if (SimulatedSupport.isLoaded()) {
            NativeSections.release(id);
        }
    }

    public static void readoptNative(ResourceLocation id) {
        if (SimulatedSupport.isLoaded()) {
            NativeSections.readopt(id);
        }
    }

    private static boolean isSimulated(ResourceLocation id) {
        return SimulatedSupport.isLoaded() && SimulatedHub.ownerIfAny(id) != null;
    }

    private static void replace(ResourceLocation id, UnaryOperator<CaoSection> rebuild) {
        for (ResourceLocation parent : new ArrayList<>(FancyTabSections.REGISTERED_TABS.keySet())) {
            List<Section<?>> sections = FancyTabSections.REGISTERED_TABS.get(parent);
            if (sections == null) {
                continue;
            }
            boolean changed = false;
            List<Section<?>> rebuilt = new ArrayList<>(sections.size());
            for (Section<?> section : sections) {
                if (section.id().equals(id) && section instanceof CaoSection cao) {
                    rebuilt.add(rebuild.apply(cao));
                    changed = true;
                } else {
                    rebuilt.add(section);
                }
            }
            if (changed) {
                FancyTabSections.REGISTERED_TABS.put(parent, rebuilt);
            }
        }
    }
}

