package com.sockywocky.createaddonorganizer.client;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

import com.sockywocky.createaddonorganizer.AbsorbedTabs;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.TabOrder;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

public class TabArrangerScreen extends Screen {

    private static final int IW = 195;
    private static final int IH = 136;
    private static final int TAB_W = 26;
    private static final int TAB_H = 32;
    private static final int PITCH = 27;
    private static final int PER_PAGE = 10;
    private static final int SLIDE_MS = 130;
    private static final long EDGE_CYCLE_MS = 400;

    private static final int SLOT = 26;
    private static final int SGAP = 1;
    private static final int CARD_H = 66;
    private static final int CARD_GAP = 6;

    private static final int ZONE_MARGIN = 10;
    private static final int ZONE_MIN_W = 20;
    private static final int ZONE_MAX_W = 64;
    private static final float SNAP_DIST = 80f;

    private final Screen returnTo;
    private final List<CreativeModeTab> order = new ArrayList<>();

    private boolean pagesMode;
    private int page;
    private int containerX;
    private int containerY;
    private int zoneW;

    private boolean barDragging;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int cardCols;
    private int cardW;
    private int scrollRow;

    private int dragFrom = -1;
    private int dragTarget = -1;
    private CreativeModeTab dragTab;
    private final UndoStack<List<CreativeModeTab>> history = new UndoStack<>();
    private Button undoButton;
    private Button redoButton;
    private boolean dragging;
    private long lastEdgeCycle;
    private boolean dirty;

    private int zoneLeftX;
    private int zoneRightX;

    private final Map<ResourceLocation, Slide> slides = new HashMap<>();

    private static final class Slide {
        float fromX;
        float fromY;
        int toX;
        int toY;
        long start;
    }

    public TabArrangerScreen(Screen returnTo) {
        super(Component.translatable("createaddonorganizer.arranger.title"));
        this.returnTo = returnTo;
    }

