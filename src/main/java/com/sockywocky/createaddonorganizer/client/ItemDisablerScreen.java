package com.sockywocky.createaddonorganizer.client;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.ItemObliteratorConfig;

public class ItemDisablerScreen extends Screen {

    private static final int CELL = ItemGridWidget.CELL;
    private static final int PANE_PAD = 8;
    private static final int SPLIT_W = 8;
    private static final int INSET = 1;
    private static final int DRAG_THRESHOLD = 3;
    private static final int HEADER_H = 12;

    private static final int WELL_TOP = 0x52000000;
    private static final int WELL_BOTTOM = 0x22000000;
    private static final int DROP_WASH = 0x2255C8E0;
    private static final int DROP_EDGE = 0xB055C8E0;
    private static final int LIFT_WASH = 0x22FF7A70;
    private static final int LIFT_EDGE = 0xB0FF7A70;
    private static final int REGEX_COLOR = 0xFFFFD86B;

    private final Screen returnTo;

    private List<String> entries = new ArrayList<>();
    private final Set<String> disabled = new HashSet<>();
    private boolean readable;
    private Component status;

    private ItemGridWidget library;
    private EditBox librarySearch;
    private EditBox manualBox;
    private Button addManualButton;

    private int paneY;
    private int paneH;
    private int wellX;
    private int wellY;
    private int wellW;
    private int wellH;
    private int wellCols;
    private int wellScroll;
    private int wellHeightPx;
    private final SmoothScroll wellGlide = new SmoothScroll();
    private boolean wellScrollbarActive;
    private int libX;
    private int libY;
    private int libW;
    private int libH;
    private boolean libScrollbarActive;

    private ItemLibrary.Entry libPress;
    private String wellPress;
    private double pressX;
    private double pressY;
    private boolean dragging;
    private ItemStack dragStack = ItemStack.EMPTY;
    private String dragLabel;
    private boolean draggingOut;

    public ItemDisablerScreen(Screen returnTo) {
        super(Component.translatable("createaddonorganizer.disabler.title"));
        this.returnTo = returnTo;
    }

