package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.List;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class SettingsListScreen extends Screen {

    private static final int OUTER = 6;
    private static final int HEADER_H = 34;
    private static final int FOOTER_H = 26;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 3;
    private static final int BOX_H = 16;
    private static final int REMOVE_W = 16;
    private static final int BUTTON_H = 18;
    private static final int SCROLLBAR_W = 4;
    private static final int PANEL_MAX_W = 520;
    private static final int GLASS_LINE = 0x4DFFFFFF;
    private static final int GLASS_LINE_HOVER = 0x99FFFFFF;

    private static final ResourceLocation VANILLA_BUTTON =
            ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation VANILLA_BUTTON_HOVER =
            ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final ResourceLocation VANILLA_FIELD =
            ResourceLocation.withDefaultNamespace("widget/text_field");
    private static final ResourceLocation VANILLA_FIELD_FOCUS =
            ResourceLocation.withDefaultNamespace("widget/text_field_highlighted");

    private final Screen parent;
    private final SettingsCatalog.Option option;
    private final List<EntryBox> entries = new ArrayList<>();
    private final Scrollbar bar = new Scrollbar();

    private int panelX;
    private int panelW;
    private int panelY;
    private int panelH;
    private int listTop;
    private int listBottom;
    private List<FormattedCharSequence> description = List.of();

    public SettingsListScreen(Screen parent, SettingsCatalog.Option option) {
        super(option.title());
        this.parent = parent;
        this.option = option;
    }

    @Override
    protected void init() {
        List<String> existing = entries.isEmpty()
                ? SettingsCatalog.listValue(option)
                : values();
        entries.clear();

        panelW = Math.min(PANEL_MAX_W, this.width - OUTER * 2);
        panelX = (this.width - panelW) / 2;
        panelY = OUTER;
        panelH = this.height - OUTER * 2;
        description = this.font.split(option.description(), panelW - 20);
        listTop = panelY + HEADER_H + Math.min(2, description.size()) * 10;
        listBottom = panelY + panelH - FOOTER_H;
        bar.bounds(panelX + panelW - 6 - SCROLLBAR_W, listTop, listBottom - listTop);

        for (String value : existing) {
            entries.add(addEntry(value));
        }
        clampScroll();
    }

    private EntryBox addEntry(String value) {
        EntryBox box = new EntryBox(value);
        addWidget(box);
        return box;
    }

    private List<String> values() {
        List<String> out = new ArrayList<>(entries.size());
        for (EntryBox box : entries) {
            out.add(box.getValue());
        }
        return out;
    }

    private int rowWidth() {
        return panelW - 20 - SCROLLBAR_W;
    }

    private double contentHeight() {
        return entries.size() * (ROW_H + ROW_GAP) + ROW_H + ROW_GAP;
    }

    private void clampScroll() {
        bar.content(contentHeight());
    }

    private boolean valid() {
        for (EntryBox box : entries) {
            if (!box.valid) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        MenuSkin.advanceBlend();
        this.renderBackground(g, mouseX, mouseY, partialTick);
        if (vanilla()) {
            MenuSkin.contrastArea(g, panelX + 1, panelY + 1, panelW - 2, panelH - 2);
            frame(g, panelX, panelY, panelW, panelH, 0xFF000000);
        } else {
            g.fill(panelX, panelY, panelX + panelW, panelY + panelH,
                    MenuSkin.tint(MenuSkin.palette().listBackground()));
            frame(g, panelX, panelY, panelW, panelH, border());
        }

        g.drawString(this.font, this.title, panelX + 10, panelY + 10, titleColor(), shadow());
        int descY = panelY + 22;
        for (int i = 0; i < Math.min(2, description.size()); i++) {
            g.drawString(this.font, description.get(i), panelX + 10, descY, bodyColor(), shadow());
            descY += 10;
        }
        g.fill(panelX + 1, listTop - 4, panelX + panelW - 1, listTop - 3, border());

        bar.content(contentHeight());
        int offset = bar.offset();
        g.enableScissor(panelX + 1, listTop, panelX + panelW - 1, listBottom);
        int width = rowWidth();
        int x = panelX + 10;
        for (int i = 0; i < entries.size(); i++) {
            int y = listTop + i * (ROW_H + ROW_GAP) - offset;
            EntryBox box = entries.get(i);
            if (y + ROW_H < listTop - 4 || y > listBottom + 4) {
                box.visible = false;
                continue;
            }
            box.visible = true;
            box.frameX = x;
            box.frameY = y + (ROW_H - BOX_H) / 2;
            box.setX(x + 4);
            box.setY(box.frameY + (BOX_H - 8) / 2);
            box.setWidth(width - REMOVE_W - 6 - 8);
            box.render(g, mouseX, mouseY, partialTick);

            int removeX = x + width - REMOVE_W;
            boolean hovered = hover(mouseX, mouseY, removeX, box.frameY, REMOVE_W, BOX_H)
                    && mouseY >= listTop && mouseY < listBottom;
            widgetBox(g, removeX, box.frameY, REMOVE_W, BOX_H, hovered);
            if (hovered) {
                frame(g, removeX, box.frameY, REMOVE_W, BOX_H, 0xFFD85B5B);
            }
            cross(g, removeX + 5, box.frameY + 5, hovered ? 0xFFFF8080 : bodyColor());
        }

        int addY = listTop + entries.size() * (ROW_H + ROW_GAP) - offset;
        boolean addHovered = hover(mouseX, mouseY, x, addY, width, BOX_H)
                && mouseY >= listTop && mouseY < listBottom;
        widgetBox(g, x, addY, width, BOX_H, addHovered);
        Component add = Component.translatable("createaddonorganizer.settings.list.add");
        g.drawString(this.font, add, x + (width - this.font.width(add)) / 2, addY + 4,
                addHovered ? titleColor() : bodyColor(), shadow());
        g.disableScissor();

        bar.render(g, mouseX, mouseY);

        int buttonW = (panelW - 30) / 2;
        int buttonY = panelY + panelH - FOOTER_H + 4;
        boolean saveHovered = hover(mouseX, mouseY, panelX + 10, buttonY, buttonW, BUTTON_H);
        if (vanilla()) {
            widgetBox(g, panelX + 10, buttonY, buttonW, BUTTON_H, saveHovered && valid());
        } else if (glass()) {
            g.fill(panelX + 10, buttonY, panelX + 10 + buttonW, buttonY + BUTTON_H,
                    MenuSkin.fade(accent(), !valid() ? 0.12f : saveHovered ? 0.55f : 0.3f));
            frame(g, panelX + 10, buttonY, buttonW, BUTTON_H, hoverLine(saveHovered && valid()));
        } else {
            int face = !valid() ? 0xFF3A3A3A
                    : saveHovered ? accent() : MenuSkin.mixColor(accent(), 0xFF000000, 0.28f);
            g.fill(panelX + 10, buttonY, panelX + 10 + buttonW, buttonY + BUTTON_H, face);
            frame(g, panelX + 10, buttonY, buttonW, BUTTON_H, MenuSkin.mixColor(face, 0xFFFFFFFF, 0.2f));
        }
        Component save = CommonComponents.GUI_DONE;
        g.drawString(this.font, save, panelX + 10 + (buttonW - this.font.width(save)) / 2, buttonY + 5,
                valid() ? 0xFFFFFFFF : 0xFF888888, shadow());

        int cancelX = panelX + panelW - 10 - buttonW;
        boolean cancelHovered = hover(mouseX, mouseY, cancelX, buttonY, buttonW, BUTTON_H);
        widgetBox(g, cancelX, buttonY, buttonW, BUTTON_H, cancelHovered);
        Component cancel = CommonComponents.GUI_CANCEL;
        g.drawString(this.font, cancel, cancelX + (buttonW - this.font.width(cancel)) / 2, buttonY + 5,
                cancelHovered ? titleColor() : bodyColor(), shadow());

        for (Renderable renderable : this.renderables) {
            renderable.render(g, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (bar.mouseClicked(mouseX, mouseY)) {
            setFocused(null);
            return true;
        }
        int buttonW = (panelW - 30) / 2;
        int buttonY = panelY + panelH - FOOTER_H + 4;
        if (hover(mouseX, mouseY, panelX + 10, buttonY, buttonW, BUTTON_H)) {
            if (valid()) {
                click();
                SettingsCatalog.apply(option, values());
                back();
            } else {
                click();
            }
            return true;
        }
        if (hover(mouseX, mouseY, panelX + panelW - 10 - buttonW, buttonY, buttonW, BUTTON_H)) {
            click();
            back();
            return true;
        }
        if (mouseY >= listTop && mouseY < listBottom) {
            int offset = bar.displayed();
            int width = rowWidth();
            int x = panelX + 10;
            for (int i = 0; i < entries.size(); i++) {
                int y = listTop + i * (ROW_H + ROW_GAP) - offset + (ROW_H - BOX_H) / 2;
                if (hover(mouseX, mouseY, x + width - REMOVE_W, y, REMOVE_W, BOX_H)) {
                    click();
                    removeWidget(entries.remove(i));
                    clampScroll();
                    return true;
                }
            }
            int addY = listTop + entries.size() * (ROW_H + ROW_GAP) - offset;
            if (hover(mouseX, mouseY, x, addY, width, BOX_H)) {
                click();
                EntryBox box = addEntry(SettingsCatalog.newElement(option));
                entries.add(box);
                setFocused(box);
                box.setFocused(true);
                bar.content(contentHeight());
                bar.setTarget(bar.max());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= listTop && mouseY < listBottom) {
            return bar.wheel(scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (bar.mouseDragged(mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        bar.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void click() {
        Sfx.uiClick();
    }

    private void back() {
        ScreenSwoosh.surface(() -> parent, Config.SWOOSH_BACK);
    }

    @Override
    public void onClose() {
        back();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean hover(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void frame(GuiGraphics g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + width, y + height, color);
        g.fill(x, y + 1, x + 1, y + height - 1, color);
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private static void cross(GuiGraphics g, int x, int y, int color) {
        for (int i = 0; i < 6; i++) {
            g.fill(x + i, y + i, x + i + 1, y + i + 1, color);
            g.fill(x + 5 - i, y + i, x + 6 - i, y + i + 1, color);
        }
    }

    private static boolean vanilla() {
        return !MenuSkin.active();
    }

    private static boolean shadow() {
        return vanilla();
    }

    private static boolean glass() {
        return Config.menuStyle() == Config.MenuStyle.DEFAULT_MODERN;
    }

    private static int hoverLine(boolean lit) {
        if (glass()) {
            return lit ? GLASS_LINE_HOVER : GLASS_LINE;
        }
        return lit ? accent() : border();
    }

    private static void widgetBox(GuiGraphics g, int x, int y, int width, int height, boolean hovered) {
        if (vanilla()) {
            g.blitSprite(hovered ? VANILLA_BUTTON_HOVER : VANILLA_BUTTON, x, y, width, height);
            return;
        }
        g.fill(x, y, x + width, y + height, card(hovered));
        frame(g, x, y, width, height, hoverLine(hovered));
    }

    private static int border() {
        if (vanilla()) {
            return 0xFF6E6E6E;
        }
        if (glass()) {
            return GLASS_LINE;
        }
        int base = MenuSkin.tint(MenuSkin.palette().listBackground());
        return MenuSkin.mixColor(0xFF000000 | (base & 0x00FFFFFF), 0xFFFFFFFF, 0.14f);
    }

    private static int card(boolean hovered) {
        if (vanilla()) {
            return hovered ? 0x70000000 : 0x50000000;
        }
        return MenuSkin.tint(hovered ? MenuSkin.palette().boxBackgroundHover() : MenuSkin.palette().boxBackground());
    }

    private static int accent() {
        return MenuSkin.accent(0xFF4A90D9);
    }

    private static int titleColor() {
        return MenuSkin.active() ? MenuSkin.palette().textHover() : 0xFFFFFFFF;
    }

    private static int bodyColor() {
        return vanilla() ? 0xFFA0A0A0 : MenuSkin.mutedColor(0xFF9A9A9A);
    }

    private final class EntryBox extends EditBox {
        private boolean valid;
        int frameX;
        int frameY;

        EntryBox(String value) {
            super(SettingsListScreen.this.font, 0, 0, 80, BOX_H, Component.empty());
            setBordered(false);
            setMaxLength(512);
            setValue(value);
            valid = SettingsCatalog.acceptsElement(option, value);
            setResponder(text -> valid = SettingsCatalog.acceptsElement(option, text));
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int width = getWidth() + 8;
            if (vanilla()) {
                g.blitSprite(isFocused() ? VANILLA_FIELD_FOCUS : VANILLA_FIELD, frameX, frameY, width, BOX_H);
                if (!valid) {
                    frame(g, frameX, frameY, width, BOX_H, 0xFFD85B5B);
                }
            } else {
                g.fill(frameX, frameY, frameX + width, frameY + BOX_H, card(isFocused()));
                frame(g, frameX, frameY, width, BOX_H,
                        !valid ? 0xFFD85B5B : hoverLine(isFocused()));
            }
            setTextColor(valid ? titleColor() : 0xFFD85B5B);
            super.renderWidget(g, mouseX, mouseY, partialTick);
        }
    }
}