    @Override
    protected void init() {
        if (order.isEmpty()) {
            order.addAll(arrangeableTabs());
        }
        pagesMode = Config.arrangerLayout() == Config.ArrangerLayout.PAGES;

        int by;
        if (pagesMode) {
            panelX = 8;
            panelY = 34;
            panelW = this.width - 16;
            panelH = this.height - 38 - panelY;
            int rowW = 5 * SLOT + 4 * SGAP;
            int cardMin = rowW + 22;
            int inner = panelW - 14;
            cardCols = Mth.clamp((inner + CARD_GAP) / (cardMin + CARD_GAP), 1, 4);
            cardW = (inner - CARD_GAP * (cardCols - 1)) / cardCols;
            by = this.height - 24;
        } else {
            containerX = (this.width - IW) / 2;
            containerY = Math.max(60, (this.height - IH) / 2 - 12);
            zoneW = Mth.clamp((containerX - 2 * ZONE_MARGIN) / 2, ZONE_MIN_W, ZONE_MAX_W);
            zoneLeftX = ZONE_MARGIN;
            zoneRightX = this.width - ZONE_MARGIN - zoneW;
            by = Math.min(containerY + IH + TAB_H + 24, this.height - 24);
        }

        int bw = 76;
        int uw = 46;
        int total = uw + uw + bw + 52 + 52 + 16;
        int bx = (this.width - total) / 2;
        undoButton = addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.undo"),
                        b -> undo()).bounds(bx, by, uw, 20)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.undo.tooltip")))
                .build());
        redoButton = addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.redo"),
                        b -> redo()).bounds(bx + uw + 4, by, uw, 20)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.redo.tooltip")))
                .build());
        refreshHistoryButtons();
        bx += uw + uw + 8;
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.arranger.reset"),
                        b -> resetOrder()).bounds(bx, by, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.save"),
                        b -> save()).bounds(bx + bw + 4, by, 52, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                        b -> onClose()).bounds(bx + bw + 60, by, 52, 20).build());
    }

    static Set<ResourceLocation> foldedAway() {
        Set<ResourceLocation> folded = new HashSet<>(AbsorbedTabs.IDS);
        folded.addAll(foldedParents().keySet());
        return folded;
    }

    static Map<ResourceLocation, ResourceLocation> foldedParents() {
        Map<ResourceLocation, ResourceLocation> parents = new HashMap<>();
        for (Map.Entry<ResourceLocation, List<Section<?>>> entry : FancyTabSections.REGISTERED_TABS.entrySet()) {
            List<Section<?>> sections = entry.getValue();
            if (sections == null) {
                continue;
            }
            for (Section<?> section : sections) {
                ResourceLocation id = section.id();
                if (id != null && !id.equals(entry.getKey())) {
                    parents.put(id, entry.getKey());
                }
            }
        }
        return parents;
    }

    static List<CreativeModeTab> editableTabs() {
        List<CreativeModeTab> out = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
            if (CreativeModeTabRegistry.getDefaultTabs().contains(tab)) {
                continue;
            }
            if (BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab) != null) {
                out.add(tab);
            }
        }
        return out;
    }

    static List<CreativeModeTab> arrangeableTabs() {
        Set<ResourceLocation> folded = foldedAway();
        List<CreativeModeTab> all = new ArrayList<>();
        List<CreativeModeTab> withItems = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
            if (CreativeModeTabRegistry.getDefaultTabs().contains(tab)) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id != null && folded.contains(id)) {
                continue;
            }
            all.add(tab);
            if (tab.hasAnyItems()) {
                withItems.add(tab);
            }
        }
        return TabOrder.apply(withItems.isEmpty() ? all : withItems);
    }

    private int pageCount() {
        return Math.max(1, (order.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private int barTrackX() {
        return panelX + panelW - 8;
    }

    private int barTrackTop() {
        return panelY + 2;
    }

    private int barTrackH() {
        return panelH - 4;
    }

    private int barThumbH() {
        return Math.max(10, barTrackH() * visibleCardRows()
                / Math.max(1, (pageCount() + cardCols - 1) / cardCols));
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        if (maxScrollRow() <= 0) {
            return false;
        }
        int trackX = barTrackX();
        return mouseX >= trackX - 2 && mouseX < trackX + 6
                && mouseY >= barTrackTop() && mouseY < barTrackTop() + barTrackH();
    }

    private void dragScrollbar(double mouseY) {
        int max = maxScrollRow();
        if (max <= 0) {
            return;
        }
        int thumbH = barThumbH();
        float usable = Math.max(1, barTrackH() - thumbH);
        float fraction = (float) (mouseY - barTrackTop() - thumbH / 2.0) / usable;
        int before = scrollRow;
        scrollRow = Mth.clamp(Math.round(fraction * max), 0, max);
        if (before != scrollRow) {
            Sfx.scroll();
        }
    }

    private int visibleCardRows() {
        return Math.max(1, (panelH - 8 + CARD_GAP) / (CARD_H + CARD_GAP));
    }

    private int maxScrollRow() {
        int rows = (pageCount() + cardCols - 1) / cardCols;
        return Math.max(0, rows - visibleCardRows());
    }

    private int hitIndex(double mouseX, double mouseY) {
        return pagesMode ? hitIndexPages(mouseX, mouseY) : hitIndexScreen(mouseX, mouseY);
    }

    private int tabsOriginX() {
        int columns = 5 + Math.max(1, CreativeModeTabRegistry.getDefaultTabs().size() / 2);
        return containerX + (IW - ((columns - 1) * PITCH + TAB_W)) / 2;
    }

    private int hitIndexScreen(double mouseX, double mouseY) {
        int tabsX = tabsOriginX();
        for (int cell = 0; cell < PER_PAGE; cell++) {
            int x = tabsX + (cell % 5) * PITCH;
            int y = cell / 5 == 0 ? containerY - TAB_H : containerY + IH;
            if (mouseX >= x && mouseX < x + TAB_W && mouseY >= y && mouseY < y + TAB_H) {
                return page * PER_PAGE + cell;
            }
        }
        return -1;
    }

    private int hitIndexPages(double mouseX, double mouseY) {
        if (mouseX < panelX || mouseX > panelX + panelW || mouseY < panelY || mouseY > panelY + panelH) {
            return -1;
        }
        int rows = visibleCardRows();
        for (int ry = 0; ry < rows; ry++) {
            for (int cx = 0; cx < cardCols; cx++) {
                int pageIndex = (scrollRow + ry) * cardCols + cx;
                if (pageIndex >= pageCount()) {
                    continue;
                }
                int px = panelX + 5 + cx * (cardW + CARD_GAP);
                int py = panelY + 4 + ry * (CARD_H + CARD_GAP);
                int rowW = 5 * SLOT + 4 * SGAP;
                int gx = px + Math.max(3, (cardW - rowW) / 2);
                for (int cell = 0; cell < PER_PAGE; cell++) {
                    int sx = gx + (cell % 5) * (SLOT + SGAP);
                    int sy = py + 15 + (cell / 5) * 25;
                    if (mouseX >= sx && mouseX < sx + SLOT && mouseY >= sy && mouseY < sy + 24) {
                        return pageIndex * PER_PAGE + cell;
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overScrollbar(mouseX, mouseY)) {
            barDragging = true;
            dragScrollbar(mouseY);
            return true;
        }
        int index = hitIndex(mouseX, mouseY);
        if (index >= 0 && index < order.size() && button == 0) {
            dragFrom = index;
            dragTarget = index;
            dragTab = order.get(index);
            dragging = true;
            return true;
        }
        if (button == 0 && !pagesMode) {
            if (overZone(zoneLeftX, mouseX, mouseY) && page > 0) {
                page--;
                return true;
            }
            if (overZone(zoneRightX, mouseX, mouseY) && page < pageCount() - 1) {
                page++;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int zoneTop() {
        return containerY - TAB_H - 6;
    }

    private int zoneHeight() {
        return IH + TAB_H * 2 + 12;
    }

    private boolean overZone(int x, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + zoneW
                && mouseY >= zoneTop() && mouseY < zoneTop() + zoneHeight();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (barDragging) {
            dragScrollbar(mouseY);
            return true;
        }
        if (!dragging) {
            return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        }
        long now = System.currentTimeMillis();
        if (now - lastEdgeCycle >= EDGE_CYCLE_MS) {
            if (pagesMode) {
                if (mouseY < panelY + 12 && scrollRow > 0) {
                    scrollRow--;
                    lastEdgeCycle = now;
                } else if (mouseY > panelY + panelH - 12 && scrollRow < maxScrollRow()) {
                    scrollRow++;
                    lastEdgeCycle = now;
                }
            } else if (overZone(zoneLeftX, mouseX, mouseY) && page > 0) {
                page--;
                lastEdgeCycle = now;
            } else if (overZone(zoneRightX, mouseX, mouseY) && page < pageCount() - 1) {
                page++;
                lastEdgeCycle = now;
            }
        }
        int hit = hitIndex(mouseX, mouseY);
        if (hit >= 0) {
            dragTarget = Mth.clamp(hit, 0, order.size() - 1);
        }
        return true;
    }

    private CreativeModeTab previewHover(int mouseX, int mouseY, int tabsX, List<CreativeModeTab> pinned) {
        List<CreativeModeTab> view = previewOrder();
        for (int cell = 0; cell < PER_PAGE; cell++) {
            int index = page * PER_PAGE + cell;
            if (index >= view.size()) {
                continue;
            }
            int row = cell / 5;
            int x = tabsX + (cell % 5) * PITCH;
            int y = row == 0 ? containerY - TAB_H : containerY + IH;
            if (mouseX >= x && mouseX < x + TAB_W && mouseY >= y && mouseY < y + TAB_H) {
                return view.get(index);
            }
        }
        for (int i = 0; i < pinned.size(); i++) {
            boolean top = i < pinned.size() / 2;
            int x = tabsX + (5 + (i % Math.max(1, pinned.size() / 2))) * PITCH;
            int y = top ? containerY - TAB_H : containerY + IH;
            if (mouseX >= x && mouseX < x + TAB_W && mouseY >= y && mouseY < y + TAB_H) {
                return pinned.get(i);
            }
        }
        return null;
    }

    private void renderPreviewItems(GuiGraphics g, CreativeModeTab tab, int gridX) {
        if (tab == null || tab.getDisplayItems() == null) {
            return;
        }
        int slot = 0;
        for (ItemStack stack : tab.getDisplayItems()) {
            if (slot >= 45) {
                break;
            }
            int x = gridX + (slot % 9) * 18 + 1;
            int y = containerY + 18 + (slot / 9) * 18 + 1;
            SafeIcon.render(g, stack, x, y);
            slot++;
        }
    }

    private List<CreativeModeTab> previewOrder() {
        if (!dragging || dragFrom < 0 || dragTarget < 0 || dragFrom >= order.size()) {
            return order;
        }
        List<CreativeModeTab> out = new ArrayList<>(order);
        CreativeModeTab moved = out.remove(dragFrom);
        out.add(Mth.clamp(dragTarget, 0, out.size()), moved);
        return out;
    }

    private float[] slidePos(CreativeModeTab tab, int x, int y) {
        ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (id == null) {
            return new float[] {x, y};
        }
        long now = System.currentTimeMillis();
        Slide slide = slides.get(id);
        if (slide == null) {
            slide = new Slide();
            slide.fromX = x;
            slide.fromY = y;
            slide.toX = x;
            slide.toY = y;
            slide.start = now - SLIDE_MS;
            slides.put(id, slide);
        } else if (slide.toX != x || slide.toY != y) {
            float t = slideEase(slide.start, now);
            float cx = Mth.lerp(t, slide.fromX, slide.toX);
            float cy = Mth.lerp(t, slide.fromY, slide.toY);
            if (!dragging || Math.abs(x - cx) > SNAP_DIST || Math.abs(y - cy) > SNAP_DIST) {
                cx = x;
                cy = y;
            }
            slide.fromX = cx;
            slide.fromY = cy;
            slide.toX = x;
            slide.toY = y;
            slide.start = now;
        }
        float t = slideEase(slide.start, now);
        return new float[] {Mth.lerp(t, slide.fromX, slide.toX), Mth.lerp(t, slide.fromY, slide.toY)};
    }

    private static float slideEase(long start, long now) {
        float t = Mth.clamp((now - start) / (float) SLIDE_MS, 0f, 1f);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (barDragging) {
            barDragging = false;
            return true;
        }
        if (dragging) {
            if (button != 0) {
                return true;
            }
            List<CreativeModeTab> committed = new ArrayList<>(previewOrder());
            if (!committed.equals(order)) {
                history.push(new ArrayList<>(order));
                order.clear();
                order.addAll(committed);
                dirty = true;
                refreshHistoryButtons();
            }
            dragging = false;
            dragFrom = -1;
            dragTarget = -1;
            dragTab = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pagesMode) {
            int beforeRow = scrollRow;
            int maxRow = maxScrollRow();
            scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxRow);
            Sfx.scrolled(beforeRow, scrollRow, maxRow > 0);
            return true;
        }
        int beforePage = page;
        page = Mth.clamp(page - (int) Math.signum(scrollY), 0, pageCount() - 1);
        Sfx.scrolled(beforePage, page, pageCount() > 1);
        return true;
    }

    private void applyOrder(List<CreativeModeTab> next) {
        order.clear();
        order.addAll(next);
        dirty = true;
        applyLive();
        refreshHistoryButtons();
    }

    private void undo() {
        if (!history.canUndo()) {
            Sfx.denied();
            return;
        }
        applyOrder(history.undo(new ArrayList<>(order)));
    }

    private void redo() {
        if (!history.canRedo()) {
            Sfx.denied();
            return;
        }
        applyOrder(history.redo(new ArrayList<>(order)));
    }

    private void refreshHistoryButtons() {
        if (undoButton != null) {
            undoButton.active = history.canUndo();
        }
        if (redoButton != null) {
            redoButton.active = history.canRedo();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Screen.hasControlDown()) {
            if (keyCode == GLFW.GLFW_KEY_Z && Screen.hasShiftDown()) {
                redo();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_Z) {
                undo();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_Y) {
                redo();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void resetOrder() {
        history.push(new ArrayList<>(order));
        Config.setTabOrder(List.of());
        order.clear();
        order.addAll(arrangeableTabs());
        dirty = false;
        applyLive();
        refreshHistoryButtons();
    }

    private void save() {
        List<ResourceLocation> ids = new ArrayList<>(order.size());
        for (CreativeModeTab tab : order) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id != null) {
                ids.add(id);
            }
        }
        Config.setTabOrder(ids);
        dirty = false;
        applyLive();
    }

    private void applyLive() {
        createaddonorganizer.refreshTabLayout(ClientRegistries.displayParams());
    }

    @Override
    public void onClose() {
        if (!dirty) {
            ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
            return;
        }
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                save();
            } else {
                dirty = false;
                order.clear();
                order.addAll(arrangeableTabs());
                applyLive();
            }
            ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
        }, Component.translatable("createaddonorganizer.colors.unsaved.title"),
                Component.translatable("createaddonorganizer.arranger.unsaved.message"),
                Component.translatable("createaddonorganizer.colors.unsaved.save"),
                Component.translatable("createaddonorganizer.colors.unsaved.discard")));
    }

    private static void box(GuiGraphics g, int x, int y, int w, int h, int fill, int light, int dark) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, light);
        g.fill(x, y, x + 1, y + h, light);
        g.fill(x, y + h - 1, x + w, y + h, dark);
        g.fill(x + w - 1, y, x + w, y + h, dark);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        ArrangerSkin skin = ArrangerSkin.current();
        g.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

        if (pagesMode) {
            renderPages(g, mouseX, mouseY, skin);
        } else {
            renderScreen(g, mouseX, mouseY, skin);
        }

        if (dragging && dragTab != null) {
            box(g, mouseX - 13, mouseY - 16, TAB_W, TAB_H, skin.tabSelected(), skin.tabSelectedEdge(),
                    skin.tabSelectedEdge());
            SafeIcon.render(g, SafeIcon.of(dragTab), mouseX - 8, mouseY - 8);
        } else {
            int index = hitIndex(mouseX, mouseY);
            if (index >= 0 && index < order.size()) {
                g.renderTooltip(this.font, order.get(index).getDisplayName(), mouseX, mouseY);
            }
        }
    }

    private void renderScreen(GuiGraphics g, int mouseX, int mouseY, ArrangerSkin skin) {
        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.arranger.page",
                page + 1, pageCount()), this.width / 2, containerY - 46, 0xFFFFFFFF);

        drawZone(g, zoneLeftX, zoneTop(), zoneHeight(), "<",
                overZone(zoneLeftX, mouseX, mouseY) && page > 0, page > 0);
        drawZone(g, zoneRightX, zoneTop(), zoneHeight(), ">",
                overZone(zoneRightX, mouseX, mouseY) && page < pageCount() - 1,
                page < pageCount() - 1);

        box(g, containerX, containerY, IW, IH, skin.panel(), skin.panelEdgeLight(), skin.panelEdgeDark());
        g.drawString(this.font, Component.translatable("createaddonorganizer.arranger.preview"),
                containerX + 8, containerY + 6, skin.title(), false);
        List<CreativeModeTab> pinnedTabs = CreativeModeTabRegistry.getDefaultTabs();
        int tabsX = tabsOriginX();
        CreativeModeTab hovered = previewHover(mouseX, mouseY, tabsX, pinnedTabs);
        int gridX = containerX + (IW - 9 * 18) / 2;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                box(g, gridX + col * 18, containerY + 18 + row * 18, 18, 18,
                        skin.slot(), skin.slotEdgeLight(), skin.slotEdgeDark());
            }
        }

        renderPreviewItems(g, hovered, gridX);

        List<CreativeModeTab> view = previewOrder();
        for (int cell = 0; cell < PER_PAGE; cell++) {
            int index = page * PER_PAGE + cell;
            int row = cell / 5;
            int x = tabsX + (cell % 5) * PITCH;
            int y = row == 0 ? containerY - TAB_H : containerY + IH;
            if (index >= view.size()) {
                box(g, x, y, TAB_W, TAB_H, 0x40000000, 0x1AFFFFFF, 0x1AFFFFFF);
                continue;
            }
            CreativeModeTab tab = view.get(index);
            if (dragging && tab == dragTab) {
                box(g, x, y, TAB_W, TAB_H, MenuSkin.accent(0x66C89A3E),
                        MenuSkin.accent(GlassSkin.DEFAULT_ACCENT), MenuSkin.accent(GlassSkin.DEFAULT_ACCENT));
                continue;
            }
            float[] pos = slidePos(tab, x, y);
            int px = Math.round(pos[0]);
            int py = Math.round(pos[1]);
            box(g, px, py, TAB_W, TAB_H, skin.tabIdle(), skin.tabIdleEdge(), skin.slotEdgeDark());
            SafeIcon.render(g, SafeIcon.of(tab), px + 5, py + (row == 0 ? 8 : 10));
        }

        List<CreativeModeTab> pinned = pinnedTabs;
        for (int i = 0; i < pinned.size(); i++) {
            boolean top = i < pinned.size() / 2;
            int col = 5 + (i % Math.max(1, pinned.size() / 2));
            int x = tabsX + col * PITCH;
            int y = top ? containerY - TAB_H : containerY + IH;
            box(g, x, y, TAB_W, TAB_H, skin.tabLocked(), skin.tabLockedEdge(), skin.tabLockedEdge());
            SafeIcon.render(g, SafeIcon.of(pinned.get(i)), x + 5, y + (top ? 8 : 10));
        }

        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.arranger.hint"),
                this.width / 2, containerY + IH + TAB_H + 10, 0xFFAAAAAA);
    }

    private void renderPages(GuiGraphics g, int mouseX, int mouseY, ArrangerSkin skin) {
        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.arranger.summary",
                pageCount(), order.size()), this.width / 2, 21, 0xFFAAAAAA);

        box(g, panelX, panelY, panelW, panelH, skin.panel(), skin.panelEdgeLight(), skin.panelEdgeDark());
        g.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);

        int rows = visibleCardRows();
        int rowW = 5 * SLOT + 4 * SGAP;
        List<CreativeModeTab> view = previewOrder();
        for (int ry = 0; ry < rows; ry++) {
            for (int cx = 0; cx < cardCols; cx++) {
                int pageIndex = (scrollRow + ry) * cardCols + cx;
                if (pageIndex >= pageCount()) {
                    continue;
                }
                int px = panelX + 5 + cx * (cardW + CARD_GAP);
                int py = panelY + 4 + ry * (CARD_H + CARD_GAP);
                g.fill(px, py, px + cardW, py + CARD_H, 0x0DFFFFFF);
                g.drawString(this.font, Component.translatable("createaddonorganizer.tabs.page", pageIndex + 1),
                        px + 4, py + 4, 0xFFAAAAAA, false);

                int gx = px + Math.max(3, (cardW - rowW) / 2);
                for (int cell = 0; cell < PER_PAGE; cell++) {
                    int index = pageIndex * PER_PAGE + cell;
                    int sx = gx + (cell % 5) * (SLOT + SGAP);
                    int sy = py + 15 + (cell / 5) * 25;
                    if (index >= view.size()) {
                        box(g, sx, sy, SLOT, 24, 0x40000000, 0x1AFFFFFF, 0x1AFFFFFF);
                        continue;
                    }
                    CreativeModeTab tab = view.get(index);
                    if (dragging && tab == dragTab) {
                        box(g, sx, sy, SLOT, 24, MenuSkin.accent(0x66C89A3E),
                                MenuSkin.accent(GlassSkin.DEFAULT_ACCENT), MenuSkin.accent(GlassSkin.DEFAULT_ACCENT));
                        continue;
                    }
                    float[] pos = slidePos(tab, sx, sy);
                    int slotX = Math.round(pos[0]);
                    int slotY = Math.round(pos[1]);
                    box(g, slotX, slotY, SLOT, 24, skin.tabIdle(), skin.tabIdleEdge(), skin.slotEdgeDark());
                    SafeIcon.render(g, SafeIcon.of(tab), slotX + 5, slotY + 4);
                }
            }
        }
        g.disableScissor();

        int max = maxScrollRow();
        if (max > 0) {
            int trackX = barTrackX();
            int thumbH = barThumbH();
            int thumbY = barTrackTop() + (barTrackH() - thumbH) * scrollRow / max;
            Scrollbar.paint(g, trackX, barTrackTop(), 6, barTrackH(), thumbY, thumbH,
                    barDragging || overScrollbar(mouseX, mouseY));
        }

        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.arranger.hintPages"),
                this.width / 2, panelY + panelH + 4, 0xFFAAAAAA);
    }

    private void drawZone(GuiGraphics g, int x, int y, int h, String glyph, boolean hot, boolean enabled) {
        int fill = hot ? MenuSkin.accent(0x55C89A3E) : 0x6B000000;
        int edge = hot ? MenuSkin.accent(GlassSkin.DEFAULT_ACCENT) : 0x1AFFFFFF;
        box(g, x, y, zoneW, h, fill, edge, edge);
        int color = enabled ? (hot ? 0xFFFFE37A : 0xFFAAAAAA) : 0xFF555555;
        g.drawCenteredString(this.font, glyph, x + zoneW / 2, y + h / 2 - 4, color);
    }
}