    @Override
    protected void init() {
        if (!readable) {
            reload();
        }

        layoutPanes();

        librarySearch = new EditBox(this.font, libX, paneY + HEADER_H, libW - 2, 16,
                Component.translatable("createaddonorganizer.tabs.searchItems"));
        librarySearch.setHint(Component.translatable("createaddonorganizer.tabs.searchItems"));
        librarySearch.setResponder(s -> library.setEntries(ItemLibrary.search(s)));
        addRenderableWidget(librarySearch);

        library = new ItemGridWidget(libX, libY, libW, libH);
        library.setEntries(ItemLibrary.search(""));
        library.setPlacedCount(entry -> disabled.contains(entry.id()) ? 1 : 0);
        addRenderableWidget(library);

        int manualY = this.height - 60;
        int addW = 46;
        manualBox = new EditBox(this.font, wellX, manualY, wellW - addW - 4, 18,
                Component.translatable("createaddonorganizer.disabler.manual"));
        manualBox.setHint(Component.translatable("createaddonorganizer.disabler.manual"));
        manualBox.setMaxLength(256);
        manualBox.setResponder(s -> refreshManualButton());
        addRenderableWidget(manualBox);

        addManualButton = addRenderableWidget(Button.builder(
                        Component.translatable("createaddonorganizer.disabler.add"), b -> addManual())
                .bounds(wellX + wellW - addW, manualY, addW, 18)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.disabler.manual.tooltip")))
                .build());
        refreshManualButton();

        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.disabler.openFile"),
                        b -> openConfigFolder())
                .bounds(libX, manualY, libW - 2, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 70, this.height - 38, 140, 18).build());

        layoutWell();
    }

    private void layoutPanes() {
        int m = 8;
        paneY = 42;
        paneH = this.height - paneY - 66;
        int span = this.width - m * 2 - SPLIT_W;
        wellX = m;
        wellW = Math.max(CELL * 4, span * 2 / 5);
        libX = wellX + wellW + SPLIT_W;
        libW = this.width - m - libX;

        wellY = paneY + HEADER_H;
        wellH = paneH - HEADER_H;
        libY = paneY + HEADER_H + 18;
        libH = paneH - HEADER_H - 18;
    }

    private void layoutWell() {
        wellCols = Math.max(1, (wellW - PANE_PAD) / CELL);
        int rows = (entries.size() + wellCols - 1) / wellCols;
        wellHeightPx = rows * CELL;
        clampWellScroll();
    }

    private int maxWellScroll() {
        return Math.max(0, wellHeightPx - wellH);
    }

    private void clampWellScroll() {
        wellScroll = Mth.clamp(wellScroll, 0, maxWellScroll());
        wellGlide.setTarget(wellScroll);
    }

    private void reload() {
        List<String> read = ItemObliteratorConfig.read();
        readable = read != null;
        entries = read == null ? new ArrayList<>() : new ArrayList<>(read);
        rebuildIndex();
        if (!readable) {
            status = Component.translatable("createaddonorganizer.disabler.noConfig");
        }
    }

    private void rebuildIndex() {
        disabled.clear();
        for (String entry : entries) {
            if (!ItemObliteratorConfig.isRegex(entry)) {
                disabled.add(entry);
            }
        }
    }

    private void refreshManualButton() {
        if (addManualButton != null) {
            addManualButton.active = readable && !manualBox.getValue().trim().isEmpty();
        }
    }

    private void commit(Component done) {
        List<String> tidy = ItemObliteratorConfig.tidied(entries);
        if (!ItemObliteratorConfig.write(tidy)) {
            status = Component.translatable("createaddonorganizer.disabler.writeFailed");
            Sfx.denied();
            reload();
            layoutWell();
            return;
        }
        entries = tidy;
        rebuildIndex();
        status = done;
        Sfx.snap();
        layoutWell();
    }

    private void addEntry(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || !readable) {
            Sfx.denied();
            return;
        }
        if (entries.contains(value)) {
            status = Component.translatable("createaddonorganizer.disabler.alreadyThere", labelOf(value));
            Sfx.denied();
            return;
        }
        entries.add(value);
        commit(Component.translatable("createaddonorganizer.disabler.added", labelOf(value)));
    }

    private void addManual() {
        String value = manualBox.getValue();
        manualBox.setValue("");
        refreshManualButton();
        addEntry(value);
    }

    private void removeEntry(String entry) {
        if (!entries.remove(entry)) {
            return;
        }
        commit(Component.translatable("createaddonorganizer.disabler.removed", labelOf(entry)));
    }

    private void openConfigFolder() {
        Path path = ItemObliteratorConfig.file();
        Util.getPlatform().openPath(path == null ? FMLPaths.CONFIGDIR.get() : path.getParent());
    }

    private static String labelOf(String entry) {
        if (ItemObliteratorConfig.isRegex(entry)) {
            return entry;
        }
        ItemLibrary.Entry found = ItemLibrary.byId(entry);
        return found == null ? entry : found.name();
    }

    private boolean inWell(double mouseX, double mouseY) {
        return mouseX >= wellX && mouseX < wellX + wellW && mouseY >= wellY && mouseY < wellY + wellH;
    }

    private int wellIndexAt(double mouseX, double mouseY) {
        if (!inWell(mouseX, mouseY)) {
            return -1;
        }
        int localX = (int) mouseX - wellX - INSET;
        int localY = (int) mouseY - wellY + wellScroll;
        int col = localX / CELL;
        int row = localY / CELL;
        if (localX < 0 || localY < 0 || col < 0 || col >= wellCols) {
            return -1;
        }
        int index = row * wellCols + col;
        return index >= 0 && index < entries.size() ? index : -1;
    }

    private boolean overWellScrollbar(double mouseX, double mouseY) {
        if (maxWellScroll() <= 0) {
            return false;
        }
        int trackX = wellX + wellW - 6;
        return mouseX >= trackX && mouseX < trackX + 6 && mouseY >= wellY && mouseY < wellY + wellH;
    }

    private void dragWellScrollbar(double mouseY) {
        int max = maxWellScroll();
        if (max <= 0) {
            return;
        }
        int thumbH = Math.max(8, wellH * wellH / Math.max(1, wellHeightPx));
        float usable = Math.max(1, wellH - thumbH);
        wellScroll = Mth.clamp((int) Math.round((mouseY - wellY - thumbH / 2.0) / usable * max), 0, max);
        wellGlide.setTarget(wellScroll);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (overWellScrollbar(mouseX, mouseY)) {
            wellScrollbarActive = true;
            dragWellScrollbar(mouseY);
            return true;
        }
        if (library != null && library.overScrollbar(mouseX, mouseY)) {
            libScrollbarActive = true;
            library.dragScrollbar(mouseY);
            return true;
        }
        if (readable && library != null && library.isMouseOver(mouseX, mouseY)) {
            ItemLibrary.Entry entry = library.entryAt(mouseX, mouseY);
            if (entry != null) {
                libPress = entry;
                pressX = mouseX;
                pressY = mouseY;
                return true;
            }
        }
        if (readable) {
            int index = wellIndexAt(mouseX, mouseY);
            if (index >= 0) {
                wellPress = entries.get(index);
                pressX = mouseX;
                pressY = mouseY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean moved(double mouseX, double mouseY) {
        return Math.abs(mouseX - pressX) > DRAG_THRESHOLD || Math.abs(mouseY - pressY) > DRAG_THRESHOLD;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button != 0) {
            return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        }
        if (wellScrollbarActive) {
            dragWellScrollbar(mouseY);
            return true;
        }
        if (libScrollbarActive) {
            if (library != null) {
                library.dragScrollbar(mouseY);
            }
            return true;
        }
        if (!dragging && (libPress != null || wellPress != null) && moved(mouseX, mouseY)) {
            dragging = true;
            draggingOut = wellPress != null;
            if (libPress != null) {
                dragStack = libPress.stack();
                dragLabel = null;
            } else {
                dragStack = ItemObliteratorConfig.isRegex(wellPress)
                        ? ItemStack.EMPTY
                        : ItemLibrary.stackOf(wellPress);
                dragLabel = ItemObliteratorConfig.isRegex(wellPress) ? ".*" : null;
            }
            Sfx.grab();
        }
        if (dragging) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        if (wellScrollbarActive) {
            wellScrollbarActive = false;
            return true;
        }
        if (libScrollbarActive) {
            libScrollbarActive = false;
            return true;
        }
        boolean wasDragging = dragging;
        ItemLibrary.Entry droppedIn = libPress;
        String liftedOut = wellPress;
        dragging = false;
        libPress = null;
        wellPress = null;
        dragStack = ItemStack.EMPTY;
        dragLabel = null;
        if (!wasDragging) {
            if (liftedOut != null) {
                removeEntry(liftedOut);
                return true;
            }
            if (droppedIn != null) {
                addEntry(droppedIn.id());
                return true;
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }
        if (droppedIn != null) {
            if (inWell(mouseX, mouseY)) {
                addEntry(droppedIn.id());
            } else {
                Sfx.release();
            }
            return true;
        }
        if (liftedOut != null) {
            if (!inWell(mouseX, mouseY)) {
                removeEntry(liftedOut);
            } else {
                Sfx.release();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inWell(mouseX, mouseY)) {
            int before = wellScroll;
            wellScroll = Mth.clamp(wellScroll - (int) Math.signum(scrollY) * CELL, 0, maxWellScroll());
            wellGlide.setTarget(wellScroll);
            Sfx.scrolled(before, wellScroll, maxWellScroll() > 0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        Component sub = status != null
                ? status
                : Component.translatable("createaddonorganizer.disabler.count", entries.size());
        g.drawCenteredString(this.font, sub, this.width / 2, 26, MenuSkin.bodyColor(0xFF8A9AA8));

        renderWell(g, mouseX, mouseY);

        g.drawString(this.font, Component.translatable("createaddonorganizer.disabler.libraryPane"),
                libX, paneY, MenuSkin.bodyColor(0xFF8A9AA8), false);

        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.disabler.restartNote"),
                this.width / 2, this.height - 14, MenuSkin.bodyColor(0xFF6A737B));

        renderTooltip(g, mouseX, mouseY);
        renderDragGhost(g, mouseX, mouseY);
    }

    private void renderWell(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, Component.translatable("createaddonorganizer.disabler.disabledPane"),
                wellX, paneY, MenuSkin.bodyColor(0xFF8A9AA8), false);

        g.fillGradient(wellX, wellY, wellX + wellW, wellY + wellH, WELL_TOP, WELL_BOTTOM);

        boolean droppable = dragging && !draggingOut;
        boolean liftable = dragging && draggingOut;
        if (droppable) {
            boolean hot = inWell(mouseX, mouseY);
            g.fill(wellX, wellY, wellX + wellW, wellY + wellH, hot ? DROP_WASH : 0x14FFFFFF);
            outline(g, wellX, wellY, wellX + wellW, wellY + wellH, hot ? DROP_EDGE : 0x40FFFFFF);
        } else if (liftable) {
            boolean hot = !inWell(mouseX, mouseY);
            g.fill(libX, libY - 18, libX + libW, libY + libH, hot ? LIFT_WASH : 0x14FFFFFF);
            outline(g, libX, libY - 18, libX + libW, libY + libH, hot ? LIFT_EDGE : 0x40FFFFFF);
        }

        wellGlide.setTarget(wellScroll);
        int glide = (int) Math.round(wellGlide.advance());

        g.enableScissor(wellX, wellY, wellX + wellW, wellY + wellH);
        for (int i = 0; i < entries.size(); i++) {
            int x = wellX + INSET + (i % wellCols) * CELL;
            int y = wellY + (i / wellCols) * CELL - glide;
            if (y + CELL < wellY || y > wellY + wellH) {
                continue;
            }
            String entry = entries.get(i);
            TabEditorScreen.drawCell(g, x, y);
            if (ItemObliteratorConfig.isRegex(entry)) {
                g.drawString(this.font, ".*", x + 4, y + 5, MenuSkin.accent(REGEX_COLOR), false);
            } else {
                SafeIcon.render(g, ItemLibrary.stackOf(entry), x + 1, y + 1);
            }
            if (!dragging && mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL
                    && inWell(mouseX, mouseY)) {
                g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0x40FF7A70);
            }
        }
        g.disableScissor();

        if (entries.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.disabler.dropHint"),
                    wellX + wellW / 2, wellY + wellH / 2 - 4, 0xFF7C8894);
        }
        renderWellScrollbar(g, mouseX, mouseY);
    }

    private void renderWellScrollbar(GuiGraphics g, int mouseX, int mouseY) {
        int max = maxWellScroll();
        if (max <= 0) {
            return;
        }
        int trackX = wellX + wellW - 6;
        int thumbH = Math.max(8, wellH * wellH / Math.max(1, wellHeightPx));
        int thumbY = wellY + Math.round((wellH - thumbH) * wellScroll / (float) max);
        Scrollbar.paint(g, trackX, wellY, 6, wellH, thumbY, thumbH, overWellScrollbar(mouseX, mouseY));
    }

    private void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (dragging) {
            return;
        }
        int index = wellIndexAt(mouseX, mouseY);
        if (index >= 0) {
            String entry = entries.get(index);
            List<Component> lines = new ArrayList<>(2);
            lines.add(Component.literal(labelOf(entry)));
            lines.add(Component.translatable("createaddonorganizer.disabler.dragOutHint"));
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }
        if (library != null) {
            Component tip = library.tooltipAt(mouseX, mouseY);
            if (tip != null) {
                g.renderTooltip(this.font, tip, mouseX, mouseY);
            }
        }
    }

    private void renderDragGhost(GuiGraphics g, int mouseX, int mouseY) {
        if (!dragging) {
            return;
        }
        int x = mouseX - CELL / 2;
        int y = mouseY - CELL / 2;
        TabEditorScreen.drawCell(g, x, y);
        if (dragLabel != null) {
            g.drawString(this.font, dragLabel, x + 4, y + 5, MenuSkin.accent(REGEX_COLOR), false);
        } else if (!dragStack.isEmpty()) {
            SafeIcon.render(g, dragStack, x + 1, y + 1);
        }
    }

    private static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }
}
