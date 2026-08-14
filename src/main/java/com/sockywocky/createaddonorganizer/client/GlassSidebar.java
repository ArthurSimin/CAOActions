package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModContainer;

final class GlassSidebar {

    static final int PAD = 8;
    static final int ROW_H = 14;
    static final int SEP_H = 9;
    static final int HEADER_H = 26;
    static final int MIN_W = 128;
    static final int MAX_W = 196;

    private static final int ICON = 14;
    private static final int VERSION_H = 41;
    private static final int LINE_GAP = 11;
    private static final int SCROLL_STEP = 14;
    private static final int MAX_JUMP_H = 6 * ROW_H;
    private static final int MIN_ROWS_AREA = 8 * ROW_H;
    private static final int UPDATE_GREEN = 0xFF6ADE6A;
    private static final int UPDATE_GREEN_HOVER = 0xFFB4FFB4;
    private static final int DANGER = GlassSkin.DANGER;
    private static final int DANGER_LIT = GlassSkin.DANGER_LIT;

    private static final ResourceLocation HEART_ICON =
            ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation BUG_ICON = ResourceLocation.fromNamespaceAndPath(
            createaddonorganizer.MODID, "textures/gui/bug.png");
    private static final int GLYPH = 9;
    private static final int GLYPH_GAP = 12;

    enum Tone { NORMAL, DANGER }

    enum Glyph { NONE, HEART, BUG }

    static final class Row {
        private final Component label;
        private final Runnable action;
        private final boolean divider;
        private boolean external;
        private Tone tone = Tone.NORMAL;
        private Glyph glyph = Glyph.NONE;
        private Supplier<Component> trailing;
        private BooleanSupplier active;
        private Component tooltip;

        private Row(Component label, Runnable action, boolean divider) {
            this.label = label;
            this.action = action;
            this.divider = divider;
        }

        static Row gap() {
            return new Row(Component.empty(), null, true);
        }

        static Row of(Component label, Runnable action) {
            return new Row(label, action, false);
        }

        Row active(BooleanSupplier supplier) {
            this.active = supplier;
            return this;
        }

        Row trailing(Supplier<Component> supplier) {
            this.trailing = supplier;
            return this;
        }

        Row glyph(Glyph value) {
            this.glyph = value;
            return this;
        }

        Row tone(Tone value) {
            this.tone = value;
            return this;
        }

        Row external() {
            this.external = true;
            return this;
        }

        Row tooltip(Component value) {
            this.tooltip = value;
            return this;
        }

        int height() {
            return divider ? SEP_H : ROW_H;
        }

        private boolean lit() {
            return active != null && active.getAsBoolean();
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final List<Row> jumpRows = new ArrayList<>();
    private final Scrollbar bar = new Scrollbar().width(2).step(SCROLL_STEP);
    private final Scrollbar jumpBar = new Scrollbar().width(2).step(SCROLL_STEP);
    private final ModContainer container;
    private final Runnable onDone;

    private Runnable onUpdate;
    private Runnable onFolder;
    private Component folderTooltip;
    private Component title = Component.empty();

    private int x;
    private int y;
    private int width;
    private int height;
    private int listTop;
    private int listBottom;
    private int jumpTop;
    private int jumpBottom;
    private int versionTop;
    private int updateY;
    private int doneY;
    private int contentH;
    private Component hoverTip;

    GlassSidebar(ModContainer container, Runnable onDone) {
        this.container = container;
        this.onDone = onDone;
    }

    static int widthFor(int screenWidth) {
        int value = Mth.clamp(screenWidth / 4, MIN_W, MAX_W);
        if (value > screenWidth - 200) {
            value = Math.max(96, screenWidth - 200);
        }
        return value;
    }

    GlassSidebar title(Component value) {
        this.title = value;
        return this;
    }

    GlassSidebar onUpdate(Runnable value) {
        this.onUpdate = value;
        return this;
    }

    GlassSidebar onFolder(Runnable value, Component tooltip) {
        this.onFolder = value;
        this.folderTooltip = tooltip;
        return this;
    }

    List<Row> rows() {
        return rows;
    }

    List<Row> jumpRows() {
        return jumpRows;
    }

    void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        layout();
    }

