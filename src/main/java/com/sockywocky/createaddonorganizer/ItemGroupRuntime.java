package com.sockywocky.createaddonorganizer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;

public final class ItemGroupRuntime {
    public record Fold(String groupId, String title, int index, int memberCount, boolean headIsMember,
            boolean open) {
        public int slotSpan() {
            if (!open) {
                return 1;
            }
            return headIsMember ? Math.max(1, memberCount) : memberCount + 1;
        }
    }

    private static final Map<ResourceLocation, List<Fold>> BY_SECTION = new ConcurrentHashMap<>();

    private ItemGroupRuntime() {}

    public static ResourceLocation flatKey(ResourceLocation tabId) {
        if (tabId == null) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID,
                "tab/" + tabId.getNamespace() + "/" + tabId.getPath());
    }

    public static String key(ResourceLocation sectionId, String groupId) {
        return sectionId == null || groupId == null ? null : sectionId + "#" + groupId;
    }

    public static boolean isOpen(ResourceLocation sectionId, String groupId) {
        return Config.isItemGroupOpen(key(sectionId, groupId));
    }

    public static void setOpen(ResourceLocation sectionId, String groupId, boolean open) {
        Config.setItemGroupOpen(key(sectionId, groupId), open);
    }

    public static boolean toggle(ResourceLocation sectionId, String groupId) {
        boolean next = !isOpen(sectionId, groupId);
        setOpen(sectionId, groupId, next);
        return next;
    }

    public static void register(ResourceLocation sectionId, List<Fold> folds) {
        if (sectionId == null) {
            return;
        }
        if (folds == null || folds.isEmpty()) {
            BY_SECTION.remove(sectionId);
        } else {
            BY_SECTION.put(sectionId, List.copyOf(folds));
        }
    }

    public static List<Fold> foldsOf(ResourceLocation sectionId) {
        if (sectionId == null) {
            return List.of();
        }
        return BY_SECTION.getOrDefault(sectionId, List.of());
    }

    public static boolean anyFolds() {
        return !BY_SECTION.isEmpty();
    }

    public static Fold foldAt(ResourceLocation sectionId, int localIndex) {
        for (Fold fold : foldsOf(sectionId)) {
            if (fold.index() == localIndex) {
                return fold;
            }
        }
        return null;
    }

    public static Fold owningFold(ResourceLocation sectionId, int localIndex) {
        for (Fold fold : foldsOf(sectionId)) {
            if (localIndex >= fold.index() && localIndex < fold.index() + fold.slotSpan()) {
                return fold;
            }
        }
        return null;
    }

    public static void clear() {
        BY_SECTION.clear();
    }

    public static void clear(ResourceLocation sectionId) {
        if (sectionId != null) {
            BY_SECTION.remove(sectionId);
        }
    }
}
