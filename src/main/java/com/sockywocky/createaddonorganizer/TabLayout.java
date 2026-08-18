package com.sockywocky.createaddonorganizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record TabLayout(String tab, String name, String icon, int nextSectionId,
        List<Entry> entries, List<String> removed, boolean seeded,
        List<ItemGroup> itemGroups, int nextGroupId) {

    public static final String SLOT_PREFIX = "custom_";

    public record Entry(Integer section, String title, String item, String source, String group) {

        public static Entry section(int id, String title) {
            return new Entry(id, title, null, null, null);
        }

        public static Entry section(int id, String title, String source) {
            return new Entry(id, title, null, source, null);
        }

        public static Entry item(String itemId) {
            return new Entry(null, null, itemId, null, null);
        }

        public static Entry item(String itemId, String groupId) {
            return new Entry(null, null, itemId, null, groupId);
        }

        public ResourceLocation sourceId() {
            return source == null || source.isBlank() ? null : ResourceLocation.tryParse(source);
        }

        public boolean isSection() {
            return section != null;
        }

        public boolean isItem() {
            return item != null && !item.isBlank();
        }

        public String groupId() {
            return group == null || group.isBlank() ? null : group;
        }

        public boolean isGrouped() {
            return isItem() && groupId() != null;
        }

        public Entry withGroup(String groupId) {
            return new Entry(section, title, item, source, groupId);
        }
    }

    public record ItemGroup(String id, String title, String icon) {

        public String displayTitle() {
            return title == null || title.isBlank() ? "Group" : title;
        }

        public ItemGroup withTitle(String newTitle) {
            return new ItemGroup(id, newTitle, icon);
        }

        public ItemGroup withIcon(String newIcon) {
            return new ItemGroup(id, title, newIcon);
        }
    }

    public static TabLayout empty(ResourceLocation tabId, String name, String icon) {
        boolean custom = slotOf(tabId) >= 0;
        return new TabLayout(tabId.toString(), name, icon, 0, List.of(), List.of(), custom, List.of(), 0);
    }

    public static ResourceLocation idForSlot(int slot) {
        return ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID, SLOT_PREFIX + slot);
    }

    public static int slotOf(ResourceLocation id) {
        if (id == null || !createaddonorganizer.MODID.equals(id.getNamespace())) {
            return -1;
        }
        String path = id.getPath();
        if (!path.startsWith(SLOT_PREFIX) || path.indexOf('/') >= 0) {
            return -1;
        }
        try {
            return Integer.parseInt(path.substring(SLOT_PREFIX.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public ResourceLocation id() {
        ResourceLocation parsed = ResourceLocation.tryParse(tab);
        return parsed == null ? idForSlot(0) : parsed;
    }

    public int slot() {
        return slotOf(id());
    }

    public boolean isCustom() {
        return slot() >= 0;
    }

    public ResourceLocation sectionId(int sectionId) {
        ResourceLocation base = id();
        int slot = slotOf(base);
        if (slot >= 0) {
            return ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID,
                    SLOT_PREFIX + slot + "/s" + sectionId);
        }
        return ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID,
                base.getNamespace() + "/" + base.getPath() + "/s" + sectionId);
    }

    public static ResourceLocation ownerOfSectionId(ResourceLocation sectionId) {
        if (sectionId == null || !createaddonorganizer.MODID.equals(sectionId.getNamespace())) {
            return null;
        }
        String path = sectionId.getPath();
        int lastSlash = path.lastIndexOf("/s");
        if (lastSlash < 0) {
            return null;
        }
        String suffix = path.substring(lastSlash + 2);
        if (suffix.isEmpty() || !suffix.chars().allMatch(Character::isDigit)) {
            return null;
        }
        String owner = path.substring(0, lastSlash);
        int split = owner.indexOf('/');
        if (split < 0) {
            return ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID, owner);
        }
        return ResourceLocation.tryParse(owner.substring(0, split) + ":" + owner.substring(split + 1));
    }

    public boolean hasSourcedSections() {
        for (Entry entry : safeEntries()) {
            if (entry.isSection() && entry.sourceId() != null) {
                return true;
            }
        }
        return false;
    }

    public ResourceLocation sectionIdFor(Entry entry) {
        ResourceLocation source = entry.sourceId();
        return source != null ? source : sectionId(entry.section());
    }

    public TabLayout withSeeded(List<Entry> newEntries, int newNextSectionId) {
        return rebuilt(name, icon, newNextSectionId, List.copyOf(newEntries), safeRemoved(), true);
    }

    private TabLayout rebuilt(String newName, String newIcon, int newNextSectionId, List<Entry> newEntries,
            List<String> newRemoved, boolean newSeeded) {
        List<ItemGroup> defined = safeItemGroups();
        if (defined.isEmpty()) {
            return new TabLayout(tab, newName, newIcon, newNextSectionId, newEntries, newRemoved, newSeeded,
                    defined, nextGroupId);
        }
        Map<String, Integer> counts = new HashMap<>();
        for (Entry entry : newEntries) {
            String id = entry.isItem() ? entry.groupId() : null;
            if (id != null) {
                counts.merge(id, 1, Integer::sum);
            }
        }
        Set<String> kept = new LinkedHashSet<>();
        List<ItemGroup> groups = new ArrayList<>(defined.size());
        for (ItemGroup group : defined) {
            if (counts.getOrDefault(group.id(), 0) >= 2) {
                kept.add(group.id());
                groups.add(group);
            }
        }
        List<Entry> cleaned = newEntries;
        if (kept.size() != counts.size()) {
            cleaned = new ArrayList<>(newEntries.size());
            for (Entry entry : newEntries) {
                String id = entry.groupId();
                cleaned.add(id != null && !kept.contains(id) ? entry.withGroup(null) : entry);
            }
            cleaned = List.copyOf(cleaned);
        }
        return new TabLayout(tab, newName, newIcon, newNextSectionId, cleaned, newRemoved, newSeeded,
                List.copyOf(groups), nextGroupId);
    }

    public List<Entry> safeEntries() {
        return entries == null ? List.of() : entries;
    }

    public List<ItemGroup> safeItemGroups() {
        return itemGroups == null ? List.of() : itemGroups;
    }

    public ItemGroup itemGroup(String groupId) {
        if (groupId == null) {
            return null;
        }
        for (ItemGroup group : safeItemGroups()) {
            if (groupId.equals(group.id())) {
                return group;
            }
        }
        return null;
    }

    public int itemGroupCount() {
        return safeItemGroups().size();
    }

    public List<String> membersOf(String groupId) {
        if (groupId == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Entry entry : safeEntries()) {
            if (entry.isItem() && groupId.equals(entry.groupId())) {
                out.add(entry.item());
            }
        }
        return out;
    }

    public String iconItemOf(String groupId) {
        ItemGroup group = itemGroup(groupId);
        if (group != null && group.icon() != null && !group.icon().isBlank()
                && resolveItem(group.icon()) != null) {
            return group.icon();
        }
        List<String> members = membersOf(groupId);
        return members.isEmpty() ? null : members.get(0);
    }

    public TabLayout withEntriesGrouped(List<Entry> members, String title) {
        return withEntriesGrouped(members, title, null);
    }

    public TabLayout withEntriesGrouped(List<Entry> members, String title, String iconItem) {
        if (members == null || members.size() < 2) {
            return this;
        }
        String groupId = "g" + nextGroupId;
        List<Entry> src = safeEntries();
        List<Entry> moved = new ArrayList<>(members.size());
        List<Entry> out = new ArrayList<>(src.size());
        int insertAt = -1;
        for (Entry entry : src) {
            if (entry.isItem() && containsSame(members, entry)) {
                if (insertAt < 0) {
                    insertAt = out.size();
                }
                moved.add(entry.withGroup(groupId));
                continue;
            }
            out.add(entry);
        }
        if (moved.size() < 2 || insertAt < 0) {
            return this;
        }
        out.addAll(insertAt, moved);
        List<ItemGroup> groups = new ArrayList<>(safeItemGroups());
        groups.add(new ItemGroup(groupId, title,
                iconItem == null || iconItem.isBlank() ? moved.get(0).item() : iconItem));
        return new TabLayout(tab, name, icon, nextSectionId, List.copyOf(out), safeRemoved(), seeded,
                List.copyOf(groups), nextGroupId + 1);
    }

    public TabLayout withGroupDissolved(String groupId) {
        if (itemGroup(groupId) == null) {
            return this;
        }
        List<Entry> out = new ArrayList<>(safeEntries().size());
        for (Entry entry : safeEntries()) {
            out.add(groupId.equals(entry.groupId()) ? entry.withGroup(null) : entry);
        }
        return withEntries(out);
    }

    public TabLayout withItemUngrouped(String itemId) {
        List<Entry> out = new ArrayList<>(safeEntries().size());
        boolean changed = false;
        for (Entry entry : safeEntries()) {
            if (entry.isItem() && entry.item().equals(itemId) && entry.groupId() != null) {
                out.add(entry.withGroup(null));
                changed = true;
                continue;
            }
            out.add(entry);
        }
        return changed ? withEntries(out) : this;
    }

    public TabLayout withGroupChanged(ItemGroup updated) {
        if (updated == null || itemGroup(updated.id()) == null) {
            return this;
        }
        List<ItemGroup> groups = new ArrayList<>(safeItemGroups());
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).id().equals(updated.id())) {
                groups.set(i, updated);
            }
        }
        return new TabLayout(tab, name, icon, nextSectionId, safeEntries(), safeRemoved(), seeded,
                List.copyOf(groups), nextGroupId);
    }

    public TabLayout withImportedGroup(ItemGroup group, List<String> members) {
        if (group == null || members.size() < 2) {
            return this;
        }
        TabLayout base = withGroupDissolved(group.id());
        Set<String> wanted = new LinkedHashSet<>(members);
        List<Entry> src = base.safeEntries();
        List<Entry> moved = new ArrayList<>();
        List<Entry> out = new ArrayList<>(src.size());
        int insertAt = -1;
        for (Entry entry : src) {
            if (entry.isItem() && wanted.contains(entry.item())) {
                if (insertAt < 0) {
                    insertAt = out.size();
                }
                moved.add(entry.withGroup(group.id()));
                continue;
            }
            out.add(entry);
        }
        if (moved.size() < 2 || insertAt < 0) {
            return this;
        }
        out.addAll(insertAt, moved);
        List<ItemGroup> groups = new ArrayList<>(base.safeItemGroups());
        groups.removeIf(existing -> existing.id().equals(group.id()));
        groups.add(group);
        return new TabLayout(tab, name, icon, nextSectionId, List.copyOf(out), safeRemoved(), seeded,
                List.copyOf(groups), nextGroupId);
    }

    public List<String> safeRemoved() {
        return removed == null ? List.of() : removed;
    }

    public Set<String> removedSet() {
        return new LinkedHashSet<>(safeRemoved());
    }

    public String nameOverride() {
        return name == null || name.isBlank() ? null : name;
    }

    public String displayName() {
        String override = nameOverride();
        if (override != null) {
            return override;
        }
        int slot = slot();
        return slot >= 0
                ? Component.translatable("createaddonorganizer.tabs.defaultTabName", slot + 1).getString()
                : id().toString();
    }

    public String iconOverride() {
        return icon == null || icon.isBlank() ? null : icon;
    }

    public ItemStack iconStack() {
        Item item = resolveItem(icon);
        if (item != null) {
            return new ItemStack(item);
        }
        return isCustom() ? new ItemStack(Items.CHEST) : ItemStack.EMPTY;
    }

    public List<ItemStack> resolvedItems() {
        List<ItemStack> out = new ArrayList<>();
        for (Entry entry : safeEntries()) {
            if (!entry.isItem()) {
                continue;
            }
            Item item = resolveItem(entry.item());
            if (item != null) {
                out.add(new ItemStack(item));
            }
        }
        return out;
    }

    public record Group(Integer sectionId, String source, String title, List<ItemStack> items,
            List<ItemGroupRuntime.Fold> folds) {

        public Group(Integer sectionId, String source, String title, List<ItemStack> items) {
            this(sectionId, source, title, items, List.of());
        }

        public ResourceLocation sourceId() {
            return source == null || source.isBlank() ? null : ResourceLocation.tryParse(source);
        }

        public List<ItemGroupRuntime.Fold> safeFolds() {
            return folds == null ? List.of() : folds;
        }
    }

    public ResourceLocation idForGroup(Group group) {
        ResourceLocation source = group.sourceId();
        if (source != null) {
            return source;
        }
        return group.sectionId() == null ? id() : sectionId(group.sectionId());
    }

    public List<Group> groups() {
        List<Group> out = new ArrayList<>();
        Integer currentId = null;
        String currentSource = null;
        String currentTitle = displayName();
        List<ItemStack> current = new ArrayList<>();
        for (Entry entry : safeEntries()) {
            if (entry.isSection()) {
                if (!current.isEmpty()) {
                    out.add(new Group(currentId, currentSource, currentTitle, List.copyOf(current)));
                }
                current = new ArrayList<>();
                currentId = entry.section();
                currentSource = entry.source();
                currentTitle = entry.title() == null || entry.title().isBlank() ? "Section" : entry.title();
            } else if (entry.isItem()) {
                Item item = resolveItem(entry.item());
                if (item != null) {
                    current.add(new ItemStack(item));
                }
            }
        }
        if (!current.isEmpty() || out.isEmpty()) {
            out.add(new Group(currentId, currentSource, currentTitle, List.copyOf(current)));
        }
        return out;
    }

    public int itemCount() {
        int n = 0;
        for (Entry entry : safeEntries()) {
            if (entry.isItem()) {
                n++;
            }
        }
        return n;
    }

    public int missingItemCount() {
        int n = 0;
        for (Entry entry : safeEntries()) {
            if (entry.isItem() && resolveItem(entry.item()) == null) {
                n++;
            }
        }
        return n;
    }

    public int sectionCount() {
        int n = 0;
        for (Entry entry : safeEntries()) {
            if (entry.isSection()) {
                n++;
            }
        }
        return n;
    }

    public Map<String, Integer> itemCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Entry entry : safeEntries()) {
            if (entry.isItem()) {
                counts.merge(entry.item(), 1, Integer::sum);
            }
        }
        return counts;
    }

    public static int indexOfSame(List<Entry> entries, Entry target) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    public boolean isEmptyOverride() {
        return safeEntries().isEmpty() && safeRemoved().isEmpty()
                && nameOverride() == null && iconOverride() == null;
    }

    public TabLayout withName(String newName) {
        return rebuilt(newName, icon, nextSectionId, safeEntries(), safeRemoved(), seeded);
    }

    public TabLayout withIcon(String newIcon) {
        return rebuilt(name, newIcon, nextSectionId, safeEntries(), safeRemoved(), seeded);
    }

    public TabLayout withSectionsOrdered(List<ResourceLocation> order) {
        List<Entry> src = safeEntries();
        if (src.isEmpty() || order == null || order.size() < 2) {
            return this;
        }
        List<List<Entry>> blocks = new ArrayList<>();
        List<Entry> current = null;
        for (Entry entry : src) {
            if (entry.isSection() || current == null) {
                if (current != null) {
                    blocks.add(current);
                }
                current = new ArrayList<>();
            }
            current.add(entry);
        }
        if (current != null) {
            blocks.add(current);
        }

        Map<ResourceLocation, Integer> rank = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            rank.putIfAbsent(order.get(i), i);
        }
        List<Integer> slots = new ArrayList<>();
        List<List<Entry>> movable = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            ResourceLocation id = blockSectionId(blocks.get(i));
            if (id != null && rank.containsKey(id)) {
                slots.add(i);
                movable.add(blocks.get(i));
            }
        }
        if (movable.size() < 2) {
            return this;
        }
        movable.sort(Comparator.comparingInt(b -> rank.get(blockSectionId(b))));
        for (int i = 0; i < slots.size(); i++) {
            blocks.set(slots.get(i), movable.get(i));
        }
        List<Entry> out = new ArrayList<>(src.size());
        for (List<Entry> block : blocks) {
            out.addAll(block);
        }
        return out.equals(src) ? this : withEntries(out);
    }

    private ResourceLocation blockSectionId(List<Entry> block) {
        Entry head = block.isEmpty() ? null : block.get(0);
        return head == null || !head.isSection() ? null : sectionIdFor(head);
    }

    public TabLayout withEntries(List<Entry> newEntries) {
        return rebuilt(name, icon, nextSectionId, List.copyOf(newEntries), safeRemoved(), seeded);
    }

    public TabLayout withEntriesAndRemoved(List<Entry> newEntries, List<String> newRemoved) {
        return rebuilt(name, icon, nextSectionId, List.copyOf(newEntries), List.copyOf(newRemoved), seeded);
    }

    public TabLayout withSeeded(List<Entry> newEntries) {
        return rebuilt(name, icon, nextSectionId, List.copyOf(newEntries), safeRemoved(), true);
    }

    public TabLayout withSectionTitle(ResourceLocation sectionId, String title) {
        List<Entry> updated = new ArrayList<>(safeEntries());
        boolean changed = false;
        for (int i = 0; i < updated.size(); i++) {
            Entry entry = updated.get(i);
            if (entry.isSection() && sectionIdFor(entry).equals(sectionId)) {
                updated.set(i, new Entry(entry.section(), title, null, entry.source(), null));
                changed = true;
            }
        }
        return changed ? withEntries(updated) : this;
    }

    public TabLayout withTabFolded(ResourceLocation source, String title, List<String> itemIds) {
        String sourceKey = source.toString();
        for (Entry entry : safeEntries()) {
            if (entry.isSection() && sourceKey.equals(entry.source())) {
                return this;
            }
        }
        Set<String> claimed = new LinkedHashSet<>(safeRemoved());
        for (Entry entry : safeEntries()) {
            if (entry.isItem()) {
                claimed.add(entry.item());
            }
        }
        List<Entry> added = new ArrayList<>();
        for (String itemId : itemIds) {
            if (claimed.add(itemId)) {
                added.add(Entry.item(itemId));
            }
        }
        if (added.isEmpty()) {
            return this;
        }
        List<Entry> updated = new ArrayList<>(safeEntries());
        updated.add(Entry.section(nextSectionId, title, sourceKey));
        updated.addAll(added);
        return rebuilt(name, icon, nextSectionId + 1, List.copyOf(updated), safeRemoved(), seeded);
    }

    public TabLayout withSectionAdded(String title) {
        List<Entry> updated = new ArrayList<>(safeEntries());
        updated.add(Entry.section(nextSectionId, title));
        return rebuilt(name, icon, nextSectionId + 1, List.copyOf(updated), safeRemoved(), seeded);
    }

    public TabLayout withEntryInserted(Entry entry, int index) {
        if (entry == null || !entry.isItem()) {
            return this;
        }
        List<Entry> updated = new ArrayList<>(safeEntries());
        if (index < 0 || index > updated.size()) {
            updated.add(entry);
        } else {
            updated.add(index, entry);
        }
        List<String> stillRemoved = new ArrayList<>(safeRemoved());
        stillRemoved.remove(entry.item());
        return withEntriesAndRemoved(updated, stillRemoved);
    }

    public TabLayout withEntriesRemoved(List<Entry> doomed) {
        if (doomed == null || doomed.isEmpty()) {
            return this;
        }
        List<Entry> updated = new ArrayList<>(safeEntries().size());
        List<String> lostItems = new ArrayList<>();
        for (Entry entry : safeEntries()) {
            if (containsSame(doomed, entry)) {
                if (entry.isItem()) {
                    lostItems.add(entry.item());
                }
                continue;
            }
            updated.add(entry);
        }
        if (isCustom() || lostItems.isEmpty()) {
            return withEntries(updated);
        }
        List<String> nowRemoved = new ArrayList<>(safeRemoved());
        for (String itemId : lostItems) {
            if (nowRemoved.contains(itemId) || stillHolds(updated, itemId)) {
                continue;
            }
            nowRemoved.add(itemId);
        }
        return withEntriesAndRemoved(updated, nowRemoved);
    }

    private static boolean stillHolds(List<Entry> entries, String itemId) {
        for (Entry entry : entries) {
            if (entry.isItem() && entry.item().equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSame(List<Entry> entries, Entry target) {
        return indexOfSame(entries, target) >= 0;
    }

    public TabLayout reconciledWith(List<String> producedIds) {
        Set<String> known = new LinkedHashSet<>();
        for (Entry entry : safeEntries()) {
            if (entry.isItem()) {
                known.add(entry.item());
            }
        }
        known.addAll(safeRemoved());
        List<Entry> appended = null;
        for (String id : producedIds) {
            if (known.add(id)) {
                if (appended == null) {
                    appended = new ArrayList<>(safeEntries());
                }
                appended.add(Entry.item(id));
            }
        }
        return appended == null ? this : withSeeded(appended);
    }

    private static Item resolveItem(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(parsed);
        return item == Items.AIR ? null : item;
    }
}