    void layout() {
        doneY = y + height - 4 - MenuLayout.ROW_H;
        updateY = doneY - MenuLayout.ROW_H - 3;
        versionTop = (UpdateCheck.available() ? updateY : doneY) - VERSION_H;
        listBottom = versionTop - 5;

        jumpTop = y + HEADER_H + 5;
        int room = Math.max(ROW_H * 2, listBottom - jumpTop - MIN_ROWS_AREA);
        int jumpH = jumpRows.isEmpty() ? 0 : Math.min(MAX_JUMP_H, room);
        jumpBottom = jumpTop + jumpH;
        listTop = jumpH > 0 ? jumpBottom + SEP_H : jumpTop;

        double jumpContent = 0;
        for (Row row : jumpRows) {
            jumpContent += row.height();
        }
        jumpBar.bounds(x + width - 4, jumpTop, jumpH);
        jumpBar.content(jumpContent);

        contentH = 0;
        for (Row row : rows) {
            contentH += row.height();
        }
        bar.bounds(x + width - 4, listTop, listBottom - listTop);
        bar.content(contentH);
    }

    int innerX() {
        return x + PAD;
    }

    int innerWidth() {
        return width - PAD * 2;
    }

    int listTop() {
        return listTop;
    }

    int listBottom() {
        return listBottom;
    }

    Component hoverTip() {
        return hoverTip;
    }

    void render(GuiGraphics g, Font font, int mouseX, int mouseY) {
        hoverTip = null;
        GlassSkin.panel(g, x, y, width, height);

        int textLeft = innerX();
        g.drawString(font, title, textLeft, y + 9, GlassSkin.titleTextColor(), GlassSkin.shadow());

        if (onFolder != null) {
            int folderX = x + width - PAD - ICON;
            int folderY = y + 6;
            boolean hovered = inside(mouseX, mouseY, folderX, folderY, ICON, ICON);
            GlassSkin.widgetBox(g, folderX, folderY, ICON, ICON, hovered);
            folderIcon(g, folderX + 3, folderY + 4,
                    hovered ? GlassSkin.titleTextColor() : GlassSkin.bodyTextColor());
            if (hovered && folderTooltip != null) {
                hoverTip = folderTooltip;
            }
        }

        int ruleY = y + HEADER_H;
        g.fill(x + 1, ruleY, x + width - 1, ruleY + 1, GlassSkin.borderColor());

        if (jumpBottom > jumpTop) {
            g.enableScissor(x + 1, jumpTop, x + width - 1, jumpBottom);
            int jumpY = jumpTop - jumpBar.offset();
            for (Row row : jumpRows) {
                int rowH = row.height();
                boolean hovered = mouseY >= jumpY && mouseY < jumpY + rowH
                        && mouseY >= jumpTop && mouseY < jumpBottom
                        && mouseX >= x && mouseX < x + width;
                renderRow(g, font, row, textLeft, jumpY, rowH, hovered);
                jumpY += rowH;
            }
            g.disableScissor();
            jumpBar.render(g, mouseX, mouseY);
            int dividerY = jumpBottom + SEP_H / 2;
            g.fill(textLeft, dividerY, x + width - PAD, dividerY + 1, GlassSkin.borderColor());
        }

        g.enableScissor(x + 1, listTop, x + width - 1, listBottom);
        int rowY = listTop - bar.offset();
        for (Row row : rows) {
            int rowH = row.height();
            if (row.divider) {
                int lineY = rowY + rowH / 2;
                g.fill(textLeft, lineY, x + width - PAD, lineY + 1, GlassSkin.borderColor());
                rowY += rowH;
                continue;
            }
            boolean hovered = mouseY >= rowY && mouseY < rowY + rowH
                    && mouseY >= listTop && mouseY < listBottom
                    && mouseX >= x && mouseX < x + width;
            renderRow(g, font, row, textLeft, rowY, rowH, hovered);
            rowY += rowH;
        }
        g.disableScissor();

        renderScrollbar(g, mouseX, mouseY);
        renderVersion(g, font, textLeft);
        renderUpdate(g, font, mouseX, mouseY, textLeft);
        renderDone(g, font, mouseX, mouseY, textLeft);
    }

