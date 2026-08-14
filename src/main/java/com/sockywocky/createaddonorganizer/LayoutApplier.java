package com.sockywocky.createaddonorganizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class LayoutApplier {

    private LayoutApplier() {}

    public static List<String> producedIds(Iterable<ItemStack> stacks) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ItemStack stack : stacks) {
            String id = idOf(stack);
            if (id != null && seen.add(id)) {
                out.add(id);
            }
        }
        return out;
    }

    public static void apply(BuildCreativeModeTabContentsEvent event, ResourceLocation tabId) {
        TabLayout layout = TabLayoutStore.byId(tabId);
        if (layout == null || layout.isCustom() || !layout.seeded()) {
            return;
        }
        if (layout.hasSourcedSections()) {
            applyRemovals(event, layout);
            return;
        }
        List<ItemStack> parent = new ArrayList<>(event.getParentEntries());
        if (parent.isEmpty() && layout.safeEntries().isEmpty()) {
            return;
        }

        Map<String, List<ItemStack>> byId = new LinkedHashMap<>();
        for (ItemStack stack : parent) {
            String id = idOf(stack);
            if (id != null) {
                byId.computeIfAbsent(id, k -> new ArrayList<>()).add(stack);
            }
        }

        Set<String> removed = layout.removedSet();
        Set<String> placed = new HashSet<>();
        List<ItemStack> ordered = new ArrayList<>(parent.size());
        for (TabLayout.Entry entry : layout.safeEntries()) {
            if (!entry.isItem() || removed.contains(entry.item()) || !placed.add(entry.item())) {
                continue;
            }
            List<ItemStack> existing = byId.get(entry.item());
            if (existing != null) {
                ordered.addAll(existing);
                continue;
            }
            ItemStack added = stackOf(entry.item());
            if (!added.isEmpty()) {
                ordered.add(added);
            }
        }
        for (Map.Entry<String, List<ItemStack>> e : byId.entrySet()) {
            if (!placed.contains(e.getKey()) && !removed.contains(e.getKey())) {
                ordered.addAll(e.getValue());
            }
        }

        for (ItemStack stack : parent) {
            event.remove(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
        for (ItemStack stack : ordered) {
            CreativeModeTab.TabVisibility visibility = event.getSearchEntries().contains(stack)
                    ? CreativeModeTab.TabVisibility.PARENT_TAB_ONLY
                    : CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            try {
                event.accept(stack, visibility);
            } catch (IllegalArgumentException e) {
                createaddonorganizer.LOGGER.debug("[CAO] could not re-add {} to {}", stack, tabId);
            }
        }
    }

    private static void applyRemovals(BuildCreativeModeTabContentsEvent event, TabLayout layout) {
        Set<String> removed = layout.removedSet();
        if (removed.isEmpty()) {
            return;
        }
        for (ItemStack stack : new ArrayList<>(event.getParentEntries())) {
            String id = idOf(stack);
            if (id != null && removed.contains(id)) {
                event.remove(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            }
        }
    }

    public static List<TabLayout.Group> groupsOf(TabLayout layout, Iterable<ItemStack> realEntries,
            String ownTitle) {
        return groupsOf(layout, realEntries, ownTitle, List.of());
    }

    public static List<TabLayout.Group> groupsOf(TabLayout layout, Iterable<ItemStack> realEntries,
            String ownTitle, Iterable<ItemStack> lookupOnly) {
        Map<String, List<ItemStack>> byId = new LinkedHashMap<>();
        for (ItemStack stack : realEntries) {
            String id = idOf(stack);
            if (id != null) {
                byId.computeIfAbsent(id, k -> new ArrayList<>()).add(stack);
            }
        }
        Map<String, List<ItemStack>> borrowed = new LinkedHashMap<>();
        for (ItemStack stack : lookupOnly) {
            String id = idOf(stack);
            if (id != null && !byId.containsKey(id)) {
                borrowed.computeIfAbsent(id, k -> new ArrayList<>()).add(stack);
            }
        }

        List<TabLayout.Group> out = new ArrayList<>();
        Integer currentId = null;
        String currentSource = null;
        String currentTitle = ownTitle;
        SectionStacks current = new SectionStacks();
        Set<String> consumed = new HashSet<>(layout.removedSet());
        for (TabLayout.Entry entry : layout.safeEntries()) {
            if (entry.isSection()) {
                if (!current.isEmpty() || isOwnHeader(layout, currentId, currentSource)) {
                    out.add(current.build(layout, currentId, currentSource, currentTitle));
                }
                current = new SectionStacks();
                currentId = entry.section();
                currentSource = entry.source();
                currentTitle = entry.title() == null || entry.title().isBlank() ? "Section" : entry.title();
            } else if (entry.isItem() && consumed.add(entry.item())) {
                List<ItemStack> stacks = byId.get(entry.item());
                if (stacks == null) {
                    stacks = borrowed.get(entry.item());
                }
                if (stacks == null) {
                    ItemStack resolved = stackOf(entry.item());
                    stacks = resolved.isEmpty() ? List.of() : List.of(resolved);
                }
                current.add(entry.groupId(), stacks);
            }
        }
        if (!current.isEmpty() || out.isEmpty() || isOwnHeader(layout, currentId, currentSource)) {
            out.add(current.build(layout, currentId, currentSource, currentTitle));
        }

        List<ItemStack> unclaimed = new ArrayList<>();
        for (Map.Entry<String, List<ItemStack>> e : byId.entrySet()) {
            if (!consumed.contains(e.getKey())) {
                unclaimed.addAll(e.getValue());
            }
        }
        return withUnclaimed(layout, out, unclaimed, ownTitle);
    }

    private static final class SectionStacks {

        private final List<Object> slots = new ArrayList<>();
        private final Map<String, List<ItemStack>> members = new LinkedHashMap<>();

        private boolean isEmpty() {
            return slots.isEmpty();
        }

        private void add(String groupId, List<ItemStack> stacks) {
            if (stacks.isEmpty()) {
                return;
            }
            if (groupId == null) {
                slots.addAll(stacks);
                return;
            }
            List<ItemStack> existing = members.get(groupId);
            if (existing == null) {
                existing = new ArrayList<>();
                members.put(groupId, existing);
                slots.add(groupId);
            }
            existing.addAll(stacks);
        }

        private TabLayout.Group build(TabLayout layout, Integer sectionId, String source, String title) {
            TabLayout.Group shell = new TabLayout.Group(sectionId, source, title, List.of());
            if (members.isEmpty()) {
                List<ItemStack> plain = new ArrayList<>(slots.size());
                for (Object slot : slots) {
                    plain.add((ItemStack) slot);
                }
                return new TabLayout.Group(sectionId, source, title, List.copyOf(plain), List.of());
            }
            ResourceLocation resolvedSectionId = layout.idForGroup(shell);
            List<ItemStack> stacks = new ArrayList<>(slots.size());
            List<ItemGroupRuntime.Fold> folds = new ArrayList<>(members.size());
            for (Object slot : slots) {
                if (slot instanceof ItemStack stack) {
                    stacks.add(stack);
                    continue;
                }
                String groupId = (String) slot;
                List<ItemStack> owned = members.getOrDefault(groupId, List.of());
                if (owned.size() < 2) {
                    stacks.addAll(owned);
                    continue;
                }
                boolean open = ItemGroupRuntime.isOpen(resolvedSectionId, groupId);
                TabLayout.ItemGroup def = layout.itemGroup(groupId);
                folds.add(new ItemGroupRuntime.Fold(groupId,
                        def == null ? "Group" : def.displayTitle(),
                        stacks.size(), owned.size(), open));
                stacks.add(groupIconStack(layout, groupId, owned));
                if (open) {
                    stacks.addAll(owned);
                }
            }
            return new TabLayout.Group(sectionId, source, title, List.copyOf(stacks), List.copyOf(folds));
        }
    }

    private static ItemStack groupIconStack(TabLayout layout, String groupId, List<ItemStack> members) {
        String iconId = layout.iconItemOf(groupId);
        ItemStack icon = iconId == null ? ItemStack.EMPTY : stackOf(iconId);
        return icon.isEmpty() ? members.get(0).copy() : icon;
    }

    private static boolean isOwnHeader(TabLayout layout, Integer sectionId, String source) {
        if (sectionId == null && (source == null || source.isBlank())) {
            return false;
        }
        return layout.id().equals(layout.idForGroup(
                new TabLayout.Group(sectionId, source, "", List.of())));
    }

    private static List<TabLayout.Group> withUnclaimed(TabLayout layout, List<TabLayout.Group> groups,
            List<ItemStack> unclaimed, String ownTitle) {
        if (unclaimed.isEmpty()) {
            return groups;
        }
        ResourceLocation tabId = layout.id();
        for (int i = 0; i < groups.size(); i++) {
            TabLayout.Group group = groups.get(i);
            if (tabId.equals(layout.idForGroup(group))) {
                List<ItemStack> merged = new ArrayList<>(group.items());
                merged.addAll(unclaimed);
                groups.set(i, new TabLayout.Group(group.sectionId(), group.source(), group.title(),
                        List.copyOf(merged), group.safeFolds()));
                return groups;
            }
        }
        List<TabLayout.Group> out = new ArrayList<>(groups.size() + 1);
        out.add(new TabLayout.Group(null, null, ownTitle, List.copyOf(unclaimed)));
        out.addAll(groups);
        return out;
    }

    public static List<ItemStack> stacksOf(List<String> itemIds) {
        List<ItemStack> out = new ArrayList<>(itemIds.size());
        for (String id : itemIds) {
            ItemStack stack = stackOf(id);
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
        return out;
    }

    private static ItemStack stackOf(String itemId) {
        ResourceLocation parsed = ResourceLocation.tryParse(itemId);
        if (parsed == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(parsed);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static String idOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? null : key.toString();
    }
}
