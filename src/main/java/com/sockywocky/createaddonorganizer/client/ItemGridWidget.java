package com.sockywocky.createaddonorganizer.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class ItemGridWidget extends AbstractWidget {

    public static final int CELL = 18;
    private static final int INSET = 1;
    private static final int EDGE_FADE = 12;
    private static final long SLIDE_MS = 130;
    private static final long FADE_MS = 150;
    private static final int HOVER = (0x55 << 24) | (GlassSkin.DEFAULT_ACCENT_LIT & 0x00FFFFFF);
    private static final int BADGE = 0xFFFFD86B;
    private static final float BADGE_Z = 300f;

    private List<ItemLibrary.Entry> entries = List.of();
    private Consumer<ItemLibrary.Entry> onClick = e -> {};
    private ToIntFunction<ItemLibrary.Entry> placedCount = e -> 0;
    private int scrollRow;
    private final SmoothScroll rowGlide = new SmoothScroll();
    private float glideTopPx;

    private boolean animate;
    private boolean justEnabled;
    private final Map<String, Cell> anims = new HashMap<>();

    private static final class Cell {
        float fromX;
        float fromY;
        int toX;
        int toY;
        long moveStart;
        long appearStart;
    }

    public ItemGridWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public void setEntries(List<ItemLibrary.Entry> list) {
        this.entries = list == null ? List.of() : list;
        this.scrollRow = 0;
        rowGlide.jumpTo(0);
        anims.clear();
    }

    public void setOnClick(Consumer<ItemLibrary.Entry> handler) {
        this.onClick = handler == null ? e -> {} : handler;
    }

    public void setPlacedCount(ToIntFunction<ItemLibrary.Entry> counter) {
        this.placedCount = counter == null ? e -> 0 : counter;
    }

    private void drawPlacedBadge(GuiGraphics g, ItemLibrary.Entry entry, int x, int y) {
        int count = placedCount.applyAsInt(entry);
        if (count <= 0) {
            return;
        }
        String text = count > 99 ? "99+" : String.valueOf(count);
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(0f, 0f, BADGE_Z);
        g.drawString(font, text, x + CELL - 1 - font.width(text), y + CELL - 9,
                MenuSkin.accent(BADGE), true);
        g.pose().popPose();
    }

    public void setAnimate(boolean value) {
        if (value && !animate) {
            justEnabled = true;
        }
        if (!value) {
            anims.clear();
        }
        animate = value;
    }

    public int columns() {
        return Math.max(1, (this.width - 8) / CELL);
    }

    public int visibleRows() {
        return Math.max(1, this.height / CELL);
    }

    private int drawnRows() {
        return Math.max(1, (this.height + CELL - 1) / CELL);
    }

    public int totalRows() {
        return (entries.size() + columns() - 1) / columns();
    }

    private int maxScrollRow() {
        return Math.max(0, totalRows() - visibleRows());
    }

    public ItemLibrary.Entry entryAt(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return null;
        }
        int col = (int) ((mouseX - this.getX() - INSET) / CELL);
        int row = (int) Math.floor((mouseY - this.getY() + glideTopPx) / CELL);
        if (col < 0 || col >= columns() || row < 0) {
            return null;
        }
        int index = row * columns() + col;
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        RenderProfiler.begin(RenderProfiler.LIB);
        try {
            renderGrid(g, mouseX, mouseY);
        } finally {
            RenderProfiler.end(RenderProfiler.LIB);
        }
    }

    @SuppressWarnings("deprecation")
    private void renderGrid(GuiGraphics g, int mouseX, int mouseY) {
        this.scrollRow = Mth.clamp(this.scrollRow, 0, maxScrollRow());
        rowGlide.setTarget(scrollRow * (double) CELL);
        glideTopPx = (float) rowGlide.advance();
        int cols = columns();

        IconAtlas.uploadPending(g);
        g.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
        if (animate) {
            g.drawManaged(() -> renderAnimated(g, cols, drawnRows()));
        } else {
            g.drawManaged(() -> renderStatic(g, cols, mouseX, mouseY));
        }
        g.disableScissor();

        g.drawManaged(() -> {
            renderEdgeFades(g);
            renderScrollbar(g, mouseX, mouseY);
        });
    }

    private void renderStatic(GuiGraphics g, int cols, int mouseX, int mouseY) {
        int firstRow = Math.max(0, (int) Math.floor(glideTopPx / CELL));
        int lastRow = Math.min(totalRows() - 1, (int) Math.floor((glideTopPx + this.height) / CELL));

        SafeIcon.beginBatch();
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = 0; col < cols; col++) {
                int index = row * cols + col;
                if (index >= entries.size()) {
                    break;
                }
                int x = this.getX() + INSET + col * CELL;
                int y = this.getY() + Math.round(row * CELL - glideTopPx);
                TabEditorScreen.drawCell(g, x, y);
                ItemStack stack = entries.get(index).stack();
                if (!IconAtlas.queue(stack, x + 1, y + 1)) {
                    SafeIcon.batched(g, stack, x + 1, y + 1);
                }
            }
        }
        SafeIcon.endBatch(g);
        IconAtlas.flushQuads(g);

        boolean inside = isMouseOver(mouseX, mouseY);
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = 0; col < cols; col++) {
                int index = row * cols + col;
                if (index >= entries.size()) {
                    break;
                }
                int x = this.getX() + INSET + col * CELL;
                int y = this.getY() + Math.round(row * CELL - glideTopPx);
                if (inside && mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
                    fillOverIcon(g, x + 1, y + 1, x + CELL - 1, y + CELL - 1, MenuSkin.accent(HOVER));
                }
                drawPlacedBadge(g, entries.get(index), x, y);
            }
        }
    }

    private static void fillOverIcon(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(RenderType.guiOverlay(), x1, y1, x2, y2, color);
    }

    private void renderAnimated(GuiGraphics g, int cols, int rows) {
        long now = System.currentTimeMillis();
        int base = scrollRow * cols;
        int visible = Math.min(entries.size(), base + rows * cols);
        Set<String> live = new HashSet<>();
        for (int index = base; index < visible; index++) {
            ItemLibrary.Entry entry = entries.get(index);
            String id = entry.id();
            live.add(id);
            int slot = index - base;
            int tx = INSET + (slot % cols) * CELL;
            int ty = (slot / cols) * CELL;

            Cell cell = anims.get(id);
            if (cell == null) {
                cell = new Cell();
                cell.fromX = tx;
                cell.fromY = ty;
                cell.toX = tx;
                cell.toY = ty;
                cell.moveStart = now - SLIDE_MS;
                cell.appearStart = justEnabled ? now - FADE_MS : now;
                anims.put(id, cell);
            } else if (cell.toX != tx || cell.toY != ty) {
                float moved = ease(cell.moveStart, now, SLIDE_MS);
                cell.fromX = Mth.lerp(moved, cell.fromX, cell.toX);
                cell.fromY = Mth.lerp(moved, cell.fromY, cell.toY);
                cell.toX = tx;
                cell.toY = ty;
                cell.moveStart = now;
            }

            float t = ease(cell.moveStart, now, SLIDE_MS);
            int dx = this.getX() + Math.round(Mth.lerp(t, cell.fromX, cell.toX));
            int dy = this.getY() + Math.round(Mth.lerp(t, cell.fromY, cell.toY));

            TabEditorScreen.drawCell(g, dx, dy);
            SafeIcon.render(g, entry.stack(), dx + 1, dy + 1);
            drawPlacedBadge(g, entry, dx, dy);
            float fade = Mth.clamp((now - cell.appearStart) / (float) FADE_MS, 0f, 1f);
            if (fade < 1f) {
                int a = Math.round((1f - fade) * 0xDD);
                fillOverIcon(g, dx + 1, dy + 1, dx + CELL - 1, dy + CELL - 1, a << 24);
            }
        }
        anims.keySet().retainAll(live);
        justEnabled = false;
    }

    private void renderEdgeFades(GuiGraphics g) {
        int x1 = this.getX();
        int x2 = this.getX() + this.width;
        int top = this.getY();
        int bottom = this.getY() + this.height;
        if (MenuSkin.active()) {
            return;
        }
        if (scrollRow > 0) {
            g.fillGradient(RenderType.guiOverlay(), x1, top, x2, top + EDGE_FADE, 0x90000000, 0x00000000, 0);
        }
        if (scrollRow < maxScrollRow()) {
            g.fillGradient(RenderType.guiOverlay(), x1, bottom - EDGE_FADE, x2, bottom, 0x00000000, 0x90000000, 0);
        }
    }

    public boolean overScrollbar(double mouseX, double mouseY) {
        int max = maxScrollRow();
        if (max <= 0) {
            return false;
        }
        int trackX = this.getX() + this.width - 6;
        return mouseX >= trackX && mouseX < trackX + 6
                && mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }

    public void dragScrollbar(double mouseY) {
        int max = maxScrollRow();
        if (max <= 0) {
            return;
        }
        int thumbH = Math.max(8, this.height * visibleRows() / Math.max(1, totalRows()));
        float usable = Math.max(1, this.height - thumbH);
        float frac = (float) (mouseY - this.getY() - thumbH / 2.0) / usable;
        int before = scrollRow;
        scrollRow = Mth.clamp(Math.round(frac * max), 0, max);
        if (before != scrollRow) {
            Sfx.scroll();
        }
        rowGlide.setTarget(scrollRow * (double) CELL);
    }

    private void renderScrollbar(GuiGraphics g, int mouseX, int mouseY) {
        int max = maxScrollRow();
        if (max <= 0) {
            return;
        }
        int trackX = this.getX() + this.width - 6;
        int thumbH = Math.max(8, this.height * visibleRows() / Math.max(1, totalRows()));
        float rowPos = glideTopPx / CELL;
        int thumbY = this.getY() + Math.round((this.height - thumbH) * rowPos / max);
        Scrollbar.paint(g, trackX, this.getY(), 6, this.height, thumbY, thumbH,
                overScrollbar(mouseX, mouseY));
    }

    private static float ease(long start, long now, long dur) {
        float t = Mth.clamp((now - start) / (float) dur, 0f, 1f);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    public Component tooltipAt(double mouseX, double mouseY) {
        ItemLibrary.Entry entry = entryAt(mouseX, mouseY);
        return entry == null ? null : entry.stack().getHoverName();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        ItemLibrary.Entry entry = entryAt(mouseX, mouseY);
        if (entry != null) {
            onClick.accept(entry);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        int before = scrollRow;
        int max = maxScrollRow();
        scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scrollY), 0, max);
        Sfx.scrolled(before, scrollRow, max > 0);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