    private void renderRow(GuiGraphics g, Font font, Row row, int textLeft, int rowY, int rowH, boolean hovered) {
        boolean lit = row.lit();
        boolean danger = row.tone == Tone.DANGER;

        if (danger && hovered) {
            g.fill(x + 1, rowY, x + width - 1, rowY + rowH, MenuSkin.fade(DANGER, 0.3f));
            g.fill(x + 1, rowY, x + 3, rowY + rowH, DANGER_LIT);
        } else if (lit && GlassSkin.glass()) {
            g.fill(x + 1, rowY, x + width - 1, rowY + rowH, MenuSkin.fade(GlassSkin.accent(), 0.14f));
            g.fill(x + 1, rowY, x + 3, rowY + rowH, GlassSkin.accent());
        }

        int color;
        if (danger) {
            color = hovered ? DANGER_LIT : MenuSkin.bodyColor(0xFFB0B0B0);
        } else if (lit) {
            color = GlassSkin.accent();
        } else if (hovered) {
            color = GlassSkin.titleTextColor();
        } else {
            color = MenuSkin.bodyColor(0xFFB0B0B0);
        }

        if (hovered && row.tooltip != null) {
            hoverTip = row.tooltip;
        }

        int textX = textLeft;
        if (row.glyph != Glyph.NONE) {
            drawGlyph(g, row.glyph, textX, rowY + (rowH - GLYPH) / 2);
            textX += GLYPH_GAP;
        }

        int room = x + width - PAD - textX - (row.external ? 9 : 0);
        Component tail = row.trailing != null ? row.trailing.get() : null;
        int tailWidth = 0;
        if (tail != null) {
            tailWidth = Math.min(font.width(tail), Math.max(0, room - 20));
            room -= tailWidth + 6;
        }
        String label = font.plainSubstrByWidth(row.label.getString(), Math.max(0, room));
        g.drawString(font, label, textX, rowY + 3, color, GlassSkin.shadow());
        if (row.external) {
            linkArrow(g, textX + font.width(label) + 3, rowY + 3, color);
        }
        if (tail != null && tailWidth > 0) {
            String tailText = font.plainSubstrByWidth(tail.getString(), tailWidth);
            g.drawString(font, tailText, x + width - PAD - font.width(tailText), rowY + 3,
                    lit ? GlassSkin.accent() : GlassSkin.bodyTextColor(), GlassSkin.shadow());
        }
    }

    private void renderScrollbar(GuiGraphics g, int mouseX, int mouseY) {
        bar.render(g, mouseX, mouseY);
    }

    private void renderVersion(GuiGraphics g, Font font, int textLeft) {
        int color = GlassSkin.vanilla() ? 0xFF808080 : MenuSkin.mutedColor(0xFF6E6E6E);
        int room = innerWidth();
        g.drawString(font, font.plainSubstrByWidth(container.getModInfo().getDisplayName(), room),
                textLeft, versionTop, color, GlassSkin.shadow());
        g.drawString(font, font.plainSubstrByWidth("v" + container.getModInfo().getVersion(), room),
                textLeft, versionTop + LINE_GAP, color, GlassSkin.shadow());
        g.drawString(font, font.plainSubstrByWidth(
                        "MC " + SharedConstants.getCurrentVersion().getName() + " [NeoForge]", room),
                textLeft, versionTop + LINE_GAP * 2, color, GlassSkin.shadow());
    }

    private void renderUpdate(GuiGraphics g, Font font, int mouseX, int mouseY, int textLeft) {
        if (!UpdateCheck.available() || onUpdate == null) {
            return;
        }
        int room = innerWidth();
        boolean hovered = inside(mouseX, mouseY, textLeft, updateY, room, MenuLayout.ROW_H);
        GlassSkin.widgetBox(g, textLeft, updateY, room, MenuLayout.ROW_H, hovered);
        String label = font.plainSubstrByWidth(
                Component.translatable("createaddonorganizer.settings.update").getString(), room - 16);
        int color = hovered ? UPDATE_GREEN_HOVER : UPDATE_GREEN;
        int textWidth = font.width(label);
        int textX = textLeft + (room - textWidth - 9) / 2;
        g.drawString(font, label, textX, updateY + 5, color, GlassSkin.shadow());
        linkArrow(g, textX + textWidth + 3, updateY + 5, color);
        if (hovered) {
            hoverTip = Component.translatable("createaddonorganizer.settings.update.tooltip",
                    UpdateCheck.latestVersion());
        }
    }

