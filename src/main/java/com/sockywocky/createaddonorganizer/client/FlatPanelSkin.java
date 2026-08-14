package com.sockywocky.createaddonorganizer.client;

import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;

public final class FlatPanelSkin implements IndexPanelSkin {

    private static final MenuPixels.Palette UNIQUE_DARK_PALETTE =
            new MenuPixels.Palette(0xFF292929, 0xFF1A1A1A, 0xFF0D0D0D, 0xFF3A3A3A, false);

    private static final ResourceLocation UNIQUE_DARK_TOGGLE_COLLAPSED = toggle("collapsed");
    private static final ResourceLocation UNIQUE_DARK_TOGGLE_EXPANDED = toggle("expanded");

    public static final FlatPanelSkin UNIQUE_DARK = new FlatPanelSkin(() -> UNIQUE_DARK_PALETTE, true);
    public static final FlatPanelSkin ADAPTIVE = new FlatPanelSkin(MenuPixels::palette, false);

    private static ResourceLocation toggle(String state) {
        return ResourceLocation.fromNamespaceAndPath("createaddonorganizer",
                "textures/gui/sidebar_toggle_" + state + "_unique_dark.png");
    }

    private static final int ICON = 18;
    private static final int GAP = 2;
    private static final int PANEL_W = 24;

    private final Supplier<MenuPixels.Palette> palette;
    private final boolean texturedToggle;

    private FlatPanelSkin(Supplier<MenuPixels.Palette> palette, boolean texturedToggle) {
        this.palette = palette;
        this.texturedToggle = texturedToggle;
    }

    private static Rect panelRect(CreativeModeInventoryScreen s, int sectionCount) {
        int py = s.getGuiTop() + 4;
        int maxH = s.getGuiTop() + s.getYSize() - 4 - py;
        int rows = Math.min(sectionCount, Math.max(0, (maxH - 6 + GAP) / (ICON + GAP)));
        int h = rows <= 0 ? 0 : rows * (ICON + GAP) - GAP + 6;
        return new Rect(s.getGuiLeft() - 2 - PANEL_W, py, PANEL_W, h);
    }

    private static Rect entryRect(Rect panel, int index, float scroll) {
        int ix = panel.x() + (PANEL_W - ICON) / 2;
        int iy = panel.y() + 3 + index * (ICON + GAP) - (int) scroll;
        return new Rect(ix, iy, ICON, ICON);
    }

    @Override
    public Hit hitTest(CreativeModeInventoryScreen screen, View view, double mouseX, double mouseY) {
        Rect panel = panelRect(screen, view.sectionCount());
        if (!view.expanded() || panel.h() <= ICON || !panel.contains(mouseX, mouseY)) {
            return new Hit.None();
        }
        int i = (int) ((mouseY - (panel.y() + 3) + view.scroll()) / (ICON + GAP));
        if (i >= 0 && i < view.sectionCount() && entryRect(panel, i, view.scroll()).contains(mouseX, mouseY)) {
            return new Hit.Entry(i);
        }
        return new Hit.PanelBody();
    }

    @Override
    public boolean wheelOver(CreativeModeInventoryScreen screen, View view, double mouseX, double mouseY) {
        return view.expanded() && panelRect(screen, view.sectionCount()).contains(mouseX, mouseY);
    }

    @Override
    public float wheelStep() {
        return 20f;
    }

    @Override
    public float maxScroll(CreativeModeInventoryScreen screen, int sectionCount) {
        int contentH = sectionCount * (ICON + GAP) - GAP + 6;
        return Math.max(0, contentH - panelRect(screen, sectionCount).h());
    }

