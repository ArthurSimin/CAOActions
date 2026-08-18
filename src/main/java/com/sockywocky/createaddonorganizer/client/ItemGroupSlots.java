package com.sockywocky.createaddonorganizer.client;

import java.util.List;

import net.mcexpanded.fancytabsections.FTSInternal;
import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.ItemGroupRuntime;
import com.sockywocky.createaddonorganizer.TabLayout;
import com.sockywocky.createaddonorganizer.TabLayoutStore;
import com.sockywocky.createaddonorganizer.createaddonorganizer;
import com.sockywocky.createaddonorganizer.mixin.CreativeModeInventoryScreenAccessor;

public final class ItemGroupSlots {
    private static final int COLS = 9;
    private static final int VISIBLE_ROWS = 5;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_INNER = 16;

    private static final int BACKDROP = 0x7F111111;
    private static final int GLYPH_EDGE = 0xFF000000;

    private ItemGroupSlots() {}

    private record Hit(ResourceLocation sectionId, ResourceLocation source, ItemGroupRuntime.Fold fold,
            boolean head) {}

    private static CreativeModeTab cachedTab;
    private static ResourceLocation cachedTabId;

    public static boolean enabled() {
        return ItemGroupRuntime.anyFolds();
    }

    public static boolean slotClicked(CreativeModeInventoryScreen screen, Slot slot) {
        if (!enabled() || !isGridSlot(slot)) {
            return false;
        }
        Hit hit = hitFor(screen, slot.index);
        if (hit == null) {
            return false;
        }
        CreativeModeTab tab = CreativeModeInventoryScreenAccessor.getSelectedTab();
        ResourceLocation tabId = tab == null ? null : BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (tabId == null) {
            return false;
        }
        boolean opened = ItemGroupRuntime.toggle(hit.sectionId(), hit.fold().groupId());
        Sfx.groupToggle(opened);
        if (hit.source() != null && !hit.source().equals(tabId)) {
            createaddonorganizer.rebuildTab(ClientRegistries.displayParams(), hit.source());
        }
        createaddonorganizer.refreshTabRows(ClientRegistries.displayParams(), tabId);
        return true;
    }

    private static boolean isGridSlot(Slot slot) {
        return slot != null && slot.container == CreativeModeInventoryScreenAccessor.getContainer();
    }

    public static Component tooltipFor(CreativeModeInventoryScreen screen, Slot slot) {
        if (!enabled() || !isGridSlot(slot)) {
            return null;
        }
        Hit hit = hitFor(screen, slot.index);
        if (hit == null) {
            return null;
        }
        return Component.translatable(hit.fold().open()
                        ? "createaddonorganizer.group.close"
                        : "createaddonorganizer.group.open",
                hit.fold().title(), hit.fold().memberCount());
    }

    private static Hit[] visibleSpans(CreativeModeInventoryScreen screen) {
        if (!enabled() || !Config.showItemGroupMarkers()) {
            return null;
        }
        Hit[] spans = new Hit[COLS * VISIBLE_ROWS];
        boolean any = false;
        for (int index = 0; index < spans.length; index++) {
            spans[index] = spanAt(screen, index);
            any |= spans[index] != null;
        }
        return any ? spans : null;
    }

    public static void renderRuns(CreativeModeInventoryScreen screen, GuiGraphics g) {
        Hit[] spans = visibleSpans(screen);
        if (spans == null) {
            return;
        }
        int left = screen.getGuiLeft() + 9;
        int top = screen.getGuiTop() + 18;
        for (int index = 0; index < spans.length; index++) {
            Hit hit = spans[index];
            if (hit == null) {
                continue;
            }
            int col = index % COLS;
            int row = index / COLS;
            int x = left + col * SLOT_SIZE;
            int y = top + row * SLOT_SIZE;
            g.fill(x, y, x + SLOT_INNER, y + SLOT_INNER, BACKDROP);
            drawRunEdges(g, spans, index, col, row, x, y, edgeColorFor(hit));
        }
    }

    public static void renderBadge(CreativeModeInventoryScreen screen, GuiGraphics g, Slot slot) {
        if (!enabled() || !Config.showItemGroupMarkers() || !isGridSlot(slot)) {
            return;
        }
        Hit hit = hitFor(screen, slot.index);
        if (hit == null) {
            return;
        }
        drawMarker(g, slot.x, slot.y, hit.fold().open(), glyphColorFor(hit));
    }

    private static boolean sameRun(Hit a, Hit b) {
        return a != null && b != null && a.sectionId().equals(b.sectionId())
                && a.fold().groupId().equals(b.fold().groupId());
    }

