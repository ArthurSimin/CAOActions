package com.sockywocky.createaddonorganizer.client;

import java.util.List;

import net.mcexpanded.fancytabsections.FTSInternal;
import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.ItemGroupRuntime;
import com.sockywocky.createaddonorganizer.createaddonorganizer;
import com.sockywocky.createaddonorganizer.mixin.CreativeModeInventoryScreenAccessor;

public final class ItemGroupSlots {
    private static final int COLS = 9;
    private static final int VISIBLE_ROWS = 5;
    private static final int SLOT_SIZE = 18;

    private static final int MARK_BG = 0xC0101418;
    private static final int MARK_OPEN = GlassSkin.DEFAULT_ACCENT_LIT;
    private static final int MARK_CLOSED = 0xFFD8D8D8;

    private ItemGroupSlots() {}

    private record Hit(ResourceLocation sectionId, ItemGroupRuntime.Fold fold) {}

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

    public static void render(CreativeModeInventoryScreen screen, GuiGraphics g) {
        if (!enabled() || !Config.showItemGroupMarkers()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int left = screen.getGuiLeft() + 9;
        int top = screen.getGuiTop() + 18;
        for (int index = 0; index < COLS * VISIBLE_ROWS; index++) {
            Hit hit = hitFor(screen, index);
            if (hit == null) {
                continue;
            }
            int x = left + (index % COLS) * SLOT_SIZE;
            int y = top + (index / COLS) * SLOT_SIZE;
            drawMarker(g, font, x, y, hit.fold().open());
        }
    }

    private static void drawMarker(GuiGraphics g, Font font, int x, int y, boolean open) {
        int mx = x + 10;
        int my = y + 10;
        g.fill(mx, my, mx + 7, my + 7, MARK_BG);
        int color = open ? MenuSkin.accent(MARK_OPEN) : MARK_CLOSED;
        g.drawString(font, open ? "-" : "+", mx + 2, my - 1, color, false);
    }

    private static ResourceLocation idOf(CreativeModeTab tab) {
        if (tab != cachedTab) {
            cachedTab = tab;
            cachedTabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        }
        return cachedTabId;
    }

    private static Hit hitFor(CreativeModeInventoryScreen screen, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= COLS * VISIBLE_ROWS) {
            return null;
        }
        CreativeModeTab tab = CreativeModeInventoryScreenAccessor.getSelectedTab();
        if (tab == null || tab.getType() != CreativeModeTab.Type.CATEGORY) {
            return null;
        }
        ResourceLocation tabId = idOf(tab);
        List<Section<?>> sections = tabId == null ? null : FancyTabSections.REGISTERED_TABS.get(tabId);
        if (sections == null || sections.isEmpty()) {
            return null;
        }
        int absolute = topRow(screen) * COLS + slotIndex;

        int row = 0;
        for (Section<?> section : sections) {
            int contentStart = (row + 1) * COLS;
            boolean collapsed = FTSInternal.isCollapsed(section);
            int size = collapsed ? 0 : section.items().getStacks().size();
            if (size > 0 && absolute >= contentStart && absolute < contentStart + size) {
                ItemGroupRuntime.Fold fold = ItemGroupRuntime.foldAt(section.id(), absolute - contentStart);
                return fold == null ? null : new Hit(section.id(), fold);
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