    private void renderDone(GuiGraphics g, Font font, int mouseX, int mouseY, int textLeft) {
        int room = innerWidth();
        boolean hovered = inside(mouseX, mouseY, textLeft, doneY, room, MenuLayout.ROW_H);
        GlassSkin.accentBox(g, textLeft, doneY, room, MenuLayout.ROW_H, hovered, 1f);
        int textWidth = font.width(CommonComponents.GUI_DONE);
        g.drawString(font, CommonComponents.GUI_DONE, textLeft + (room - textWidth) / 2,
                doneY + (MenuLayout.ROW_H - 8) / 2,
                GlassSkin.vanilla() && hovered ? 0xFFFFFFA0 : 0xFFFFFFFF, GlassSkin.shadow());
    }

    boolean mouseClicked(double mouseX, double mouseY) {
        if (jumpBar.mouseClicked(mouseX, mouseY) || bar.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        if (jumpBottom > jumpTop && mouseX >= x && mouseX < x + width
                && mouseY >= jumpTop && mouseY < jumpBottom) {
            int rowY = jumpTop - jumpBar.displayed();
            for (Row row : jumpRows) {
                int rowH = row.height();
                if (mouseY >= rowY && mouseY < rowY + rowH) {
                    if (row.action != null) {
                        click();
                        row.action.run();
                    }
                    return true;
                }
                rowY += rowH;
            }
            return true;
        }
        int room = innerWidth();
        int textLeft = innerX();
        if (UpdateCheck.available() && onUpdate != null
                && inside(mouseX, mouseY, textLeft, updateY, room, MenuLayout.ROW_H)) {
            click();
            onUpdate.run();
            return true;
        }
        if (inside(mouseX, mouseY, textLeft, doneY, room, MenuLayout.ROW_H)) {
            click();
            onDone.run();
            return true;
        }
        if (onFolder != null
                && inside(mouseX, mouseY, x + width - PAD - ICON, y + 6, ICON, ICON)) {
            click();
            onFolder.run();
            return true;
        }
        if (mouseX < x || mouseX >= x + width || mouseY < listTop || mouseY >= listBottom) {
            return false;
        }
        int rowY = listTop - bar.displayed();
        for (Row row : rows) {
            int rowH = row.height();
            if (!row.divider && mouseY >= rowY && mouseY < rowY + rowH) {
                if (row.action != null) {
                    click();
                    row.action.run();
                }
                return true;
            }
            rowY += rowH;
        }
        return false;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (jumpBottom > jumpTop && mouseX >= x && mouseX < x + width
                && mouseY >= jumpTop && mouseY < jumpBottom) {
            jumpBar.wheel(scrollY);
            return true;
        }
        if (mouseX < x || mouseX >= x + width || mouseY < listTop || mouseY >= listBottom) {
            return false;
        }
        bar.wheel(scrollY);
        return true;
    }

    boolean mouseDragged(double mouseY) {
        return jumpBar.mouseDragged(mouseY) || bar.mouseDragged(mouseY);
    }

    void mouseReleased() {
        jumpBar.mouseReleased();
        bar.mouseReleased();
    }

    boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static void drawGlyph(GuiGraphics g, Glyph glyph, int x, int y) {
        if (glyph == Glyph.HEART) {
            g.blitSprite(HEART_ICON, x, y, GLYPH, GLYPH);
        } else if (glyph == Glyph.BUG) {
            g.blit(BUG_ICON, x, y, GLYPH, GLYPH, 0f, 0f, 13, 13, 13, 13);
        }
    }

    static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    static void magnifier(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 1, y, x + 4, y + 1, color);
        g.fill(x, y + 1, x + 1, y + 4, color);
        g.fill(x + 4, y + 1, x + 5, y + 4, color);
        g.fill(x + 1, y + 4, x + 4, y + 5, color);
        g.fill(x + 4, y + 4, x + 6, y + 6, color);
    }

    static void linkArrow(GuiGraphics g, int x, int y, int color) {
        for (int i = 0; i < 5; i++) {
            g.fill(x + i, y + 5 - i, x + i + 1, y + 6 - i, color);
        }
        g.fill(x + 2, y + 1, x + 5, y + 2, color);
        g.fill(x + 4, y + 1, x + 5, y + 4, color);
    }

    static void folderIcon(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 4, y + 1, color);
        g.fill(x, y + 1, x + 8, y + 2, color);
        g.fill(x, y + 2, x + 1, y + 7, color);
        g.fill(x + 7, y + 2, x + 8, y + 7, color);
        g.fill(x, y + 6, x + 8, y + 7, color);
    }
}