    private static void drawRunEdges(GuiGraphics g, Hit[] spans, int index, int col, int row, int x, int y,
            int color) {
        Hit self = spans[index];
        boolean west = col > 0 && sameRun(spans[index - 1], self);
        boolean east = col < COLS - 1 && sameRun(spans[index + 1], self);
        boolean north = row > 0 && sameRun(spans[index - COLS], self);
        boolean south = row < VISIBLE_ROWS - 1 && sameRun(spans[index + COLS], self);
        if (!north) {
            g.fill(x - 1, y - 1, x + SLOT_INNER + 1, y, color);
        }
        if (!south) {
            g.fill(x - 1, y + SLOT_INNER, x + SLOT_INNER + 1, y + SLOT_INNER + 1, color);
        }
        if (!west) {
            g.fill(x - 1, y - 1, x, y + SLOT_INNER + 1, color);
        }
        if (!east) {
            g.fill(x + SLOT_INNER, y - 1, x + SLOT_INNER + 1, y + SLOT_INNER + 1, color);
        }
    }

    private static TabLayout layoutFor(Hit hit) {
        ResourceLocation owner = hit.source() != null
                ? hit.source()
                : TabLayout.ownerOfSectionId(hit.sectionId());
        return owner == null ? null : TabLayoutStore.byId(owner);
    }

    private static int edgeColorFor(Hit hit) {
        return ItemGroupColors.slotEdge(layoutFor(hit), hit.fold().groupId());
    }

    private static int glyphColorFor(Hit hit) {
        return ItemGroupColors.iconEdge(layoutFor(hit), hit.fold().groupId());
    }

    private static void drawMarker(GuiGraphics g, int x, int y, boolean open, int color) {
        RenderType over = RenderType.guiOverlay();
        if (!open) {
            g.fill(over, x + 11, y + 9, x + 14, y + 16, GLYPH_EDGE);
        }
        g.fill(over, x + 9, y + 11, x + 16, y + 14, GLYPH_EDGE);
        if (!open) {
            g.fill(over, x + 12, y + 10, x + 13, y + 15, color);
        }
        g.fill(over, x + 10, y + 12, x + 15, y + 13, color);
    }

    private static ResourceLocation idOf(CreativeModeTab tab) {
        if (tab != cachedTab) {
            cachedTab = tab;
            cachedTabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        }
        return cachedTabId;
    }

    private static Hit hitFor(CreativeModeInventoryScreen screen, int slotIndex) {
        Hit hit = spanAt(screen, slotIndex);
        return hit == null || !hit.head() ? null : hit;
    }

    private static Hit spanAt(CreativeModeInventoryScreen screen, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= COLS * VISIBLE_ROWS) {
            return null;
        }
        CreativeModeTab tab = CreativeModeInventoryScreenAccessor.getSelectedTab();
        if (tab == null || tab.getType() != CreativeModeTab.Type.CATEGORY) {
            return null;
        }
        ResourceLocation tabId = idOf(tab);
        if (tabId == null) {
            return null;
        }
        int absolute = topRow(screen) * COLS + slotIndex;
        List<Section<?>> sections = FancyTabSections.REGISTERED_TABS.get(tabId);
        if (sections == null || sections.isEmpty()) {
            ResourceLocation flatKey = ItemGroupRuntime.flatKey(tabId);
            ItemGroupRuntime.Fold flat = ItemGroupRuntime.owningFold(flatKey, absolute);
            return flat == null ? null : new Hit(flatKey, tabId, flat, flat.index() == absolute);
        }

        int row = 0;
        for (Section<?> section : sections) {
            int contentStart = (row + 1) * COLS;
            boolean collapsed = FTSInternal.isCollapsed(section);
            int size = collapsed ? 0 : section.items().getStacks().size();
            if (size > 0 && absolute >= contentStart && absolute < contentStart + size) {
                int local = absolute - contentStart;
                ItemGroupRuntime.Fold fold = ItemGroupRuntime.owningFold(section.id(), local);
                if (fold != null) {
                    return new Hit(section.id(), null, fold, fold.index() == local);
                }
                ResourceLocation flatKey = ItemGroupRuntime.flatKey(section.id());
                ItemGroupRuntime.Fold folded = ItemGroupRuntime.owningFold(flatKey, local);
                return folded == null ? null : new Hit(flatKey, section.id(), folded, folded.index() == local);
            }
            row += 1 + (size == 0 ? 0 : Mth.positiveCeilDiv(size, COLS));
        }
        return null;
    }

    private static int topRow(CreativeModeInventoryScreen screen) {
        int rows = Mth.positiveCeilDiv(screen.getMenu().items.size(), COLS) - VISIBLE_ROWS;
        if (rows <= 0) {
            return 0;
        }
        float offs = ((CreativeModeInventoryScreenAccessor) screen).getScrollOffs();
        return Math.round(Mth.clamp(offs, 0f, 1f) * rows);
    }
}