    @Override
    public void render(CreativeModeInventoryScreen screen, GuiGraphics gg, View view, int mouseX, int mouseY) {
        if (!view.expanded()) {
            return;
        }
        Rect panel = panelRect(screen, view.sectionCount());
        if (panel.h() <= ICON) {
            return;
        }
        MenuPixels.Palette colors = palette.get();
        int px = panel.x();
        int py = panel.y();
        int pBottom = py + panel.h();

        int edgeDark = darker(colors.shadow(), colors.panel());
        int edgeLight = lighter(colors.highlight(), colors.panel());

        gg.fill(px, py, px + PANEL_W, pBottom, colors.panel());
        outline(gg, px, py, PANEL_W, panel.h(), edgeDark);

        gg.enableScissor(px + 2, py + 2, px + PANEL_W - 2, pBottom - 2);
        for (int i = 0; i < view.sectionCount(); i++) {
            Rect entry = entryRect(panel, i, view.scroll());
            int ix = entry.x();
            int iy = entry.y();
            if (iy + ICON < py || iy > pBottom) {
                continue;
            }
            gg.fill(ix, iy, ix + ICON, iy + ICON, colors.slot());
            gg.fill(ix, iy, ix + ICON, iy + 1, edgeDark);
            gg.fill(ix, iy, ix + 1, iy + ICON, edgeDark);
            gg.fill(ix, iy + ICON - 1, ix + ICON, iy + ICON, edgeLight);
            gg.fill(ix + ICON - 1, iy, ix + ICON, iy + ICON, edgeLight);
            if (i == view.selectedIndex()) {
                outline(gg, ix, iy, ICON, ICON, ColorUtil.brighten(colors.slot(), 0.45f));
            }
            if (entry.contains(mouseX, mouseY) && panel.contains(mouseX, mouseY)) {
                gg.fill(ix + 1, iy + 1, ix + ICON - 1, iy + ICON - 1, 0x30FFFFFF);
            }
            SafeIcon.render(gg, view.icons().get(i), ix + 1, iy + 1);
        }
        gg.disableScissor();
    }

    @Override
    public boolean drawsToggleItself() {
        return true;
    }

    @Override
    public void renderToggle(GuiGraphics gg, Rect rect, boolean expanded, boolean hovered) {
        MenuPixels.Palette colors = palette.get();
        int x = rect.x();
        int y = rect.y();
        int w = rect.w();
        int h = rect.h();

        if (texturedToggle) {
            gg.blit(expanded ? UNIQUE_DARK_TOGGLE_EXPANDED : UNIQUE_DARK_TOGGLE_COLLAPSED,
                    x, y, 0f, 0f, w, h, w, h);
            if (hovered) {
                gg.fill(x, y, x + w, y + h, 0x40FFFFFF);
            }
            return;
        }

        gg.fill(x, y, x + w, y + h, colors.slot());
        gg.fill(x, y, x + w, y + 1, darker(colors.shadow(), colors.slot()));
        gg.fill(x, y, x + 1, y + h, darker(colors.shadow(), colors.slot()));

        int tint = ColorUtil.brighten(colors.slot(), 0.55f);
        int centerY = y + h / 2;
        int left = x + 2;
        int arm = Math.max(1, (h - 2) / 2);
        for (int c = 0; c < arm; c++) {
            int half = expanded ? c : arm - 1 - c;
            int top = Math.max(y + 1, centerY - half);
            int bottom = Math.min(y + h, centerY + half + 1);
            gg.fill(left + c, top, left + c + 1, bottom, tint);
        }
        if (hovered) {
            gg.fill(x, y, x + w, y + h, 0x40FFFFFF);
        }
    }

    private static int darker(int candidate, int reference) {
        return luma(candidate) < luma(reference) - 8 ? candidate : scale(reference, 0.55f);
    }

    private static int scale(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.round(((argb >> 16) & 0xFF) * factor);
        int g = Math.round(((argb >> 8) & 0xFF) * factor);
        int b = Math.round((argb & 0xFF) * factor);
        return (a << 24) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
    }

    private static int lighter(int candidate, int reference) {
        return luma(candidate) > luma(reference) + 8 ? candidate : ColorUtil.brighten(reference, 0.18f);
    }

    private static int luma(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    private static void outline(GuiGraphics gg, int x, int y, int w, int h, int color) {
        gg.fill(x, y, x + w, y + 1, color);
        gg.fill(x, y + h - 1, x + w, y + h, color);
        gg.fill(x, y, x + 1, y + h, color);
        gg.fill(x + w - 1, y, x + w, y + h, color);
    }
}
